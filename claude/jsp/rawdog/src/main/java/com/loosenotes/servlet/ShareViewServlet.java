package com.loosenotes.servlet;

import com.loosenotes.dao.AttachmentDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.RatingDAO;
import com.loosenotes.dao.ShareLinkDAO;
import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import com.loosenotes.model.ShareLink;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/share/*")
public class ShareViewServlet extends HttpServlet {

    private final ShareLinkDAO shareLinkDAO = new ShareLinkDAO();
    private final NoteDAO noteDAO = new NoteDAO();
    private final AttachmentDAO attachmentDAO = new AttachmentDAO();
    private final RatingDAO ratingDAO = new RatingDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            resp.sendError(404);
            return;
        }
        String token = pathInfo.substring(1); // strip leading "/"

        ShareLink shareLink = shareLinkDAO.findByToken(token);
        if (shareLink == null) {
            resp.sendError(404, "Share link not found or expired.");
            return;
        }

        Note note = noteDAO.findById(shareLink.getNoteId());
        if (note == null) {
            resp.sendError(404);
            return;
        }

        List<Attachment> attachments = attachmentDAO.findByNoteId(note.getId());
        List<Rating> ratings = ratingDAO.findByNoteId(note.getId());

        req.setAttribute("note", note);
        req.setAttribute("attachments", attachments);
        req.setAttribute("ratings", ratings);
        req.getRequestDispatcher("/WEB-INF/jsp/note/share-view.jsp").forward(req, resp);
    }
}
