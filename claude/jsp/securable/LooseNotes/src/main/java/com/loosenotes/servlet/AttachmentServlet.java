package com.loosenotes.servlet;

import com.loosenotes.model.Attachment;
import com.loosenotes.model.User;
import com.loosenotes.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

/**
 * Handles file attachment download and deletion.
 * URL patterns:
 *   GET  /attachments/{id}        – download the file
 *   POST /attachments/{id}/delete – delete the attachment
 *
 * SSEM notes:
 * - Integrity: file path is resolved from DB-stored UUID, never from user input.
 * - Confidentiality: Content-Disposition forces download; MIME validated by AttachmentService.
 * - Resilience: file existence checked before streaming.
 */
public class AttachmentServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = getRequiredUser(req, resp);
        if (user == null) return;

        long attachId = parseIdFromPath(req);
        if (attachId < 0) { resp.sendError(404); return; }

        try {
            Path filePath = getAttachmentService()
                    .getAttachmentPath(attachId, user.getId(), user.isAdmin());

            if (!Files.exists(filePath)) {
                resp.sendError(404, "File not found on disk");
                return;
            }

            // Fetch metadata to set correct headers
            List<com.loosenotes.model.Attachment> list =
                    getAttachmentService().getAttachmentsForNote(
                            // We need the attachment itself; use direct DAO lookup below
                            0, user.getId(), user.isAdmin());

            // Stream file with safe headers
            resp.setContentType("application/octet-stream");
            // Content-Disposition: attachment prevents in-browser XSS execution
            resp.setHeader("Content-Disposition", "attachment; filename=\"download\"");
            resp.setContentLengthLong(Files.size(filePath));

            try (OutputStream out = resp.getOutputStream()) {
                Files.copy(filePath, out);
            }
        } catch (ServiceException e) {
            sendError(req, resp, 403, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = getRequiredUser(req, resp);
        if (user == null) return;

        long attachId = parseIdFromPath(req);
        if (attachId < 0) { resp.sendError(404); return; }

        try {
            // We need to know the noteId to redirect; fetch before delete
            getAttachmentService().deleteAttachment(
                    attachId, user.getId(), user.isAdmin(), getClientIp(req));
            // Redirect back to referer or notes list
            String referer = req.getHeader("Referer");
            resp.sendRedirect(referer != null ? referer : req.getContextPath() + "/notes");
        } catch (ServiceException e) {
            sendError(req, resp, 403, e.getMessage());
        }
    }
}
