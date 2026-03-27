package com.loosenotes.servlet;

import com.loosenotes.dao.NoteDAO;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getSession().getAttribute("user");
        try {
            List<Note> recentNotes = noteDAO.findRecentByUserId(user.getId(), 5);
            int totalNotes = noteDAO.countByUserId(user.getId());
            request.setAttribute("recentNotes", recentNotes);
            request.setAttribute("totalNotes", totalNotes);
            request.getRequestDispatcher("/dashboard.jsp").forward(request, response);
        } catch (Exception e) {
            request.setAttribute("errorMessage", "Error loading dashboard: " + e.getMessage());
            request.getRequestDispatcher("/dashboard.jsp").forward(request, response);
        }
    }
}
