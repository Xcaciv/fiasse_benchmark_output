package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.util.FileUtil;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REQ-007 – Delete a note (owner or admin only).
 * SSEM: Authorization — ownership/role verified before deletion.
 */
public class NoteDeleteServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(NoteDeleteServlet.class.getName());
    private final NoteDAO noteDAO = new NoteDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        long noteId = ValidationUtil.parseLong(req.getParameter("noteId"));
        long userId = (long) req.getSession().getAttribute("userId");
        String role = (String) req.getSession().getAttribute("role");

        if (noteId < 0) { res.sendError(HttpServletResponse.SC_BAD_REQUEST); return; }

        try {
            Note note = noteDAO.findById(noteId);
            if (note == null) { res.sendError(HttpServletResponse.SC_NOT_FOUND); return; }

            // Only owner or admin may delete
            boolean isOwner = note.getUserId() == userId;
            boolean isAdmin = "ADMIN".equalsIgnoreCase(role);

            if (!isOwner && !isAdmin) {
                LOGGER.warning("Unauthorized delete attempt by user " + userId + " on note " + noteId);
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // Remove uploaded files first (before DB row deletion cascades)
            List<Attachment> attachments = attachmentDAO.findByNoteId(noteId);
            for (Attachment a : attachments) {
                FileUtil.delete(a.getStoredFilename());
            }

            noteDAO.delete(noteId);
            LOGGER.info("Note " + noteId + " deleted by user " + userId);
            res.sendRedirect(req.getContextPath() + "/home");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting note " + noteId, e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
