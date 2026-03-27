package com.loosenotes.servlet.attachment;

import com.loosenotes.model.Attachment;
import com.loosenotes.service.AttachmentService;
import com.loosenotes.util.AuditLogger;
import com.loosenotes.util.FileUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@WebServlet("/attachment/download")
public class FileDownloadServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(FileDownloadServlet.class);
    private final AttachmentService attachmentService = new AttachmentService();
    private final AuditLogger auditLogger = AuditLogger.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idParam = req.getParameter("id");
        Long userId = (Long) req.getSession(false) != null ?
                (Long) req.getSession(false).getAttribute("userId") : null;
        String ip = getClientIp(req);
        String sessionId = req.getSession(false) != null ? req.getSession(false).getId() : null;

        if (idParam == null) { resp.sendError(400); return; }
        long attachmentId;
        try { attachmentId = Long.parseLong(idParam); } catch (NumberFormatException e) { resp.sendError(400); return; }

        Optional<Attachment> attOpt = attachmentService.findById(attachmentId);
        if (attOpt.isEmpty()) { resp.sendError(404); return; }

        Attachment attachment = attOpt.get();
        if (!attachmentService.canAccess(attachmentId, userId)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path filePath = FileUtils.getUploadDirectory().resolve(attachment.getStoredFilename());
        if (!Files.exists(filePath)) { resp.sendError(404); return; }

        resp.setContentType(attachment.getContentType());
        resp.setHeader("Content-Disposition", "attachment; filename=\"" +
                attachment.getOriginalFilename().replaceAll("[\"\\\\]", "_") + "\"");
        resp.setContentLengthLong(attachment.getFileSize());
        resp.setHeader("Cache-Control", "no-store");

        try (InputStream in = Files.newInputStream(filePath);
             OutputStream out = resp.getOutputStream()) {
            in.transferTo(out);
        }

        auditLogger.log("FILE_DOWNLOADED", userId, String.valueOf(attachmentId), ip, "SUCCESS", sessionId,
                "file:" + attachment.getOriginalFilename());
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
