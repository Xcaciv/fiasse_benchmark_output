package com.loosenotes.servlet;

import com.loosenotes.dao.ActivityLogDAO;
import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.RatingDAO;
import com.loosenotes.dao.ShareLinkDAO;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import com.loosenotes.model.ShareLink;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class ShareServlet extends HttpServlet {

    private final ShareLinkDAO shareLinkDAO = new ShareLinkDAO();
    private final NoteDAO noteDAO = new NoteDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final RatingDAO ratingDAO = new RatingDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String token = request.getParameter("token");
        if (token == null || token.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        try {
            ShareLink shareLink = shareLinkDAO.findByToken(token);
            if (shareLink == null) {
                request.setAttribute("errorMessage", "This share link is invalid or has been revoked.");
                request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
                return;
            }

            Note note = noteDAO.findById(shareLink.getNoteId());
            if (note == null) {
                request.setAttribute("errorMessage", "The note associated with this link no longer exists.");
                request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
                return;
            }

            List<Attachment> attachments = attachmentDAO.findByNoteId(note.getId());
            List<Rating> ratings = ratingDAO.findByNoteId(note.getId());
            double avgRating = ratingDAO.getAverageRating(note.getId());
            int ratingCount = ratingDAO.getRatingCount(note.getId());
            note.setAverageRating(avgRating);
            note.setRatingCount(ratingCount);

            request.setAttribute("note", note);
            request.setAttribute("attachments", attachments);
            request.setAttribute("ratings", ratings);
            request.getRequestDispatcher("/WEB-INF/jsp/shareView.jsp").forward(request, response);

        } catch (SQLException e) {
            getServletContext().log("Share view error", e);
            request.setAttribute("errorMessage", "Failed to load shared note.");
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        int userId = (Integer) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");

        String action = request.getParameter("action");
        String noteIdParam = request.getParameter("noteId");

        if (noteIdParam == null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }

        try {
            int noteId = Integer.parseInt(noteIdParam);
            Note note = noteDAO.findById(noteId);

            if (note == null || (note.getUserId() != userId && !"ADMIN".equals(role))) {
                response.sendRedirect(request.getContextPath() + "/dashboard");
                return;
            }

            if ("generate".equals(action)) {
                // Remove existing share link if any
                shareLinkDAO.deleteByNoteId(noteId);
                // Create new share link
                String token = UUID.randomUUID().toString();
                shareLinkDAO.create(noteId, token);
                activityLogDAO.log(userId, "GENERATE_SHARE_LINK", "Generated share link for note id=" + noteId);
                response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId + "&success=Share+link+generated");

            } else if ("revoke".equals(action)) {
                shareLinkDAO.deleteByNoteId(noteId);
                activityLogDAO.log(userId, "REVOKE_SHARE_LINK", "Revoked share link for note id=" + noteId);
                response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId + "&success=Share+link+revoked");

            } else {
                response.sendRedirect(request.getContextPath() + "/notes?action=view&id=" + noteId);
            }

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (SQLException e) {
            getServletContext().log("Share action error", e);
            response.sendRedirect(request.getContextPath() + "/dashboard?error=Share+action+failed");
        }
    }
}
