package com.loosenotes.servlet;

import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Note;
import com.loosenotes.util.SessionUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

public class SearchServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String query = request.getParameter("q");
        HttpSession session = request.getSession(false);
        Long userId = SessionUtil.getUserId(session);

        if (query == null || query.trim().isEmpty()) {
            request.getRequestDispatcher("/WEB-INF/views/search.jsp").forward(request, response);
            return;
        }

        List<Note> results = noteDAO.searchNotes(query.trim(), userId);
        request.setAttribute("query", query);
        request.setAttribute("results", results);
        request.getRequestDispatcher("/WEB-INF/views/search.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/search?q=" + 
            java.net.URLEncoder.encode(request.getParameter("q"), "UTF-8"));
    }
}
