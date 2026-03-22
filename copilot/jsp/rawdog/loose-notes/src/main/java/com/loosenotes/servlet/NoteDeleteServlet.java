package com.loosenotes.servlet;

import com.loosenotes.dao.ActivityLogDAO;
import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.RatingDAO;
import com.loosenotes.dao.ShareLinkDAO;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.util.List;

@WebServlet("/notes/delete")
public class NoteDeleteServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final RatingDAO ratingDAO = new RatingDAO();
    private final ShareLinkDAO shareLinkDAO = new ShareLinkDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendRedirect(req.getContextPath() + "/notes");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int userId = (Integer) req.getSession().getAttribute("userId");
        boolean isAdmin = Boolean.TRUE.equals(req.getSession().getAttribute("isAdmin"));
        try {
            int noteId = Integer.parseInt(req.getParameter("noteId"));
            Note note = noteDAO.getNoteById(noteId);
            if (note == null || (note.getUserId() != userId && !isAdmin)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            // Delete physical attachment files
            List<Attachment> attachments = attachmentDAO.getAttachmentsByNote(noteId);
            String uploadDir = getServletContext().getRealPath("/") + "uploads";
            for (Attachment att : attachments) {
                new File(uploadDir + File.separator + att.getStoredFilename()).delete();
            }

            attachmentDAO.deleteAttachmentsByNote(noteId);
            ratingDAO.deleteRatingsByNote(noteId);
            shareLinkDAO.deleteShareLinkByNote(noteId);
            noteDAO.deleteNote(noteId);

            activityLogDAO.log(userId, "NOTE_DELETE", "Deleted note id=" + noteId + " title=" + note.getTitle());

            HttpSession session = req.getSession();
            session.setAttribute("successMessage", "Note deleted successfully.");
            resp.sendRedirect(req.getContextPath() + "/notes");
        } catch (Exception e) {
            req.setAttribute("error", "Error deleting note: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
        }
    }
}
