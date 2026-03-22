package com.loosenotes.web;

import com.loosenotes.config.AppConfig;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@WebServlet("/attachments/download")
public class AttachmentDownloadServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long attachmentId = requireLongParameter(request, response, "id");
        if (attachmentId == null) {
            return;
        }
        String token = request.getParameter("token");
        Attachment attachment = app().getAttachmentDao().findById(attachmentId).orElse(null);
        if (attachment == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Attachment not found.");
            return;
        }
        Note note = app().getNoteDao().findById(attachment.getNoteId()).orElse(null);
        if (note == null || !canView(note, currentUser(request), token)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have access to this attachment.");
            return;
        }

        AppConfig config = app().getConfig();
        Path uploadDirectory = config.getUploadDirectory().toAbsolutePath().normalize();
        Path filePath = uploadDirectory.resolve(attachment.getStorageName()).normalize();
        if (!filePath.startsWith(uploadDirectory)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid attachment path.");
            return;
        }
        if (!Files.exists(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "The stored file could not be found.");
            return;
        }

        response.setContentType(attachment.getContentType() == null ? "application/octet-stream" : attachment.getContentType());
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + attachment.getOriginalFilename().replace("\"", "") + "\"");
        response.setContentLengthLong(attachment.getFileSize());
        Files.copy(filePath, response.getOutputStream());
    }
}
