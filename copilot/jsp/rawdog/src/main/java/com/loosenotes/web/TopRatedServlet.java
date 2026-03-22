package com.loosenotes.web;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/top-rated")
public class TopRatedServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("pageTitle", "Top Rated Notes");
        request.setAttribute("notes", app().getNoteDao().topRatedPublicNotes());
        render(request, response, "top-rated");
    }
}
