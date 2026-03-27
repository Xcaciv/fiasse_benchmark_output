package com.loosenotes.servlet;

import com.loosenotes.model.Attachment;
import com.loosenotes.model.User;
import com.loosenotes.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Handles file attachment upload and download.
 * SSEM: Integrity - file extension, size, and content-type validated.
 * SSEM: Availability - max file size enforced at servlet and service level.
 * SSEM: Confidentiality - stored filename (UUID) never exposed; only original name.
 * SSEM: Resilience - streams closed via try-with-resources.
 */
@WebServlet("/attachments/*")
@MultipartConfig(
    maxFileSize    = 10 * 1024 * 1024,   // 10 MB per file
    maxRequestSize = 11 * 1024 * 1024,   // 11 MB per request
    fileSizeThreshold = 1024 * 1024      // 1 MB before spooling to disk
)
public class AttachmentServlet extends BaseServlet {

    private static final Logger log = LoggerFactory.getLogger(AttachmentServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String action = getAction(req);
        if ("upload".equals(action)) {
            handleUpload(req, res);
        } else {
            sendNotFound(res);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String action = getAction(req);
        if ("download".equals(action)) {
            handleDownload(req, res);
        } else {
            sendNotFound(res);
        }
    }

    private void handleUpload(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        User user = getCurrentUser(req);
        long noteId = ValidationUtil.parseLongSafe(req.getParameter("noteId"));
        if (!ValidationUtil.isValidId(noteId)) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid note ID");
            return;
        }

        Part filePart = req.getPart("file");
        if (filePart == null || filePart.getSize() == 0) {
            redirect(res, req, "/notes/view/" + noteId + "?uploadError=empty");
            return;
        }

        try (InputStream in = filePart.getInputStream()) {
            String filename    = filePart.getSubmittedFileName();
            String contentType = filePart.getContentType();
            long fileSize      = filePart.getSize();

            getAttachmentService().upload(noteId, user.getId(), filename,
                contentType, in, fileSize);
            redirect(res, req, "/notes/view/" + noteId + "?uploaded=true");
        } catch (IllegalArgumentException e) {
            redirect(res, req, "/notes/view/" + noteId + "?uploadError=invalid");
        } catch (SecurityException e) {
            sendForbidden(res);
        } catch (SQLException e) {
            log.error("DB error during attachment upload for note id={}", noteId, e);
            redirect(res, req, "/notes/view/" + noteId + "?uploadError=system");
        }
    }

    private void handleDownload(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        long attachmentId = parseAttachmentId(req);
        if (!ValidationUtil.isValidId(attachmentId)) { sendNotFound(res); return; }

        User user = getCurrentUser(req);
        boolean shareAccess = isShareAccess(req);

        try {
            Optional<Path> pathOpt = getAttachmentService()
                .getFilePath(attachmentId, user != null ? user.getId() : 0L, shareAccess);
            if (pathOpt.isEmpty()) { sendNotFound(res); return; }

            Optional<Attachment> meta = getAttachmentService().findById(attachmentId);
            if (meta.isEmpty()) { sendNotFound(res); return; }

            serveFile(res, pathOpt.get(), meta.get());
        } catch (SecurityException e) {
            sendForbidden(res);
        } catch (SQLException e) {
            log.error("DB error during attachment download id={}", attachmentId, e);
            sendNotFound(res);
        }
    }

    private void serveFile(HttpServletResponse res, Path filePath,
                            Attachment meta) throws IOException {
        // SSEM: Integrity - force download, prevent execution
        res.setContentType("application/octet-stream");
        // Use the safe original filename, encoded for Content-Disposition
        String safeFilename = meta.getOriginalFilename()
            .replace("\"", "").replace("\\", "").replace("\n", "").replace("\r", "");
        res.setHeader("Content-Disposition",
            "attachment; filename=\"" + safeFilename + "\"");
        res.setContentLengthLong(meta.getFileSize());
        java.nio.file.Files.copy(filePath, res.getOutputStream());
    }

    private long parseAttachmentId(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) return -1;
        String[] parts = pathInfo.substring(1).split("/");
        if (parts.length < 2) return -1;
        return ValidationUtil.parseLongSafe(parts[1]);
    }

    private String getAction(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) return "";
        return pathInfo.substring(1).split("/")[0];
    }

    private boolean isShareAccess(HttpServletRequest req) {
        return "true".equals(req.getParameter("share"));
    }
}
