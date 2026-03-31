package com.loosenotes.servlet;

import com.loosenotes.dao.ActivityLogDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.UserDAO;
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

@WebServlet("/admin")
public class AdminServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminServlet.class);
    private final UserDAO userDAO = new UserDAO();
    private final NoteDAO noteDAO = new NoteDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = getAuthenticatedUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        
        if (!user.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }
        
        String action = request.getParameter("action");
        
        try {
            switch (action == null ? "dashboard" : action) {
                case "users":
                    listUsers(request, response);
                    break;
                case "searchUsers":
                    searchUsers(request, response);
                    break;
                default:
                    showDashboard(request, response);
            }
        } catch (SQLException e) {
            logger.error("Database error in AdminServlet", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred");
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = getAuthenticatedUser(request);
        if (user == null || !user.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }
        
        String action = request.getParameter("action");
        
        try {
            if ("reassignNote".equals(action)) {
                reassignNoteOwnership(request, response, user);
            } else {
                response.sendRedirect(request.getContextPath() + "/admin");
            }
        } catch (SQLException e) {
            logger.error("Database error in AdminServlet", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred");
        }
    }
    
    private void showDashboard(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        int userCount = userDAO.count();
        int noteCount = noteDAO.count();
        List<ActivityLogDAO.ActivityLogEntry> recentActivity = activityLogDAO.findRecent(50);
        
        request.setAttribute("userCount", userCount);
        request.setAttribute("noteCount", noteCount);
        request.setAttribute("recentActivity", recentActivity);
        
        request.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(request, response);
    }
    
    private void listUsers(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        List<User> users = userDAO.findAll();
        request.setAttribute("users", users);
        request.getRequestDispatcher("/WEB-INF/views/admin/users.jsp").forward(request, response);
    }
    
    private void searchUsers(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        String query = request.getParameter("query");
        
        if (query == null || query.trim().isEmpty()) {
            listUsers(request, response);
            return;
        }
        
        List<User> users = userDAO.search(query.trim());
        request.setAttribute("users", users);
        request.setAttribute("query", query);
        request.getRequestDispatcher("/WEB-INF/views/admin/users.jsp").forward(request, response);
    }
    
    private void reassignNoteOwnership(HttpServletRequest request, HttpServletResponse response, User admin)
            throws SQLException, IOException {
        String noteIdParam = request.getParameter("noteId");
        String newOwnerIdParam = request.getParameter("newOwnerId");
        
        if (noteIdParam == null || newOwnerIdParam == null) {
            response.sendRedirect(request.getContextPath() + "/admin");
            return;
        }
        
        try {
            Long noteId = Long.parseLong(noteIdParam);
            Long newOwnerId = Long.parseLong(newOwnerIdParam);
            
            User newOwner = userDAO.findById(newOwnerId).orElse(null);
            if (newOwner == null) {
                response.sendRedirect(request.getContextPath() + "/admin?error=userNotFound");
                return;
            }
            
            noteDAO.updateOwner(noteId, newOwnerId);
            
            activityLogDAO.log(admin.getId(), "NOTE_REASSIGNED", 
                "Note ID: " + noteId + " reassigned to user: " + newOwner.getUsername(), 
                getClientIp(request));
            
            logger.info("Note {} reassigned to user {} by admin {}", noteId, newOwnerId, admin.getUsername());
            
        } catch (NumberFormatException e) {
            logger.error("Invalid note or user ID", e);
        }
        
        response.sendRedirect(request.getContextPath() + "/admin");
    }
    
    private User getAuthenticatedUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return (User) session.getAttribute("user");
        }
        return null;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
