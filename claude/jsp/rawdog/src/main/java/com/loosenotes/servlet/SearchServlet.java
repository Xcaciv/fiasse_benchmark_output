package com.loosenotes.servlet;

import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/search")
public class SearchServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User currentUser = (User) req.getSession().getAttribute("currentUser");
        String keyword = req.getParameter("q");
        List<Note> results = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            results = noteDAO.search(keyword.trim(), currentUser.getId());
        }

        req.setAttribute("keyword", keyword);
        req.setAttribute("results", results);
        req.getRequestDispatcher("/WEB-INF/jsp/note/search.jsp").forward(req, resp);
    }
}
