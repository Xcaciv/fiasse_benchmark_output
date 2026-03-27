package com.loosenotes.servlet.notes;

import com.loosenotes.model.Note;
import com.loosenotes.service.NoteService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@WebServlet("/notes/search")
public class NoteSearchServlet extends HttpServlet {
    private final NoteService noteService = new NoteService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String query = req.getParameter("q");
        List<Note> results = Collections.emptyList();
        int page = 1;
        int pageSize = 10;

        if (query != null && !query.isBlank() && query.length() <= 200) {
            try { page = Integer.parseInt(req.getParameter("page")); if (page < 1) page = 1; } catch (Exception e) { page = 1; }
            results = noteService.searchPublicNotes(query.trim(), page, pageSize);
        }

        req.setAttribute("results", results);
        req.setAttribute("query", query);
        req.setAttribute("currentPage", page);
        req.getRequestDispatcher("/WEB-INF/jsp/notes/search.jsp").forward(req, resp);
    }
}
