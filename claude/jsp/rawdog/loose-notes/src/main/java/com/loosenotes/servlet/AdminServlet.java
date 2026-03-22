package com.loosenotes.servlet;

import com.loosenotes.dao.ActivityLogDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.ActivityLog;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final NoteDAO noteDAO = new NoteDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");

        if ("users".equals(action)) {
            handleUserList(request, response);
        } else {
            handleDashboard(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");

        if ("reassign".equals(action)) {
            handleReassign(request, response);
        } else {
            response.sendRedirect(request.getContextPath() + "/admin");
        }
    }

    private void handleDashboard(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            int userCount = userDAO.countAll();
            int noteCount = noteDAO.countAll();
            List<ActivityLog> recentActivity = activityLogDAO.findRecent(20);

            request.setAttribute("userCount", userCount);
            request.setAttribute("noteCount", noteCount);
            request.setAttribute("recentActivity", recentActivity);

            request.getRequestDispatcher("/WEB-INF/jsp/admin/dashboard.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Admin dashboard error", e);
            request.setAttribute("errorMessage", "Failed to load admin dashboard.");
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
        }
    }

    private void handleUserList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String searchQuery = request.getParameter("search");
        try {
            List<User> users;
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                users = userDAO.search(searchQuery.trim());
                request.setAttribute("searchQuery", searchQuery.trim());
            } else {
                users = userDAO.findAll();
            }

            // Build note count map
            Map<Integer, Integer> noteCounts = new HashMap<>();
            for (User user : users) {
                noteCounts.put(user.getId(), userDAO.getNoteCount(user.getId()));
            }

            request.setAttribute("users", users);
            request.setAttribute("noteCounts", noteCounts);

            // Also load all notes for reassignment
            List<Note> allNotes = new ArrayList<>();
            for (User user : users) {
                allNotes.addAll(noteDAO.findByUserId(user.getId()));
            }
            request.setAttribute("allNotes", allNotes);

            request.getRequestDispatcher("/WEB-INF/jsp/admin/users.jsp").forward(request, response);
        } catch (SQLException e) {
            getServletContext().log("Admin user list error", e);
            request.setAttribute("errorMessage", "Failed to load users.");
            request.getRequestDispatcher("/WEB-INF/jsp/error.jsp").forward(request, response);
        }
    }

    private void handleReassign(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        int adminId = (Integer) session.getAttribute("userId");

        String noteIdParam = request.getParameter("noteId");
        String newUserIdParam = request.getParameter("newUserId");

        if (noteIdParam == null || newUserIdParam == null) {
            response.sendRedirect(request.getContextPath() + "/admin?action=users");
            return;
        }

        try {
            int noteId = Integer.parseInt(noteIdParam);
            int newUserId = Integer.parseInt(newUserIdParam);

            Note note = noteDAO.findById(noteId);
            User newUser = userDAO.findById(newUserId);

            if (note == null || newUser == null) {
                response.sendRedirect(request.getContextPath() + "/admin?action=users&error=Note+or+user+not+found");
                return;
            }

            noteDAO.reassign(noteId, newUserId);
            activityLogDAO.log(adminId, "ADMIN_REASSIGN_NOTE",
                    "Admin reassigned note id=" + noteId + " to user id=" + newUserId + " (" + newUser.getUsername() + ")");

            response.sendRedirect(request.getContextPath() + "/admin?action=users&success=Note+reassigned+successfully");

        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/admin?action=users");
        } catch (SQLException e) {
            getServletContext().log("Admin reassign error", e);
            response.sendRedirect(request.getContextPath() + "/admin?action=users&error=Reassignment+failed");
        }
    }
}
