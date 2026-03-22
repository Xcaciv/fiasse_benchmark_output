package com.loosenotes.servlet;

import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Note;
import com.loosenotes.util.CsrfUtil;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REQ-006 – Edit a note.
 * SSEM: Authorization — verifies ownership before allowing edits.
 */
public class NoteEditServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(NoteEditServlet.class.getName());
    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        long noteId = ValidationUtil.parseLong(req.getParameter("id"));
        long userId = (long) req.getSession().getAttribute("userId");

        try {
            Note note = noteDAO.findById(noteId);
            if (note == null) { res.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
            if (note.getUserId() != userId) { res.sendError(HttpServletResponse.SC_FORBIDDEN); return; }

            CsrfUtil.getOrCreateToken(req);
            req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));
            req.setAttribute("note", note);
            req.getRequestDispatcher("/jsp/notes/edit.jsp").forward(req, res);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading note for edit", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        long noteId  = ValidationUtil.parseLong(req.getParameter("noteId"));
        long userId  = (long) req.getSession().getAttribute("userId");
        String title   = ValidationUtil.truncate(req.getParameter("title"), 255);
        String content = req.getParameter("content");
        boolean isPublic = "true".equals(req.getParameter("isPublic"));

        if (noteId < 0 || ValidationUtil.isBlank(title) || ValidationUtil.isBlank(content)) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            Note note = noteDAO.findById(noteId);
            if (note == null) { res.sendError(HttpServletResponse.SC_NOT_FOUND); return; }

            // ── Ownership check ───────────────────────────────────────────
            if (note.getUserId() != userId) {
                LOGGER.warning("Unauthorized edit attempt by user " + userId + " on note " + noteId);
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            noteDAO.update(noteId, title, content, isPublic);
            res.sendRedirect(req.getContextPath() + "/notes/view?id=" + noteId);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating note " + noteId, e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
