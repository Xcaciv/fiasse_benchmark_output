package com.loosenotes.web;

import com.loosenotes.model.Note;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/ratings/save")
public class RatingSaveServlet extends BaseServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireLogin(request, response) || !requireCsrf(request, response)) {
            return;
        }
        Long noteId = requireLongParameter(request, response, "noteId");
        Integer ratingValue = null;
        if (noteId != null) {
            ratingValue = requireIntParameter(request, response, "rating");
        }
        if (noteId == null || ratingValue == null) {
            return;
        }
        String comment = request.getParameter("comment") == null ? "" : request.getParameter("comment").trim();
        String shareToken = request.getParameter("shareToken");
        Note note = app().getNoteDao().findById(noteId).orElse(null);
        if (note == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Note not found.");
            return;
        }
        if (!canView(note, currentUser(request), shareToken)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have access to rate this note.");
            return;
        }
        try {
            ValidationUtil.requireRating(ratingValue);
            app().getRatingDao().upsert(noteId, currentUser(request).getId(), ratingValue, comment);
            app().getActivityLogDao().log(currentUser(request).getId(), "note.rated", "Saved rating for note #" + noteId + '.');
            setFlash(request, "success", "Your rating has been saved.");
            String redirectUrl = "/notes/view?id=" + noteId;
            if (shareToken != null && !shareToken.isBlank()) {
                redirectUrl += "&shareToken=" + shareToken;
            }
            redirect(request, response, redirectUrl);
        } catch (IllegalArgumentException ex) {
            setFlash(request, "error", ex.getMessage());
            String redirectUrl = "/notes/view?id=" + noteId;
            if (shareToken != null && !shareToken.isBlank()) {
                redirectUrl += "&shareToken=" + shareToken;
            }
            redirect(request, response, redirectUrl);
        }
    }
}
