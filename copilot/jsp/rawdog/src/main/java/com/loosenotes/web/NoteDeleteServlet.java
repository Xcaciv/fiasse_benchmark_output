package com.loosenotes.web;

import com.loosenotes.model.Attachment;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;
import com.loosenotes.util.FileUploadUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/notes/delete")
public class NoteDeleteServlet extends BaseServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireLogin(request, response) || !requireCsrf(request, response)) {
            return;
        }
        Long noteId = requireLongParameter(request, response, "id");
        if (noteId == null) {
            return;
        }
        Note note = app().getNoteDao().findById(noteId).orElse(null);
        if (note == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Note not found.");
            return;
        }
        User actor = currentUser(request);
        if (!canManage(note, actor)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "You are not allowed to delete this note.");
            return;
        }

        List<Attachment> attachments = app().getAttachmentDao().listByNoteId(noteId);
        try {
            FileUploadUtil.deleteStoredFiles(attachments);
            app().getShareLinkDao().revokeByNoteId(noteId);
            app().getNoteDao().delete(noteId);
            app().getActivityLogDao().log(actor.getId(), "note.deleted", "Deleted note #" + noteId + '.');
            setFlash(request, "success", "Note deleted permanently.");
            redirect(request, response, "/notes");
        } catch (IOException ex) {
            throw ex;
        }
    }
}
