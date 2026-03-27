package com.loosenotes.servlet.notes;

import com.loosenotes.model.Note;
import com.loosenotes.service.NoteService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet("/notes/list")
public class NoteListServlet extends HttpServlet {
    private final NoteService noteService = new NoteService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Long userId = (Long) req.getSession().getAttribute("userId");
        int page = parsePageParam(req.getParameter("page"));
        int pageSize = 10;
        List<Note> notes = noteService.getUserNotes(userId, page, pageSize);
        long total = noteService.countUserNotes(userId);
        int totalPages = (int) Math.ceil((double) total / pageSize);
        req.setAttribute("notes", notes);
        req.setAttribute("currentPage", page);
        req.setAttribute("totalPages", totalPages);
        req.getRequestDispatcher("/WEB-INF/jsp/notes/list.jsp").forward(req, resp);
    }

    private int parsePageParam(String param) {
        try {
            int p = Integer.parseInt(param);
            return p > 0 ? p : 1;
        } catch (Exception e) {
            return 1;
        }
    }
}
