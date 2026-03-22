package com.loosenotes.servlet;

import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Note;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class SearchServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String query = request.getParameter("q");

        if (query == null || query.trim().isEmpty()) {
            request.getRequestDispatcher("/WEB-INF/jsp/search.jsp").forward(request, response);
            return;
        }

        query = query.trim();

        HttpSession session = request.getSession(false);
        Integer userId = null;
        if (session != null && session.getAttribute("userId") != null) {
            userId = (Integer) session.getAttribute("userId");
        }

        try {
            List<Note> results = noteDAO.search(query, userId);
            request.setAttribute("results", results);
            request.setAttribute("query", query);
            request.getRequestDispatcher("/WEB-INF/jsp/search.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Search error", e);
            request.setAttribute("error", "Search failed. Please try again.");
            request.setAttribute("query", query);
            request.getRequestDispatcher("/WEB-INF/jsp/search.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String query = request.getParameter("q");
        if (query == null) query = "";
        response.sendRedirect(request.getContextPath() + "/search?q=" + java.net.URLEncoder.encode(query, "UTF-8"));
    }
}
