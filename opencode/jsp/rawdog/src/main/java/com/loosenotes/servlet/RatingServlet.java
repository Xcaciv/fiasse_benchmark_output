package com.loosenotes.servlet;

import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.RatingDAO;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import com.loosenotes.util.LoggerUtil;
import com.loosenotes.util.SessionUtil;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RatingServlet extends HttpServlet {

    private final RatingDAO ratingDAO = new RatingDAO();
    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        
        if (!SessionUtil.isLoggedIn(session)) {
            response.sendRedirect(request.getContextPath() + "/auth");
            return;
        }

        Long userId = SessionUtil.getUserId(session);
        Long noteId = parseLong(request.getParameter("noteId"));
        String action = request.getParameter("action");

        if (noteId == null) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        if ("submit".equals(action)) {
            submitRating(request, response, userId, noteId);
        } else if ("update".equals(action)) {
            updateRating(request, response, userId, noteId);
        } else if ("delete".equals(action)) {
            deleteRating(request, response, userId, noteId);
        } else {
            response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
        }
    }

    private void submitRating(HttpServletRequest request, HttpServletResponse response, Long userId, Long noteId)
            throws IOException {
        String ratingStr = request.getParameter("rating");
        String comment = request.getParameter("comment");

        int ratingValue;
        try {
            ratingValue = Integer.parseInt(ratingStr);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
            return;
        }

        if (!ValidationUtil.isValidRating(ratingValue)) {
            response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
            return;
        }

        noteDAO.findById(noteId).ifPresentOrElse(
            note -> {
                Rating rating = new Rating(noteId, userId, ratingValue, 
                    ValidationUtil.sanitize(comment));
                
                if (ratingDAO.createRating(rating)) {
                    LoggerUtil.logRatingCreate(userId, noteId, ratingValue, request);
                }
                
                try {
                    response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            },
            () -> {
                try {
                    response.sendRedirect(request.getContextPath() + "/");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    private void updateRating(HttpServletRequest request, HttpServletResponse response, Long userId, Long noteId)
            throws IOException {
        Long ratingId = parseLong(request.getParameter("ratingId"));
        String ratingStr = request.getParameter("rating");
        String comment = request.getParameter("comment");

        if (ratingId == null) {
            response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
            return;
        }

        int ratingValue;
        try {
            ratingValue = Integer.parseInt(ratingStr);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
            return;
        }

        if (!ValidationUtil.isValidRating(ratingValue)) {
            response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
            return;
        }

        ratingDAO.findById(ratingId).ifPresentOrElse(
            rating -> {
                if (!rating.getUserId().equals(userId)) {
                    try {
                        response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                rating.setRating(ratingValue);
                rating.setComment(ValidationUtil.sanitize(comment));

                if (ratingDAO.updateRating(rating)) {
                    LoggerUtil.logRatingUpdate(userId, noteId, ratingValue, request);
                }
                
                try {
                    response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            },
            () -> {
                try {
                    response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    private void deleteRating(HttpServletRequest request, HttpServletResponse response, Long userId, Long noteId)
            throws IOException {
        Long ratingId = parseLong(request.getParameter("ratingId"));

        if (ratingId == null) {
            response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
            return;
        }

        ratingDAO.findById(ratingId).ifPresentOrElse(
            rating -> {
                if (!rating.getUserId().equals(userId)) {
                    try {
                        response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return;
                }

                ratingDAO.deleteRating(ratingId);
                
                try {
                    response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            },
            () -> {
                try {
                    response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    private Long parseLong(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
