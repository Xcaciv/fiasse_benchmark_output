package com.loosenotes.servlet;

import com.loosenotes.dao.AuditLogDAO;
import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AdminServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AdminServlet.class);
    private final UserDAO userDAO = new UserDAO();
    private final NoteDAO noteDAO = new NoteDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/") || pathInfo.isEmpty()) {
            handleDashboard(req, resp);
        } else {
            switch (pathInfo) {
                case "/users" -> handleUsers(req, resp);
                case "/reassign" -> handleReassignGet(req, resp);
                default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if ("/reassign".equals(pathInfo)) {
            handleReassignPost(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handleDashboard(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        int totalUsers = userDAO.getTotalCount();
        int totalNotes = noteDAO.getTotalCount();
        List<Map<String, Object>> recentActivity = auditLogDAO.getRecent(20);

        req.setAttribute("totalUsers", totalUsers);
        req.setAttribute("totalNotes", totalNotes);
        req.setAttribute("recentActivity", recentActivity);
        req.getRequestDispatcher("/WEB-INF/views/admin/index.jsp").forward(req, resp);
    }

    private void handleUsers(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String searchQuery = req.getParameter("q");
        List<User> users;

        if (searchQuery != null && !searchQuery.isBlank()) {
            users = userDAO.search(searchQuery.trim());
            req.setAttribute("searchQuery", searchQuery);
        } else {
            users = userDAO.findAll();
        }

        req.setAttribute("users", users);
        req.getRequestDispatcher("/WEB-INF/views/admin/users.jsp").forward(req, resp);
    }

    private void handleReassignGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String noteIdStr = req.getParameter("noteId");
        if (noteIdStr == null) {
            resp.sendRedirect(req.getContextPath() + "/admin/users");
            return;
        }

        try {
            int noteId = Integer.parseInt(noteIdStr);
            Note note = noteDAO.findById(noteId);
            if (note == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            List<User> users = userDAO.findAll();
            req.setAttribute("note", note);
            req.setAttribute("users", users);
            req.getRequestDispatcher("/WEB-INF/views/admin/reassignNote.jsp").forward(req, resp);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void handleReassignPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User admin = (User) req.getSession().getAttribute("currentUser");
        String noteIdStr = req.getParameter("noteId");
        String newUserIdStr = req.getParameter("newUserId");

        if (noteIdStr == null || newUserIdStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            int noteId = Integer.parseInt(noteIdStr);
            int newUserId = Integer.parseInt(newUserIdStr);

            Note note = noteDAO.findById(noteId);
            User newOwner = userDAO.findById(newUserId);

            if (note == null || newOwner == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            String oldOwner = note.getOwnerUsername();
            if (noteDAO.reassignOwner(noteId, newUserId)) {
                auditLogDAO.log(admin.getId(), "NOTE_REASSIGN",
                    "Note " + noteId + " reassigned from " + oldOwner + " to " + newOwner.getUsername());
                logger.info("Note {} reassigned from {} to {} by admin {}",
                    noteId, oldOwner, newOwner.getUsername(), admin.getUsername());
                resp.sendRedirect(req.getContextPath() + "/admin/users?success=reassigned");
            } else {
                req.setAttribute("error", "Failed to reassign note.");
                Note freshNote = noteDAO.findById(noteId);
                List<User> users = userDAO.findAll();
                req.setAttribute("note", freshNote);
                req.setAttribute("users", users);
                req.getRequestDispatcher("/WEB-INF/views/admin/reassignNote.jsp").forward(req, resp);
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
