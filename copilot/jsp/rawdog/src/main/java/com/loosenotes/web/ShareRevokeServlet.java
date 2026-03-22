package com.loosenotes.web;

import com.loosenotes.model.Note;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/notes/share/revoke")
public class ShareRevokeServlet extends BaseServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireLogin(request, response) || !requireCsrf(request, response)) {
            return;
        }
        Long noteId = requireLongParameter(request, response, "noteId");
        if (noteId == null) {
            return;
        }
        Note note = app().getNoteDao().findById(noteId).orElse(null);
        if (note == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Note not found.");
            return;
        }
        if (currentUser(request).getId() != note.getUserId()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only the note owner can manage share links.");
            return;
        }
        app().getShareLinkDao().revokeByNoteId(noteId);
        app().getActivityLogDao().log(currentUser(request).getId(), "note.share_revoked", "Revoked share links for note #" + noteId + '.');
        setFlash(request, "success", "Share link revoked.");
        redirect(request, response, "/notes/view?id=" + noteId);
    }
}
