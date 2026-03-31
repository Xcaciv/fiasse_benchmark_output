package com.loosenotes.servlet;

import com.loosenotes.dao.ActivityLogDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.RatingDAO;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import com.loosenotes.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/ratings")
public class RatingServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(RatingServlet.class);
    private final RatingDAO ratingDAO = new RatingDAO();
    private final NoteDAO noteDAO = new NoteDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = getAuthenticatedUser(request);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Please login to rate notes");
            return;
        }
        
        String action = request.getParameter("action");
        Long noteId = getNoteIdFromRequest(request);
        
        if (noteId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Note ID required");
            return;
        }
        
        try {
            Note note = noteDAO.findById(noteId).orElse(null);
            if (note == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Note not found");
                return;
            }
            
            if (!note.isPublic() && !note.getUserId().equals(user.getId()) && !user.isAdmin()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Cannot rate this note");
                return;
            }
            
            if ("create".equals(action) || "update".equals(action)) {
                handleRatingCreateOrUpdate(request, response, user, noteId);
            } else if ("delete".equals(action)) {
                handleRatingDelete(request, response, user, noteId);
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid action");
            }
            
        } catch (SQLException e) {
            logger.error("Database error in RatingServlet", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred");
        }
    }
    
    private void handleRatingCreateOrUpdate(HttpServletRequest request, HttpServletResponse response, User user, Long noteId)
            throws SQLException, IOException, ServletException {
        String valueParam = request.getParameter("value");
        String comment = request.getParameter("comment");
        
        if (valueParam == null || valueParam.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Rating value required");
            return;
        }
        
        int value;
        try {
            value = Integer.parseInt(valueParam);
            if (value < 1 || value > 5) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Rating must be between 1 and 5");
                return;
            }
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid rating value");
            return;
        }
        
        Rating existingRating = ratingDAO.findByNoteIdAndUserId(noteId, user.getId()).orElse(null);
        
        if (existingRating != null) {
            existingRating.setValue(value);
            existingRating.setComment(comment != null ? comment.trim() : null);
            ratingDAO.update(existingRating);
            activityLogDAO.log(user.getId(), "RATING_UPDATED", "Note ID: " + noteId, getClientIp(request));
            logger.info("Rating updated: noteId={}, userId={}", noteId, user.getId());
        } else {
            Rating rating = new Rating(noteId, user.getId(), value, comment != null ? comment.trim() : null);
            ratingDAO.create(rating);
            activityLogDAO.log(user.getId(), "RATING_CREATED", "Note ID: " + noteId, getClientIp(request));
            logger.info("Rating created: noteId={}, userId={}", noteId, user.getId());
        }
        
        String referer = request.getHeader("Referer");
        if (referer != null) {
            response.sendRedirect(referer);
        } else {
            response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
        }
    }
    
    private void handleRatingDelete(HttpServletRequest request, HttpServletResponse response, User user, Long noteId)
            throws SQLException, IOException {
        Rating rating = ratingDAO.findByNoteIdAndUserId(noteId, user.getId()).orElse(null);
        
        if (rating == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Rating not found");
            return;
        }
        
        ratingDAO.delete(rating.getId());
        activityLogDAO.log(user.getId(), "RATING_DELETED", "Note ID: " + noteId, getClientIp(request));
        
        String referer = request.getHeader("Referer");
        if (referer != null) {
            response.sendRedirect(referer);
        } else {
            response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
        }
    }
    
    private User getAuthenticatedUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("user");
        }
        return null;
    }
    
    private Long getNoteIdFromRequest(HttpServletRequest request) {
        String idParam = request.getParameter("noteId");
        if (idParam != null && !idParam.isEmpty()) {
            try {
                return Long.parseLong(idParam);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
