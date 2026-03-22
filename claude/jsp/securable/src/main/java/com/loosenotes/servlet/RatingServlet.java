package com.loosenotes.servlet;

import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.RatingDAO;
import com.loosenotes.model.Note;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REQ-010 – Rate a note (upsert).
 * SSEM: Authorization (must be authenticated, cannot rate own note), Input Validation (1–5).
 *
 * POST /ratings/submit
 */
public class RatingServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(RatingServlet.class.getName());
    private final RatingDAO ratingDAO = new RatingDAO();
    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        long userId = (long) req.getSession().getAttribute("userId");
        long noteId = ValidationUtil.parseLong(req.getParameter("noteId"));
        int  rating = ValidationUtil.parseInt(req.getParameter("rating"));
        String comment = ValidationUtil.truncate(req.getParameter("comment"), 1000);

        if (noteId < 0 || rating < 1 || rating > 5) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            Note note = noteDAO.findById(noteId);
            if (note == null) { res.sendError(HttpServletResponse.SC_NOT_FOUND); return; }

            // Users cannot rate their own notes
            if (note.getUserId() == userId) {
                res.sendRedirect(req.getContextPath() + "/notes/view?id=" + noteId
                        + "&error=cannotRateOwn");
                return;
            }

            ratingDAO.upsert(noteId, userId, rating, comment);
            res.sendRedirect(req.getContextPath() + "/notes/view?id=" + noteId);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error submitting rating for note " + noteId, e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
