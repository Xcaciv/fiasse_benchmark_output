package com.loosenotes.servlet;

import com.loosenotes.dao.AuditLogDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.UserDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/admin")
public class AdminDashboardServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final NoteDAO noteDAO = new NoteDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int userCount = userDAO.getTotalCount();
        int noteCount = noteDAO.getTotalCount();
        List<Map<String, Object>> recentActivity = auditLogDAO.getRecent(20);

        req.setAttribute("userCount", userCount);
        req.setAttribute("noteCount", noteCount);
        req.setAttribute("recentActivity", recentActivity);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/dashboard.jsp").forward(req, resp);
    }
}
