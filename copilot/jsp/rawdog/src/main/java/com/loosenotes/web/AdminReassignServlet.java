package com.loosenotes.web;

import com.loosenotes.model.Note;
import com.loosenotes.model.User;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/admin/reassign")
public class AdminReassignServlet extends BaseServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireAdmin(request, response) || !requireCsrf(request, response)) {
            return;
        }
        Long noteId = requireLongParameter(request, response, "noteId");
        Integer userId = null;
        if (noteId != null) {
            userId = requireIntParameter(request, response, "userId");
        }
        if (noteId == null || userId == null) {
            return;
        }
        Note note = app().getNoteDao().findById(noteId).orElse(null);
        if (note == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Note not found.");
            return;
        }
        User newOwner = app().getUserDao().findById(userId.longValue()).orElse(null);
        if (newOwner == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "User not found.");
            return;
        }
        app().getNoteDao().reassignOwner(noteId, userId.longValue());
        app().getActivityLogDao().log(currentUser(request).getId(), "admin.note_reassigned", "Reassigned note #" + noteId + " to " + newOwner.getUsername() + '.');
        setFlash(request, "success", "Note ownership updated.");
        redirect(request, response, "/admin");
    }
}
