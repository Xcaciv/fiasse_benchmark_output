package com.loosenotes.servlet;

import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/top-rated")
public class TopRatedServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(TopRatedServlet.class);
    private final NoteDAO noteDAO = new NoteDAO();
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            List<Note> topNotes = noteDAO.findTopRated(20);
            request.setAttribute("topNotes", topNotes);
            request.getRequestDispatcher("/WEB-INF/views/top-rated.jsp").forward(request, response);
        } catch (SQLException e) {
            logger.error("Database error in TopRatedServlet", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred");
        }
    }
    
    private User getAuthenticatedUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("user");
        }
        return null;
    }
}
