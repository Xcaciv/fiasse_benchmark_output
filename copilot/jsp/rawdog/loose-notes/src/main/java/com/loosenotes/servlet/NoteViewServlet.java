package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.RatingDAO;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/notes/view")
public class NoteViewServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();
    private final RatingDAO ratingDAO = new RatingDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            int noteId = Integer.parseInt(req.getParameter("id"));
            Note note = noteDAO.getNoteById(noteId);
            if (note == null) {
                req.setAttribute("error", "Note not found.");
                req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
                return;
            }

            HttpSession session = req.getSession(false);
            Integer sessionUserId = (session != null) ? (Integer) session.getAttribute("userId") : null;

            if (!note.isPublic() && (sessionUserId == null || sessionUserId != note.getUserId())) {
                boolean isAdmin = session != null && Boolean.TRUE.equals(session.getAttribute("isAdmin"));
                if (!isAdmin) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
            }

            List<Rating> ratings = ratingDAO.getRatingsByNote(noteId);
            double avgRating = ratingDAO.getAverageRating(noteId);
            int ratingCount = ratingDAO.getRatingCount(noteId);
            Rating userRating = null;
            if (sessionUserId != null) {
                userRating = ratingDAO.getRatingByNoteAndUser(noteId, sessionUserId);
            }

            req.setAttribute("note", note);
            req.setAttribute("ratings", ratings);
            req.setAttribute("avgRating", avgRating);
            req.setAttribute("ratingCount", ratingCount);
            req.setAttribute("userRating", userRating);
            req.setAttribute("attachments", attachmentDAO.getAttachmentsByNote(noteId));
            req.getRequestDispatcher("/WEB-INF/views/notes/view.jsp").forward(req, resp);
        } catch (Exception e) {
            req.setAttribute("error", "Error loading note: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
        }
    }
}
