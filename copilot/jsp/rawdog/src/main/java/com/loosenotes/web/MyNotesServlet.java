package com.loosenotes.web;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/notes")
public class MyNotesServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        request.setAttribute("pageTitle", "My Notes");
        request.setAttribute("notes", app().getNoteDao().listOwnedByUser(currentUser(request).getId()));
        render(request, response, "notes/list");
    }
}
