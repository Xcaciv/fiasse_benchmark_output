package com.loosenotes.servlet.notes;

import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import com.loosenotes.service.AttachmentService;
import com.loosenotes.service.NoteService;
import com.loosenotes.service.RatingService;
import com.loosenotes.service.ShareLinkService;
import com.loosenotes.model.ShareLink;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@WebServlet("/notes/view")
public class NoteViewServlet extends HttpServlet {
    private final NoteService noteService = new NoteService();
    private final AttachmentService attachmentService = new AttachmentService();
    private final RatingService ratingService = new RatingService();
    private final ShareLinkService shareLinkService = new ShareLinkService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idParam = req.getParameter("id");
        Long userId = (Long) req.getSession().getAttribute("userId");

        if (idParam == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        long noteId;
        try {
            noteId = Long.parseLong(idParam);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Optional<Note> noteOpt = noteService.findById(noteId);
        if (noteOpt.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Note note = noteOpt.get();
        // Privacy check: private notes only accessible to owner
        if (!"PUBLIC".equals(note.getVisibility()) && (userId == null || !note.getUserId().equals(userId))) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        List<Attachment> attachments = attachmentService.findByNoteId(noteId);
        List<Rating> ratings = ratingService.findByNoteId(noteId);
        Optional<Rating> userRating = userId != null ? ratingService.findUserRating(noteId, userId) : Optional.empty();
        Optional<ShareLink> shareLink = userId != null && note.getUserId().equals(userId) ?
                shareLinkService.findByNoteId(noteId) : Optional.empty();

        req.setAttribute("note", note);
        req.setAttribute("attachments", attachments);
        req.setAttribute("ratings", ratings);
        req.setAttribute("userRating", userRating.orElse(null));
        req.setAttribute("shareLink", shareLink.orElse(null));
        req.setAttribute("isOwner", userId != null && note.getUserId().equals(userId));
        req.getRequestDispatcher("/WEB-INF/jsp/notes/view.jsp").forward(req, resp);
    }
}
