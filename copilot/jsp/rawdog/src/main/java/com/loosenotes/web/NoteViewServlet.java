package com.loosenotes.web;

import com.loosenotes.model.Note;
import com.loosenotes.model.Rating;
import com.loosenotes.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/notes/view")
public class NoteViewServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Long noteId = requireLongParameter(request, response, "id");
        if (noteId == null) {
            return;
        }
        String shareToken = request.getParameter("shareToken");
        Note note = app().getNoteDao().findById(noteId).orElse(null);
        if (note == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Note not found.");
            return;
        }
        User user = currentUser(request);
        if (!canView(note, user, shareToken)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have access to this note.");
            return;
        }

        List<Rating> ratings = app().getRatingDao().listByNoteId(noteId);
        double average = ratings.stream().mapToInt(Rating::getRating).average().orElse(0.0);
        boolean isOwner = user != null && user.getId() == note.getUserId();

        request.setAttribute("pageTitle", note.getTitle());
        request.setAttribute("note", note);
        request.setAttribute("owner", app().getUserDao().findById(note.getUserId()).orElse(null));
        request.setAttribute("attachments", app().getAttachmentDao().listByNoteId(noteId));
        request.setAttribute("ratings", ratings);
        request.setAttribute("averageRating", average);
        request.setAttribute("canManage", canManage(note, user));
        request.setAttribute("isOwner", isOwner);
        request.setAttribute("shareLink", isOwner ? app().getShareLinkDao().findActiveByNoteId(noteId).orElse(null) : null);
        request.setAttribute("userRating", user == null ? null : app().getRatingDao().findByNoteAndUser(noteId, user.getId()).orElse(null));
        request.setAttribute("shareToken", shareToken);
        render(request, response, "notes/view");
    }
}
