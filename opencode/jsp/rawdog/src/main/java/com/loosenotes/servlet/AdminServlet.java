package com.loosenotes.servlet;

import com.loosenotes.dao.ActivityLogDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.ActivityLog;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;
import com.loosenotes.util.LoggerUtil;
import com.loosenotes.util.SessionUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

public class AdminServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final NoteDAO noteDAO = new NoteDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (!SessionUtil.isAdmin(session)) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        String action = request.getParameter("action");
        Long adminId = SessionUtil.getUserId(session);

        if (action == null) {
            showDashboard(request, response, adminId);
        } else if ("users".equals(action)) {
            listUsers(request, response, adminId);
        } else if ("notes".equals(action)) {
            listAllNotes(request, response, adminId);
        } else if ("reassign".equals(action)) {
            showReassignView(request, response, adminId);
        } else if ("activity".equals(action)) {
            showActivity(request, response, adminId);
        } else {
            showDashboard(request, response, adminId);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        
        if (!SessionUtil.isAdmin(session)) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }

        Long adminId = SessionUtil.getUserId(session);
        String action = request.getParameter("action");

        if ("reassign".equals(action)) {
            reassignNote(request, response, adminId);
        } else if ("searchUsers".equals(action)) {
            searchUsers(request, response, adminId);
        } else {
            showDashboard(request, response, adminId);
        }
    }

    private void showDashboard(HttpServletRequest request, HttpServletResponse response, Long adminId)
            throws ServletException, IOException {
        int totalUsers = userDAO.getTotalCount();
        int totalNotes = noteDAO.getTotalCount();
        List<ActivityLog> recentActivity = activityLogDAO.findRecent(20);

        request.setAttribute("totalUsers", totalUsers);
        request.setAttribute("totalNotes", totalNotes);
        request.setAttribute("recentActivity", recentActivity);
        request.getRequestDispatcher("/WEB-INF/views/admin-dashboard.jsp").forward(request, response);
    }

    private void listUsers(HttpServletRequest request, HttpServletResponse response, Long adminId)
            throws ServletException, IOException {
        List<User> users = userDAO.findAll();
        request.setAttribute("users", users);
        request.getRequestDispatcher("/WEB-INF/views/admin-users.jsp").forward(request, response);
    }

    private void listAllNotes(HttpServletRequest request, HttpServletResponse response, Long adminId)
            throws ServletException, IOException {
        List<Note> publicNotes = noteDAO.findPublicNotes();
        request.setAttribute("notes", publicNotes);
        request.getRequestDispatcher("/WEB-INF/views/admin-notes.jsp").forward(request, response);
    }

    private void showReassignView(HttpServletRequest request, HttpServletResponse response, Long adminId)
            throws ServletException, IOException {
        Long noteId = parseLong(request.getParameter("id"));
        if (noteId == null) {
            response.sendRedirect(request.getContextPath() + "/admin?action=notes");
            return;
        }

        noteDAO.findById(noteId).ifPresentOrElse(
            note -> {
                List<User> users = userDAO.findAll();
                request.setAttribute("note", note);
                request.setAttribute("users", users);
                try {
                    request.getRequestDispatcher("/WEB-INF/views/admin-reassign.jsp").forward(request, response);
                } catch (ServletException | IOException e) {
                    throw new RuntimeException(e);
                }
            },
            () -> {
                try {
                    response.sendRedirect(request.getContextPath() + "/admin?action=notes");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    private void reassignNote(HttpServletRequest request, HttpServletResponse response, Long adminId)
            throws IOException {
        Long noteId = parseLong(request.getParameter("noteId"));
        Long newOwnerId = parseLong(request.getParameter("newOwnerId"));

        if (noteId == null || newOwnerId == null) {
            response.sendRedirect(request.getContextPath() + "/admin?action=notes");
            return;
        }

        if (noteDAO.reassignOwner(noteId, newOwnerId)) {
            LoggerUtil.logAdminNoteReassign(adminId, noteId, newOwnerId, request);
        }
        
        response.sendRedirect(request.getContextPath() + "/admin?action=notes");
    }

    private void showActivity(HttpServletRequest request, HttpServletResponse response, Long adminId)
            throws ServletException, IOException {
        List<ActivityLog> recentActivity = activityLogDAO.findRecent(50);
        request.setAttribute("recentActivity", recentActivity);
        request.getRequestDispatcher("/WEB-INF/views/admin-activity.jsp").forward(request, response);
    }

    private void searchUsers(HttpServletRequest request, HttpServletResponse response, Long adminId)
            throws ServletException, IOException {
        String query = request.getParameter("query");
        if (query != null && !query.trim().isEmpty()) {
            List<User> users = userDAO.search(query);
            request.setAttribute("users", users);
            request.setAttribute("searchQuery", query);
        } else {
            List<User> users = userDAO.findAll();
            request.setAttribute("users", users);
        }
        request.getRequestDispatcher("/WEB-INF/views/admin-users.jsp").forward(request, response);
    }

    private Long parseLong(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
