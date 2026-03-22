package com.loosenotes.servlet;

import com.loosenotes.dao.ActivityLogDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.UserDAO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/admin")
public class AdminDashboardServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final NoteDAO noteDAO = new NoteDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            int userCount = userDAO.getUserCount();
            int noteCount = noteDAO.getNoteCount();
            var recentActivity = activityLogDAO.getRecentActivity(20);

            req.setAttribute("userCount", userCount);
            req.setAttribute("noteCount", noteCount);
            req.setAttribute("recentActivity", recentActivity);
            req.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(req, resp);
        } catch (Exception e) {
            req.setAttribute("error", "Error loading dashboard: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
        }
    }
}
