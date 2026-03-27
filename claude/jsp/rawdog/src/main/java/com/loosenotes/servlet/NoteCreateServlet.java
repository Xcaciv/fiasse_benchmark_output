package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.AuditLogDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;
import com.loosenotes.dao.DatabaseUtil;

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
import java.util.Set;
import java.util.UUID;

@WebServlet("/notes/create")
@MultipartConfig(maxFileSize = 10 * 1024 * 1024, maxRequestSize = 20 * 1024 * 1024)
public class NoteCreateServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
        Arrays.asList("pdf", "doc", "docx", "txt", "png", "jpg", "jpeg")
    );

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/note/create.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User currentUser = (User) req.getSession().getAttribute("currentUser");

        String title = req.getParameter("title");
        String content = req.getParameter("content");
        String isPublicStr = req.getParameter("isPublic");

        if (title == null || title.isBlank()) {
            req.setAttribute("error", "Title is required.");
            req.setAttribute("content", content);
            req.getRequestDispatcher("/WEB-INF/jsp/note/create.jsp").forward(req, resp);
            return;
        }
        if (content == null || content.isBlank()) {
            req.setAttribute("error", "Content is required.");
            req.setAttribute("title", title);
            req.getRequestDispatcher("/WEB-INF/jsp/note/create.jsp").forward(req, resp);
            return;
        }

        Note note = new Note();
        note.setTitle(title.trim());
        note.setContent(content.trim());
        note.setPublic("on".equals(isPublicStr) || "true".equals(isPublicStr));
        note.setUserId(currentUser.getId());

        if (!noteDAO.create(note)) {
            req.setAttribute("error", "Failed to create note.");
            req.setAttribute("title", title);
            req.setAttribute("content", content);
            req.getRequestDispatcher("/WEB-INF/jsp/note/create.jsp").forward(req, resp);
            return;
        }

        // Handle file uploads
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

        auditLogDAO.log(currentUser.getId(), "NOTE_CREATE", "Created note: " + note.getTitle());
        req.getSession().setAttribute("flash_success", "Note created successfully.");
        resp.sendRedirect(req.getContextPath() + "/notes/view?id=" + note.getId());
    }

    private String getExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx + 1) : "";
    }
}
