package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.AuditLogDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.DatabaseUtil;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@WebServlet("/notes/edit")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 20 * 1024 * 1024)
public class NoteEditServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
        Arrays.asList("pdf", "doc", "docx", "txt", "png", "jpg", "jpeg")
    );

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User currentUser = (User) req.getSession().getAttribute("currentUser");
        int id = parseId(req.getParameter("id"));
        if (id <= 0) { resp.sendError(400); return; }

        Note note = noteDAO.findById(id);
        if (note == null) { resp.sendError(404); return; }
        if (note.getUserId() != currentUser.getId() && !currentUser.isAdmin()) {
            resp.sendError(403); return;
        }

        List<Attachment> attachments = attachmentDAO.findByNoteId(id);
        req.setAttribute("note", note);
        req.setAttribute("attachments", attachments);
        req.getRequestDispatcher("/WEB-INF/jsp/note/edit.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User currentUser = (User) req.getSession().getAttribute("currentUser");
        int id = parseId(req.getParameter("id"));
        if (id <= 0) { resp.sendError(400); return; }

        Note note = noteDAO.findById(id);
        if (note == null) { resp.sendError(404); return; }
        if (note.getUserId() != currentUser.getId() && !currentUser.isAdmin()) {
            resp.sendError(403); return;
        }

        String title = req.getParameter("title");
        String content = req.getParameter("content");
        String isPublicStr = req.getParameter("isPublic");

        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            req.setAttribute("error", "Title and content are required.");
            req.setAttribute("note", note);
            req.setAttribute("attachments", attachmentDAO.findByNoteId(id));
            req.getRequestDispatcher("/WEB-INF/jsp/note/edit.jsp").forward(req, resp);
            return;
        }

        note.setTitle(title.trim());
        note.setContent(content.trim());
        note.setPublic("on".equals(isPublicStr) || "true".equals(isPublicStr));

        if (!noteDAO.update(note)) {
            req.setAttribute("error", "Failed to update note.");
            req.setAttribute("note", note);
            req.setAttribute("attachments", attachmentDAO.findByNoteId(id));
            req.getRequestDispatcher("/WEB-INF/jsp/note/edit.jsp").forward(req, resp);
            return;
        }

        // Handle new file uploads
        String uploadDir = DatabaseUtil.getUploadDir();
        for (Part part : req.getParts()) {
            if (!"attachment".equals(part.getName())) continue;
            String submittedFileName = part.getSubmittedFileName();
            if (submittedFileName == null || submittedFileName.isBlank()) continue;
            String ext = getExtension(submittedFileName).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(ext)) continue;

            String storedName = UUID.randomUUID().toString() + "." + ext;
            try (InputStream is = part.getInputStream()) {
                Files.copy(is, Paths.get(uploadDir, storedName), StandardCopyOption.REPLACE_EXISTING);
            }
            Attachment attachment = new Attachment();
            attachment.setNoteId(note.getId());
            attachment.setOriginalFilename(submittedFileName);
            attachment.setStoredFilename(storedName);
            attachment.setFileSize(part.getSize());
            attachmentDAO.create(attachment);
        }

        // Handle attachment deletions
        String[] deleteIds = req.getParameterValues("deleteAttachment");
        if (deleteIds != null) {
            for (String delId : deleteIds) {
                try {
                    int attId = Integer.parseInt(delId);
                    Attachment att = attachmentDAO.findById(attId);
                    if (att != null && att.getNoteId() == id) {
                        Files.deleteIfExists(Paths.get(uploadDir, att.getStoredFilename()));
                        attachmentDAO.delete(attId);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        auditLogDAO.log(currentUser.getId(), "NOTE_EDIT", "Edited note id=" + id);
        req.getSession().setAttribute("flash_success", "Note updated successfully.");
        resp.sendRedirect(req.getContextPath() + "/notes/view?id=" + id);
    }

    private int parseId(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx + 1) : "";
    }
}
