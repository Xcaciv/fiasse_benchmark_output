package com.loosenotes.web;

import com.loosenotes.dao.NoteDao;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;
import com.loosenotes.security.SecurityUtil;
import com.loosenotes.service.FileStorageService;
import com.loosenotes.util.AppUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "AttachmentServlet", urlPatterns = {"/attachments/download"})
public class AttachmentServlet extends BaseServlet {
    private final NoteDao noteDao = new NoteDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long attachmentId;
        try {
            attachmentId = Long.parseLong(request.getParameter("id"));
        } catch (NumberFormatException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid attachment id.");
            return;
        }
        try {
            Attachment attachment = noteDao.findAttachmentById(attachmentId).orElse(null);
            if (attachment == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Note note = noteDao.findById(attachment.getNoteId()).orElse(null);
            User user = currentUser(request);
            String shareToken = AppUtil.trimToEmpty(request.getParameter("share"));
            boolean allowed = canView(note, user)
                || (!shareToken.isBlank() && noteDao.shareTokenGrantsAccess(attachment.getNoteId(), SecurityUtil.sha256Base64(shareToken)));
            if (!allowed) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            Path file = FileStorageService.resolve(attachment.getStoredName());
            if (!Files.exists(file)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.setContentType(attachment.getContentType());
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + AppUtil.encodeFileName(attachment.getOriginalName()));
            response.setContentLengthLong(Files.size(file));
            Files.copy(file, response.getOutputStream());
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }
}
