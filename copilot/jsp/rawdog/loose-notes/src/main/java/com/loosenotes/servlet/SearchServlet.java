package com.loosenotes.servlet;

import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Note;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/search")
public class SearchServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String query = req.getParameter("q");
        List<Note> results = new ArrayList<>();

        HttpSession session = req.getSession(false);
        int currentUserId = -1;
        if (session != null && session.getAttribute("userId") != null) {
            currentUserId = (Integer) session.getAttribute("userId");
        }

        if (query != null && !query.trim().isEmpty()) {
            try {
                results = noteDAO.searchNotes(query.trim(), currentUserId);
            } catch (Exception e) {
                req.setAttribute("error", "Search error: " + e.getMessage());
            }
        }
        req.setAttribute("results", results);
        req.setAttribute("query", query);
        req.getRequestDispatcher("/WEB-INF/views/search/results.jsp").forward(req, resp);
    }
}
