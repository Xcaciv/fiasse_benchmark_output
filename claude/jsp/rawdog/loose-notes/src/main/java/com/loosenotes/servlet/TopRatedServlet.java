package com.loosenotes.servlet;

import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Note;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class TopRatedServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            List<Note> topRated = noteDAO.findTopRated(20);
            request.setAttribute("notes", topRated);
            request.getRequestDispatcher("/WEB-INF/jsp/topRated.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Top rated error", e);
            request.setAttribute("errorMessage", "Failed to load top rated notes.");
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
        }
    }
}
