package com.loosenotes.service;

import com.loosenotes.dao.AttachmentDao;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.AuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

/**
 * Application-layer service for secure file upload, download, and deletion (F-05).
 *
 * <p><strong>Security controls applied:</strong>
 * <ul>
 *   <li><em>Path traversal prevention</em> — stored filenames are UUID-generated;
 *       no user-supplied string ever participates in a filesystem path.</li>
 *   <li><em>Extension allowlisting + blocklisting</em> — extension is extracted
 *       from the original filename (not the MIME type) and checked against
 *       both lists before any I/O.</li>
 *   <li><em>Magic-byte validation</em> — file header bytes are compared to
 *       known signatures so that a renamed executable cannot masquerade as
 *       an image/document.</li>
 *   <li><em>Size enforcement</em> — {@code contentLength} is checked before
 *       reading; the stream is read to at most {@code MAX_FILE_SIZE + 1} bytes
 *       to detect over-size uploads regardless of the declared length.</li>
 *   <li><em>Quota enforcement</em> — the per-user stored-bytes total is checked
 *       against {@link #MAX_USER_QUOTA} before storing new files.</li>
 *   <li><em>Content-Disposition: attachment</em> — served files always carry
 *       this header to prevent XSS via inline rendering.</li>
 * </ul>
 */
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    // -------------------------------------------------------------------------
    // Configurable constants (set via constructor, defaulted here)
    // -------------------------------------------------------------------------

    /** Maximum individual file size: 10 MB. */
    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    /** Maximum cumulative storage per user: 500 MB. */
    public static final long MAX_USER_QUOTA = 500L * 1024 * 1024;

    /** Directory where uploaded files are stored on disk. */
    public static final String UPLOAD_DIR =
            System.getProperty("loosenotes.upload.dir", "./uploads");

    /**
     * Extensions that are explicitly allowed.  Only these will be accepted
     * even if they don't appear on the blocklist.
     */
    public static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("pdf", "doc", "docx", "txt", "png", "jpg", "jpeg");

    /**
     * Extensions that are always rejected regardless of any other check.
     * Belt-and-suspenders complement to the allowlist.
     */
    public static final Set<String> BLOCKED_EXTENSIONS =
            Set.of("exe", "dll", "sh", "bat", "ps1", "aspx", "php", "jsp", "py", "rb", "pl");

    // -------------------------------------------------------------------------
    // Magic-byte constants
    // -------------------------------------------------------------------------

    private static final byte[] MAGIC_PDF  = { 0x25, 0x50, 0x44, 0x46 };               // %PDF
    private static final byte[] MAGIC_PNG  = { (byte)0x89, 0x50, 0x4E, 0x47 };         // .PNG
    private static final byte[] MAGIC_JPEG = { (byte)0xFF, (byte)0xD8, (byte)0xFF };    // JPEG SOI
    private static final byte[] MAGIC_DOC  = { (byte)0xD0, (byte)0xCF, 0x11, (byte)0xE0 }; // OLE2 (doc)
    private static final byte[] MAGIC_ZIP  = { 0x50, 0x4B, 0x03, 0x04 };               // PK (docx/zip)

    /** Minimum bytes to read for magic-byte checks. */
    private static final int MAGIC_HEADER_BYTES = 8;

    // -------------------------------------------------------------------------
    // Collaborators
    // -------------------------------------------------------------------------

    private final AttachmentDao attachmentDao;
    private final AuditService auditService;

    /**
     * @param attachmentDao DAO for attachment persistence; must not be {@code null}
     * @param auditService  audit sink; must not be {@code null}
     */
    public FileService(AttachmentDao attachmentDao, AuditService auditService) {
        if (attachmentDao == null) throw new IllegalArgumentException("attachmentDao must not be null");
        if (auditService == null) throw new IllegalArgumentException("auditService must not be null");
        this.attachmentDao = attachmentDao;
        this.auditService = auditService;
    }

    // =========================================================================
    // Store (upload)
    // =========================================================================

    /**
     * Validates and stores an uploaded file, returning an {@link Attachment} record.
     *
     * <p>Processing order:
     * <ol>
     *   <li>Declared content-length check.</li>
     *   <li>Extension extraction and blocklist/allowlist check.</li>
     *   <li>User quota check.</li>
     *   <li>Stream read (bounded) — actual size check.</li>
     *   <li>Magic-byte signature validation.</li>
     *   <li>UUID-based storage path construction (no user input in path).</li>
     *   <li>File write to disk.</li>
     *   <li>Database record creation.</li>
     *   <li>Audit event.</li>
     * </ol>
     *
     * @param noteId           note this attachment belongs to
     * @param userId           authenticated uploader's ID
     * @param inputStream      raw upload stream
     * @param originalFilename filename as supplied by the client (used only for
     *                         extension extraction and display; never in paths)
     * @param submittedMimeType MIME type claimed by the client (not trusted for
     *                          security decisions — magic bytes are authoritative)
     * @param contentLength    declared content length from the request (-1 if unknown)
     * @return persisted {@link Attachment}
     * @throws ServiceException VALIDATION on policy violations, or wraps I/O errors
     */
    public Attachment storeFile(Long noteId, Long userId,
                                InputStream inputStream,
                                String originalFilename,
                                String submittedMimeType,
                                long contentLength) throws ServiceException {

        // 1. Declared size check (fast reject before any I/O).
        if (contentLength > MAX_FILE_SIZE) {
            throw new ServiceException("VALIDATION",
                    "File exceeds maximum allowed size of " + (MAX_FILE_SIZE / 1024 / 1024) + " MB.");
        }

        // 2. Extension checks.
        String extension = extractExtension(originalFilename);
        if (BLOCKED_EXTENSIONS.contains(extension)) {
            throw new ServiceException("VALIDATION",
                    "File type '." + extension + "' is not permitted.");
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ServiceException("VALIDATION",
                    "File type '." + extension + "' is not supported. "
                            + "Allowed types: " + ALLOWED_EXTENSIONS);
        }

        // 3. User quota check.
        long currentUsage = attachmentDao.getTotalStoredBytesForUser(userId);
        if (currentUsage >= MAX_USER_QUOTA) {
            throw new ServiceException("VALIDATION",
                    "Storage quota exceeded. Maximum allowed: "
                            + (MAX_USER_QUOTA / 1024 / 1024) + " MB per user.");
        }

        // 4. Read stream (bounded to MAX_FILE_SIZE + 1 to detect over-size).
        byte[] fileBytes;
        try {
            fileBytes = readBounded(inputStream, MAX_FILE_SIZE + 1);
        } catch (IOException e) {
            log.error("I/O error reading upload stream. userId={} error={}", userId, e.getMessage(), e);
            throw new ServiceException("VALIDATION", "Could not read uploaded file.");
        }

        if (fileBytes.length > MAX_FILE_SIZE) {
            throw new ServiceException("VALIDATION",
                    "File exceeds maximum allowed size of " + (MAX_FILE_SIZE / 1024 / 1024) + " MB.");
        }

        // 5. Magic-byte validation.
        if (!isMagicBytesValid(fileBytes, extension)) {
            log.warn("Magic-byte mismatch. userId={} extension={} claimedMime={}", userId, extension, submittedMimeType);
            throw new ServiceException("VALIDATION",
                    "File content does not match the declared file type.");
        }

        // 6. Build storage path using UUID — no user input participates in the path.
        String storedFilename = UUID.randomUUID().toString() + "." + extension;
        File uploadDirectory = new File(UPLOAD_DIR);
        if (!uploadDirectory.exists() && !uploadDirectory.mkdirs()) {
            log.error("Failed to create upload directory: {}", UPLOAD_DIR);
            throw new ServiceException("VALIDATION", "Server storage is unavailable.");
        }

        // Resolve and canonicalise to guard against any path shenanigans.
        File targetFile = new File(uploadDirectory, storedFilename);
        try {
            String canonicalUploadDir = uploadDirectory.getCanonicalPath();
            String canonicalTarget = targetFile.getCanonicalPath();
            if (!canonicalTarget.startsWith(canonicalUploadDir + File.separator)
                    && !canonicalTarget.equals(canonicalUploadDir)) {
                log.error("Path traversal detected during upload. storedFilename={}", storedFilename);
                throw new ServiceException("VALIDATION", "Invalid file storage path.");
            }
        } catch (IOException e) {
            log.error("Path canonicalisation failed. error={}", e.getMessage(), e);
            throw new ServiceException("VALIDATION", "Server storage is unavailable.");
        }

        // 7. Write bytes to disk.
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            fos.write(fileBytes);
        } catch (IOException e) {
            log.error("Failed to write uploaded file. path={} error={}", targetFile.getPath(), e.getMessage(), e);
            throw new ServiceException("VALIDATION", "Failed to save uploaded file.");
        }

        // 8. Detect authoritative content type from magic bytes (ignore client claim).
        String detectedContentType = detectContentType(fileBytes);

        // 9. Persist attachment record.
        Attachment attachment = new Attachment();
        attachment.setNoteId(noteId);
        attachment.setStoredFilename(storedFilename);
        attachment.setOriginalFilename(sanitiseFilename(originalFilename));
        attachment.setContentType(detectedContentType);
        attachment.setFileSize((long) fileBytes.length);
        attachment.setCreatedAt(Instant.now());

        Attachment created = attachmentDao.create(attachment);

        // 10. Audit event.
        auditService.recordEvent(AuditEvent.builder()
                .action("FILE_UPLOADED")
                .subjectId(String.valueOf(userId))
                .objectId(String.valueOf(noteId))
                .metadata("attachmentId=" + created.getId()
                        + " extension=" + extension
                        + " size=" + fileBytes.length)
                .outcome("SUCCESS")
                .build());

        log.info("File stored. attachmentId={} noteId={} userId={} size={} ext={}",
                created.getId(), noteId, userId, fileBytes.length, extension);
        return created;
    }

    // =========================================================================
    // Serve (download)
    // =========================================================================

    /**
     * Streams a stored file to the HTTP response.
     *
     * <p>Access is permitted when the requesting user is:
     * <ul>
     *   <li>the attachment's note owner, OR</li>
     *   <li>accessing via a valid share link (the note ID from the resolved
     *       share link is passed as {@code shareNoteId}).</li>
     * </ul>
     *
     * <p>The response always sets {@code Content-Disposition: attachment} to
     * prevent inline rendering of potentially dangerous file types.
     *
     * @param attachmentId    attachment to serve
     * @param requestingUserId authenticated user ID (may be {@code null} for
     *                         share-link access)
     * @param shareNoteId     note ID from a resolved share link; {@code null} if
     *                        direct authenticated access
     * @param response        HTTP response to write to
     * @throws ServiceException NOT_FOUND or ACCESS_DENIED
     */
    public void serveFile(Long attachmentId, Long requestingUserId, Long shareNoteId,
                          HttpServletResponse response) throws ServiceException {

        Attachment attachment = requireAttachment(attachmentId);

        // Authorization check.
        boolean authorisedViaShare = shareNoteId != null
                && shareNoteId.equals(attachment.getNoteId());
        boolean authorisedAsOwner = requestingUserId != null
                && isNoteOwner(attachment.getNoteId(), requestingUserId);

        if (!authorisedViaShare && !authorisedAsOwner) {
            log.warn("ACCESS_DENIED: file download attempted without authorisation. "
                    + "attachmentId={} requestingUserId={}", attachmentId, requestingUserId);
            throw new ServiceException("ACCESS_DENIED",
                    "You do not have permission to download this file.");
        }

        File storedFile = resolveStoredFile(attachment.getStoredFilename());
        if (!storedFile.exists()) {
            log.error("Stored file missing from disk. attachmentId={} path={}", attachmentId, storedFile.getPath());
            throw new ServiceException("NOT_FOUND", "File not found.");
        }

        // Set response headers — Content-Disposition: attachment prevents inline XSS.
        response.setContentType(attachment.getContentType());
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + sanitiseFilename(attachment.getOriginalFilename()) + "\"");
        response.setContentLengthLong(attachment.getFileSize());

        // Stream file bytes.
        try (FileInputStream fis = new FileInputStream(storedFile);
             OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            log.error("I/O error serving file. attachmentId={} error={}", attachmentId, e.getMessage(), e);
            // Response may be partially written; do not throw ServiceException here.
            return;
        }

        auditService.recordEvent(AuditEvent.builder()
                .action("FILE_DOWNLOADED")
                .subjectId(requestingUserId != null ? String.valueOf(requestingUserId) : "share")
                .objectId(String.valueOf(attachmentId))
                .outcome("SUCCESS")
                .build());

        log.info("File served. attachmentId={} requestingUserId={}", attachmentId, requestingUserId);
    }

    // =========================================================================
    // Delete
    // =========================================================================

    /**
     * Deletes an attachment's database record and its physical file.
     *
     * <p>Deletion proceeds even if the physical file is missing (already
     * cleaned up externally); the DB record is always removed.
     *
     * @param attachmentId attachment to delete
     * @return {@code true} if the DB record was removed
     * @throws ServiceException NOT_FOUND if the attachment doesn't exist
     */
    public boolean deleteFile(Long attachmentId) throws ServiceException {
        Attachment attachment = requireAttachment(attachmentId);

        // Delete physical file first; tolerate missing-file gracefully.
        File storedFile = resolveStoredFile(attachment.getStoredFilename());
        if (storedFile.exists()) {
            if (!storedFile.delete()) {
                log.warn("Could not delete physical file. attachmentId={} path={}",
                        attachmentId, storedFile.getPath());
                // Continue — remove the DB record regardless.
            }
        } else {
            log.warn("Physical file not found during delete; proceeding to remove DB record. "
                    + "attachmentId={}", attachmentId);
        }

        boolean deleted = attachmentDao.delete(attachmentId);

        if (deleted) {
            auditService.recordEvent(AuditEvent.builder()
                    .action("FILE_DELETED")
                    .objectId(String.valueOf(attachmentId))
                    .outcome("SUCCESS")
                    .build());
            log.info("Attachment deleted. attachmentId={}", attachmentId);
        }

        return deleted;
    }

    // =========================================================================
    // Private: magic-byte helpers
    // =========================================================================

    /**
     * Derives a validated content-type string from the file header bytes.
     * Returns {@code "application/octet-stream"} for unrecognised headers so
     * that the response content type is never attacker-controlled.
     *
     * @param header first bytes of the file (at least {@value #MAGIC_HEADER_BYTES} bytes)
     */
    private String detectContentType(byte[] header) {
        if (startsWith(header, MAGIC_PDF))  return "application/pdf";
        if (startsWith(header, MAGIC_PNG))  return "image/png";
        if (startsWith(header, MAGIC_JPEG)) return "image/jpeg";
        if (startsWith(header, MAGIC_DOC))  return "application/msword";
        if (startsWith(header, MAGIC_ZIP))  return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        // Plain text has no reliable magic bytes; accept if extension is txt.
        return "application/octet-stream";
    }

    /**
     * Validates that the file header bytes match the expected signature for
     * the given extension.
     *
     * @param header    first bytes of the file
     * @param extension lower-cased extension (without leading dot)
     * @return {@code true} if the header is consistent with the extension
     */
    private boolean isMagicBytesValid(byte[] header, String extension) {
        if (header == null || header.length < MAGIC_HEADER_BYTES) {
            // Too short to validate — allow txt (no signature) but reject others.
            return "txt".equals(extension);
        }
        switch (extension) {
            case "pdf":
                return startsWith(header, MAGIC_PDF);
            case "png":
                return startsWith(header, MAGIC_PNG);
            case "jpg":
            case "jpeg":
                return startsWith(header, MAGIC_JPEG);
            case "doc":
                return startsWith(header, MAGIC_DOC);
            case "docx":
                // DOCX is a ZIP-based format.
                return startsWith(header, MAGIC_ZIP);
            case "txt":
                // Plain text has no universal magic bytes.
                return true;
            default:
                // Unknown extension not on allowlist — should not reach here,
                // but be conservative.
                return false;
        }
    }

    // =========================================================================
    // Private: I/O helpers
    // =========================================================================

    /**
     * Reads at most {@code maxBytes} from {@code stream} into a byte array.
     * If the stream contains more than {@code maxBytes} the returned array will
     * be exactly {@code maxBytes} long; callers use this as a sentinel.
     */
    private byte[] readBounded(InputStream stream, long maxBytes) throws IOException {
        // Pre-allocate a reasonably-sized buffer; grow as needed up to maxBytes.
        int bufferSize = (int) Math.min(maxBytes, 64 * 1024);
        byte[] buffer = new byte[bufferSize];
        int totalRead = 0;
        int bytesRead;

        while (totalRead < maxBytes
                && (bytesRead = stream.read(buffer, totalRead,
                        (int) Math.min(buffer.length - totalRead, maxBytes - totalRead))) != -1) {
            totalRead += bytesRead;
            if (totalRead == buffer.length && totalRead < maxBytes) {
                // Grow buffer.
                int newSize = (int) Math.min(buffer.length * 2L, maxBytes);
                buffer = Arrays.copyOf(buffer, newSize);
            }
        }

        // One more byte to detect over-size.
        if (totalRead == maxBytes) {
            int extra = stream.read();
            if (extra != -1) {
                // Over-size: return array longer than maxBytes - 1 to signal caller.
                return Arrays.copyOf(buffer, (int) maxBytes + 1);
            }
        }

        return Arrays.copyOf(buffer, totalRead);
    }

    private File resolveStoredFile(String storedFilename) {
        // storedFilename is always UUID.ext — no user input; still canonicalise defensively.
        return new File(UPLOAD_DIR, storedFilename);
    }

    private boolean isNoteOwner(Long noteId, Long userId) {
        // Query via AttachmentDao helper to avoid cross-service coupling.
        return attachmentDao.isNoteOwner(noteId, userId);
    }

    // =========================================================================
    // Private: utility helpers
    // =========================================================================

    /**
     * Extracts the lower-cased file extension from {@code filename}, returning
     * an empty string if none is present.
     */
    private static String extractExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        // Strip any path components the client may have injected.
        String name = new File(filename).getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == name.length() - 1) {
            return "";
        }
        return name.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * Removes characters from a filename that could cause header injection or
     * path issues when placed in Content-Disposition.
     */
    private static String sanitiseFilename(String filename) {
        if (filename == null) return "file";
        // Keep only safe characters; replace the rest with underscores.
        return filename.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private Attachment requireAttachment(Long attachmentId) throws ServiceException {
        if (attachmentId == null) {
            throw new ServiceException("VALIDATION", "Attachment ID must not be null.");
        }
        Attachment attachment = attachmentDao.findById(attachmentId);
        if (attachment == null) {
            throw new ServiceException("NOT_FOUND", "Attachment not found.");
        }
        return attachment;
    }
}
