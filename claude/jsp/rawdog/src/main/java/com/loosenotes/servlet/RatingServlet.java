package com.loosenotes.servlet;

import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.RatingDAO;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import com.loosenotes.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/notes/rate")
public class RatingServlet extends HttpServlet {

    private final RatingDAO ratingDAO = new RatingDAO();
    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User currentUser = (User) req.getSession().getAttribute("currentUser");

        int noteId = parseId(req.getParameter("noteId"));
        int ratingValue = parseId(req.getParameter("rating"));
        String comment = req.getParameter("comment");

        if (noteId <= 0 || ratingValue < 1 || ratingValue > 5) {
            resp.sendError(400, "Invalid rating or note id.");
            return;
        }

        Note note = noteDAO.findById(noteId);
        if (note == null) { resp.sendError(404); return; }

        // Cannot rate own note
        if (note.getUserId() == currentUser.getId()) {
            req.getSession().setAttribute("flash_error", "You cannot rate your own note.");
            resp.sendRedirect(req.getContextPath() + "/notes/view?id=" + noteId);
            return;
        }

        // Must be able to view the note to rate it
        if (!note.isPublic() && note.getUserId() != currentUser.getId() && !currentUser.isAdmin()) {
            resp.sendError(403); return;
        }

        Rating existing = ratingDAO.findByNoteAndUser(noteId, currentUser.getId());
        if (existing != null) {
            existing.setRating(ratingValue);
            existing.setComment(comment != null ? comment.trim() : null);
            ratingDAO.update(existing);
        } else {
            Rating rating = new Rating();
            rating.setNoteId(noteId);
            rating.setUserId(currentUser.getId());
            rating.setRating(ratingValue);
            rating.setComment(comment != null ? comment.trim() : null);
            ratingDAO.create(rating);
        }

        req.getSession().setAttribute("flash_success", "Rating submitted.");
        resp.sendRedirect(req.getContextPath() + "/notes/view?id=" + noteId);
    }

    private int parseId(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return -1; }
    }
}
