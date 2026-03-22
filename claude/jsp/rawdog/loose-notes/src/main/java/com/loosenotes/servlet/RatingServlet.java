package com.loosenotes.servlet;

import com.loosenotes.dao.ActivityLogDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.RatingDAO;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

public class RatingServlet extends HttpServlet {

    private final RatingDAO ratingDAO = new RatingDAO();
    private final NoteDAO noteDAO = new NoteDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        int userId = (Integer) session.getAttribute("userId");

        String action = request.getParameter("action");
        String noteIdParam = request.getParameter("noteId");
        String ratingParam = request.getParameter("rating");
        String comment = request.getParameter("comment");

        if (noteIdParam == null || ratingParam == null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        try {
            int noteId = Integer.parseInt(noteIdParam);
            int ratingValue = Integer.parseInt(ratingParam);

            if (ratingValue < 1 || ratingValue > 5) {
                response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId + "&error=Rating+must+be+between+1+and+5");
                return;
            }

            Note note = noteDAO.findById(noteId);
            if (note == null) {
                response.sendRedirect(request.getContextPath() + "/dashboard");
                return;
            }

            // Cannot rate your own note
            if (note.getUserId() == userId) {
                response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId + "&error=You+cannot+rate+your+own+note");
                return;
            }

            if ("add".equals(action)) {
                // Check if user already rated this note
                Rating existing = ratingDAO.findByNoteAndUser(noteId, userId);
                if (existing != null) {
                    // Update instead
                    ratingDAO.update(existing.getId(), ratingValue, comment);
                    activityLogDAO.log(userId, "UPDATE_RATING", "Updated rating for note id=" + noteId);
                    response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId + "&success=Rating+updated");
                } else {
                    ratingDAO.create(noteId, userId, ratingValue, comment);
                    activityLogDAO.log(userId, "ADD_RATING", "Rated note id=" + noteId + " with " + ratingValue + " stars");
                    response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId + "&success=Rating+added");
                }
            } else if ("edit".equals(action)) {
                String ratingIdParam = request.getParameter("ratingId");
                if (ratingIdParam == null) {
                    response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
                    return;
                }
                int ratingId = Integer.parseInt(ratingIdParam);
                Rating existing = ratingDAO.findById(ratingId);
                if (existing == null || existing.getUserId() != userId) {
                    response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId + "&error=Rating+not+found");
                    return;
                }
                ratingDAO.update(ratingId, ratingValue, comment);
                activityLogDAO.log(userId, "UPDATE_RATING", "Updated rating for note id=" + noteId);
                response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId + "&success=Rating+updated");
            } else {
                response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
            }

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (SQLException e) {
            getServletContext().log("Rating error", e);
            response.sendRedirect(request.getContextPath() + "/dashboard?error=Rating+action+failed");
        }
    }
}
