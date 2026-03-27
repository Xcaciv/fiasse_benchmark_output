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

@WebServlet("/top-rated")
public class TopRatedServlet extends HttpServlet {
    private final NoteService noteService = new NoteService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Cache-Control", "public, max-age=60");
        List<Note> notes = noteService.getTopRatedNotes(20);
        req.setAttribute("notes", notes);
        req.getRequestDispatcher("/WEB-INF/jsp/notes/top-rated.jsp").forward(req, resp);
    }
}
