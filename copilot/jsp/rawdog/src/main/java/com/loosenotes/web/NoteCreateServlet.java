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
import java.util.Collections;

@WebServlet("/notes/create")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 20 * 1024 * 1024)
public class NoteCreateServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        request.setAttribute("pageTitle", "Create Note");
        request.setAttribute("editing", false);
        request.setAttribute("attachments", Collections.emptyList());
        render(request, response, "notes/form");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireLogin(request, response) || !requireCsrf(request, response)) {
            return;
        }

        String title = request.getParameter("title") == null ? "" : request.getParameter("title").trim();
        String content = request.getParameter("content") == null ? "" : request.getParameter("content").trim();
        boolean isPublic = "on".equalsIgnoreCase(request.getParameter("isPublic"));

        request.setAttribute("pageTitle", "Create Note");
        request.setAttribute("editing", false);
        request.setAttribute("titleValue", title);
        request.setAttribute("contentValue", content);
        request.setAttribute("isPublicValue", isPublic);
        request.setAttribute("attachments", Collections.emptyList());

        try {
            ValidationUtil.requireTitle(title);
            ValidationUtil.requireContent(content);
            Note note = app().getNoteDao().create(currentUser(request).getId(), title, content, isPublic);
            FileUploadUtil.saveAttachments(note.getId(), request.getParts());
            app().getActivityLogDao().log(currentUser(request).getId(), "note.created", "Created note #" + note.getId() + '.');
            setFlash(request, "success", "Note created successfully.");
            redirect(request, response, "/notes/view?id=" + note.getId());
        } catch (IllegalArgumentException ex) {
            request.setAttribute("errorMessage", ex.getMessage());
            render(request, response, "notes/form");
        }
    }
}
