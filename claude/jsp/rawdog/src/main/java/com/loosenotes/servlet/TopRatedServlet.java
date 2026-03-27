package com.loosenotes.servlet;

import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Note;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/top-rated")
public class TopRatedServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<Note> topRated = noteDAO.findTopRated(3);
        req.setAttribute("notes", topRated);
        req.getRequestDispatcher("/WEB-INF/jsp/note/top-rated.jsp").forward(req, resp);
    }
}
