package com.loosenotes.web;

import com.loosenotes.model.NoteSummary;
import com.loosenotes.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = "/")
public class HomeServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = currentUser(request);
        String query = request.getParameter("q") == null ? "" : request.getParameter("q").trim();
        List<NoteSummary> results = app().getNoteDao().searchVisible(query, user == null ? null : user.getId());

        request.setAttribute("pageTitle", "Search Notes");
        request.setAttribute("query", query);
        request.setAttribute("results", results);
        render(request, response, "index");
    }
}
