package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;
import com.loosenotes.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class AttachmentServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AttachmentServlet.class);
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // GET /attachments/{id}/download
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String[] parts = pathInfo.split("/");
        if (parts.length < 2) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            int attachmentId = Integer.parseInt(parts[1]);
            Attachment attachment = attachmentDAO.findById(attachmentId);
            if (attachment == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Note note = noteDAO.findById(attachment.getNoteId());
            if (note == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            User currentUser = (User) req.getSession().getAttribute("currentUser");
            // Allow access if note is public, or user is owner/admin
            if (!note.isPublic() && (currentUser == null ||
                    (note.getUserId() != currentUser.getId() && !currentUser.isAdmin()))) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            File file = FileUtil.getFile(attachment.getStoredFilename());
            if (!file.exists()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found on disk");
                return;
            }

            String contentType = FileUtil.getContentType(attachment.getOriginalFilename());
            resp.setContentType(contentType);
            resp.setContentLengthLong(file.length());
            resp.setHeader("Content-Disposition",
                "attachment; filename=\"" + attachment.getOriginalFilename() + "\"");

            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // POST /attachments/{id}/delete
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String[] parts = pathInfo.split("/");
        if (parts.length < 3 || !"delete".equals(parts[2])) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            int attachmentId = Integer.parseInt(parts[1]);
            Attachment attachment = attachmentDAO.findById(attachmentId);
            if (attachment == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Note note = noteDAO.findById(attachment.getNoteId());
            if (note == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            User currentUser = (User) req.getSession().getAttribute("currentUser");
            if (note.getUserId() != currentUser.getId() && !currentUser.isAdmin()) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            FileUtil.deleteFile(attachment.getStoredFilename());
            attachmentDAO.delete(attachmentId);
            logger.info("Attachment {} deleted by user {}", attachmentId, currentUser.getUsername());

            resp.sendRedirect(req.getContextPath() + "/notes/" + note.getId() + "/edit");
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
