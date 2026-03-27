package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.RatingDAO;
import com.loosenotes.dao.ShareLinkDAO;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import com.loosenotes.model.ShareLink;
import com.loosenotes.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/notes/view")
public class NoteViewServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final RatingDAO ratingDAO = new RatingDAO();
    private final ShareLinkDAO shareLinkDAO = new ShareLinkDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User currentUser = (User) req.getSession().getAttribute("currentUser");
        int id = parseId(req.getParameter("id"));
        if (id <= 0) { resp.sendError(400); return; }

        Note note = noteDAO.findById(id);
        if (note == null) { resp.sendError(404); return; }

        // Access check: must be owner, admin, or note is public
        boolean canView = note.isPublic()
            || note.getUserId() == currentUser.getId()
            || currentUser.isAdmin();
        if (!canView) { resp.sendError(403); return; }

        List<Attachment> attachments = attachmentDAO.findByNoteId(id);
        List<Rating> ratings = ratingDAO.findByNoteId(id);
        ShareLink shareLink = shareLinkDAO.findByNoteId(id);
        Rating userRating = ratingDAO.findByNoteAndUser(id, currentUser.getId());

        boolean isOwner = note.getUserId() == currentUser.getId() || currentUser.isAdmin();

        req.setAttribute("note", note);
        req.setAttribute("attachments", attachments);
        req.setAttribute("ratings", ratings);
        req.setAttribute("shareLink", shareLink);
        req.setAttribute("userRating", userRating);
        req.setAttribute("isOwner", isOwner);
        req.getRequestDispatcher("/WEB-INF/jsp/note/view.jsp").forward(req, resp);
    }
}
