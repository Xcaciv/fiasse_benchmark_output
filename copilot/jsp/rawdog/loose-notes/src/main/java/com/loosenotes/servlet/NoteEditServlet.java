package com.loosenotes.servlet;

import com.loosenotes.dao.ActivityLogDAO;
import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.util.TokenUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@WebServlet("/notes/edit")
@MultipartConfig(maxFileSize = 10485760)
public class NoteEditServlet extends HttpServlet {

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
            Arrays.asList("pdf", "doc", "docx", "txt", "png", "jpg", "jpeg"));

    private final NoteDAO noteDAO = new NoteDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int userId = (Integer) req.getSession().getAttribute("userId");
        try {
            int noteId = Integer.parseInt(req.getParameter("id"));
            Note note = noteDAO.getNoteById(noteId);
            if (note == null || (note.getUserId() != userId && !(Boolean) req.getSession().getAttribute("isAdmin"))) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            List<Attachment> attachments = attachmentDAO.getAttachmentsByNote(noteId);
            req.setAttribute("note", note);
            req.setAttribute("attachments", attachments);
            req.getRequestDispatcher("/WEB-INF/views/notes/edit.jsp").forward(req, resp);
        } catch (Exception e) {
            req.setAttribute("error", "Error loading note: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int userId = (Integer) req.getSession().getAttribute("userId");
        boolean isAdmin = Boolean.TRUE.equals(req.getSession().getAttribute("isAdmin"));
        try {
            int noteId = Integer.parseInt(req.getParameter("id"));
            Note note = noteDAO.getNoteById(noteId);
            if (note == null || (note.getUserId() != userId && !isAdmin)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // Handle attachment deletion
            String deleteAttachmentId = req.getParameter("deleteAttachment");
            if (deleteAttachmentId != null && !deleteAttachmentId.isEmpty()) {
                int attId = Integer.parseInt(deleteAttachmentId);
                Attachment att = attachmentDAO.getAttachmentById(attId);
                if (att != null) {
                    String uploadDir = getServletContext().getRealPath("/") + "uploads";
                    new File(uploadDir + File.separator + att.getStoredFilename()).delete();
                    attachmentDAO.deleteAttachment(attId);
                }
                resp.sendRedirect(req.getContextPath() + "/notes/edit?id=" + noteId);
                return;
            }

            String title = req.getParameter("title");
            String content = req.getParameter("content");
            boolean isPublic = "true".equals(req.getParameter("isPublic"));

            if (title == null || title.trim().isEmpty()) {
                req.setAttribute("error", "Title is required.");
                req.setAttribute("note", note);
                req.setAttribute("attachments", attachmentDAO.getAttachmentsByNote(noteId));
                req.getRequestDispatcher("/WEB-INF/views/notes/edit.jsp").forward(req, resp);
                return;
            }

            noteDAO.updateNote(noteId, title.trim(), content != null ? content : "", isPublic);

            Part filePart = req.getPart("file");
            if (filePart != null && filePart.getSize() > 0) {
                String originalFilename = getFileName(filePart);
                if (originalFilename != null && !originalFilename.isEmpty()) {
                    String ext = getExtension(originalFilename);
                    if (ALLOWED_EXTENSIONS.contains(ext.toLowerCase())) {
                        String storedFilename = TokenUtil.generateToken() + "." + ext;
                        String uploadDir = getServletContext().getRealPath("/") + "uploads";
                        new File(uploadDir).mkdirs();
                        filePart.write(uploadDir + File.separator + storedFilename);
                        attachmentDAO.addAttachment(noteId, originalFilename, storedFilename);
                    }
                }
            }
            activityLogDAO.log(userId, "NOTE_EDIT", "Edited note id=" + noteId);
            resp.sendRedirect(req.getContextPath() + "/notes");
        } catch (Exception e) {
            req.setAttribute("error", "Error updating note: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
        }
    }

    private String getFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        if (contentDisp == null) return null;
        for (String s : contentDisp.split(";")) {
            if (s.trim().startsWith("filename")) {
                return s.substring(s.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot + 1) : "";
    }
}
