package com.loosenotes.web;

import com.loosenotes.model.Note;
import com.loosenotes.util.FileUploadUtil;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/notes/edit")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 20 * 1024 * 1024)
public class NoteEditServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireLogin(request, response)) {
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
        if (currentUser(request).getId() != note.getUserId()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only the note owner can edit this note.");
            return;
        }

        request.setAttribute("pageTitle", "Edit Note");
        request.setAttribute("editing", true);
        request.setAttribute("note", note);
        request.setAttribute("attachments", app().getAttachmentDao().listByNoteId(noteId));
        render(request, response, "notes/form");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
        if (currentUser(request).getId() != note.getUserId()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Only the note owner can edit this note.");
            return;
        }

        String title = request.getParameter("title") == null ? "" : request.getParameter("title").trim();
        String content = request.getParameter("content") == null ? "" : request.getParameter("content").trim();
        boolean isPublic = "on".equalsIgnoreCase(request.getParameter("isPublic"));

        request.setAttribute("pageTitle", "Edit Note");
        request.setAttribute("editing", true);
        request.setAttribute("attachments", app().getAttachmentDao().listByNoteId(noteId));
        request.setAttribute("titleValue", title);
        request.setAttribute("contentValue", content);
        request.setAttribute("isPublicValue", isPublic);

        try {
            ValidationUtil.requireTitle(title);
            ValidationUtil.requireContent(content);
            app().getNoteDao().update(noteId, title, content, isPublic);
            FileUploadUtil.saveAttachments(noteId, request.getParts());
            request.setAttribute("note", app().getNoteDao().findById(noteId).orElseThrow());
            app().getActivityLogDao().log(currentUser(request).getId(), "note.updated", "Updated note #" + noteId + '.');
            setFlash(request, "success", "Note updated successfully.");
            redirect(request, response, "/notes/view?id=" + noteId);
        } catch (IllegalArgumentException ex) {
            request.setAttribute("note", note);
            request.setAttribute("errorMessage", ex.getMessage());
            render(request, response, "notes/form");
        }
    }
}
