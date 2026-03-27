package com.loosenotes.servlet;

import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Note;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class TopRatedServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();
    private static final int MIN_RATINGS = 3;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<Note> topRatedNotes = noteDAO.findTopRated(MIN_RATINGS);
        request.setAttribute("topRatedNotes", topRatedNotes);
        request.setAttribute("minRatings", MIN_RATINGS);
        request.getRequestDispatcher("/WEB-INF/views/top-rated.jsp").forward(request, response);
    }
}
