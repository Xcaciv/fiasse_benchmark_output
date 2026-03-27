package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDao;
import com.loosenotes.dao.AuditLogDao;
import com.loosenotes.service.AuditService;
import com.loosenotes.service.FileService;
import com.loosenotes.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.UUID;

/**
 * Handles authenticated file uploads and downloads for note attachments
 * at {@code /attachments/*}.
 *
 * <p>Routes:
 * <ul>
 *   <li>{@code POST /attachments/upload/{noteId}} — multipart file upload</li>
 *   <li>{@code GET  /attachments/{attachmentId}}  — authenticated file download</li>
 * </ul>
 *
 * <p>All security decisions (extension allowlisting, magic-byte validation,
 * size limits, quota enforcement, ownership checks) are delegated to
 * {@link FileService}.
 */
@WebServlet("/attachments/*")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,        // 1 MB — spool to disk above this
        maxFileSize       = 10L * 1024 * 1024,  // 10 MB per file
        maxRequestSize    = 11L * 1024 * 1024   // 11 MB total (file + form fields)
)
public class AttachmentServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AttachmentServlet.class);
    private static final long serialVersionUID = 1L;

    private FileService fileService;

    @Override
    public void init() throws ServletException {
        fileService = new FileService(
                new AttachmentDao(),
                new AuditService(new AuditLogDao())
        );
    }

    // =========================================================================
    // GET /attachments/{attachmentId} — serve file to authenticated owner
    // =========================================================================

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        MDC.put("correlationId", UUID.randomUUID().toString());
        try {
            Long userId = getRequiredUserId(request, response);
            if (userId == null) return;

            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Expect: /{attachmentId}
            String[] parts = pathInfo.split("/");
            if (parts.length != 2) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Long attachmentId = parseId(parts[1]);
            if (attachmentId == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try {
                // shareNoteId == null for direct authenticated access.
                fileService.serveFile(attachmentId, userId, null, response);
            } catch (ServiceException e) {
                if ("ACCESS_DENIED".equals(e.getCode())) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                } else if ("NOT_FOUND".equals(e.getCode())) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                } else {
                    log.error("Unexpected error serving attachment. attachmentId={} userId={}",
                            attachmentId, userId);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
            }

        } finally {
            MDC.remove("correlationId");
        }
    }

    // =========================================================================
    // POST /attachments/upload/{noteId} — multipart file upload
    // =========================================================================

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        MDC.put("correlationId", UUID.randomUUID().toString());
        try {
            Long userId = getRequiredUserId(request, response);
            if (userId == null) return;

            String pathInfo = request.getPathInfo();
            if (pathInfo == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Expect: /upload/{noteId}
            String[] parts = pathInfo.split("/");
            if (parts.length != 3 || !"upload".equals(parts[1])) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Long noteId = parseId(parts[2]);
            if (noteId == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Part filePart = request.getPart("file");
            if (filePart == null || filePart.getSize() == 0) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No file provided.");
                return;
            }

            String originalFilename = extractFilename(filePart);
            String submittedMimeType = filePart.getContentType();
            long contentLength = filePart.getSize();

            try {
                fileService.storeFile(
                        noteId,
                        userId,
                        filePart.getInputStream(),
                        originalFilename,
                        submittedMimeType,
                        contentLength
                );
                response.sendRedirect(request.getContextPath() + "/notes/" + noteId);

            } catch (ServiceException e) {
                if ("ACCESS_DENIED".equals(e.getCode())) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                } else if ("NOT_FOUND".equals(e.getCode())) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                } else {
                    // Validation errors (size, extension, magic bytes, quota).
                    log.info("File upload rejected. noteId={} userId={} reason={}",
                            noteId, userId, e.getMessage());
                    // Redirect back with error as query parameter so the note view
                    // can display it without exposing internal details.
                    response.sendRedirect(request.getContextPath() + "/notes/" + noteId
                            + "?uploadError=" + java.net.URLEncoder.encode(e.getMessage(), "UTF-8"));
                }
            }

        } finally {
            MDC.remove("correlationId");
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Returns the {@code userId} from the session, or redirects to login and
     * returns {@code null} if not authenticated.
     */
    private Long getRequiredUserId(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Long userId = (Long) session.getAttribute("userId");
            if (userId != null) return userId;
        }
        response.sendRedirect(request.getContextPath() + "/auth/login");
        return null;
    }

    private Long parseId(String segment) {
        try {
            return Long.parseLong(segment);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Extracts the original filename from the Content-Disposition header of
     * a multipart {@link Part}.  Falls back to {@code "upload"} if not found.
     * The returned name is used only for display and extension extraction —
     * never for constructing filesystem paths.
     */
    private String extractFilename(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition == null) return "upload";
        for (String element : contentDisposition.split(";")) {
            element = element.trim();
            if (element.startsWith("filename")) {
                String name = element.substring(element.indexOf('=') + 1).trim();
                // Strip surrounding quotes if present.
                if (name.startsWith("\"") && name.endsWith("\"")) {
                    name = name.substring(1, name.length() - 1);
                }
                return name.isEmpty() ? "upload" : name;
            }
        }
        return "upload";
    }
}
