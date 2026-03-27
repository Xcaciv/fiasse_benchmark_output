package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.AuditLogDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.DatabaseUtil;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@WebServlet("/notes/delete")
public class NoteDeleteServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

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

        req.setAttribute("note", note);
        req.getRequestDispatcher("/WEB-INF/jsp/note/delete.jsp").forward(req, resp);
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

        // Delete attachment files first
        List<Attachment> attachments = attachmentDAO.findByNoteId(id);
        String uploadDir = DatabaseUtil.getUploadDir();
        for (Attachment att : attachments) {
            try {
                Files.deleteIfExists(Paths.get(uploadDir, att.getStoredFilename()));
            } catch (IOException ignored) {}
        }

        if (noteDAO.delete(id)) {
            auditLogDAO.log(currentUser.getId(), "NOTE_DELETE", "Deleted note id=" + id + " title=" + note.getTitle());
            req.getSession().setAttribute("flash_success", "Note deleted successfully.");
        } else {
            req.getSession().setAttribute("flash_error", "Failed to delete note.");
        }
        resp.sendRedirect(req.getContextPath() + "/notes");
    }

    private int parseId(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }
}
