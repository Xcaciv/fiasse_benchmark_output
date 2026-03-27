package com.loosenotes.servlet;

import com.loosenotes.model.AuditLog;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;
import com.loosenotes.util.InputSanitizer;
import com.loosenotes.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Admin dashboard and user management.
 * SSEM: Authenticity - admin role required on every request.
 * SSEM: Accountability - all admin actions logged.
 */
@WebServlet("/admin/*")
public class AdminServlet extends BaseServlet {

    private static final Logger log = LoggerFactory.getLogger(AdminServlet.class);

    private static final String DASHBOARD_JSP = "/WEB-INF/jsp/admin/dashboard.jsp";
    private static final String USERS_JSP     = "/WEB-INF/jsp/admin/users.jsp";
    private static final String REASSIGN_JSP  = "/WEB-INF/jsp/admin/reassignNote.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        User user = getCurrentUser(req);
        if (requireAdmin(user, res)) return;

        String action = getAction(req);
        switch (action) {
            case ""        -> showDashboard(req, res);
            case "users"   -> showUsers(req, res);
            case "reassign"-> showReassign(req, res);
            default        -> sendNotFound(res);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        User user = getCurrentUser(req);
        if (requireAdmin(user, res)) return;

        String action = getAction(req);
        if ("reassign".equals(action)) {
            handleReassign(req, res, user);
        } else {
            sendNotFound(res);
        }
    }

    private void showDashboard(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        try {
            long userCount = getUserService().findById(1L).isPresent() ? countUsers() : 0;
            List<AuditLog> recent = getAuditService().getRecentActivity(20);
            req.setAttribute("userCount", userCount);
            req.setAttribute("recentActivity", recent);
            forward(req, res, DASHBOARD_JSP);
        } catch (SQLException e) {
            log.error("Error loading admin dashboard", e);
            forwardWithError(req, res, DASHBOARD_JSP, "Could not load dashboard data.");
        }
    }

    private void showUsers(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String searchQuery = InputSanitizer.sanitizeSingleLine(req.getParameter("q"));
        int page = ValidationUtil.parseIntSafe(req.getParameter("page"), 1);
        try {
            List<User> users = loadUsers(searchQuery, page);
            req.setAttribute("users", users);
            req.setAttribute("searchQuery", searchQuery);
            req.setAttribute("page", page);
            forward(req, res, USERS_JSP);
        } catch (SQLException e) {
            log.error("Error loading users list", e);
            forwardWithError(req, res, USERS_JSP, "Could not load users.");
        }
    }

    private void showReassign(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        long noteId = ValidationUtil.parseLongSafe(req.getParameter("noteId"));
        if (!ValidationUtil.isValidId(noteId)) { sendNotFound(res); return; }
        try {
            Optional<Note> note = getNoteService().getNoteForShare(noteId);
            if (note.isEmpty()) { sendNotFound(res); return; }
            req.setAttribute("note", note.get());
            forward(req, res, REASSIGN_JSP);
        } catch (SQLException e) {
            log.error("Error loading note for reassign id={}", noteId, e);
            sendNotFound(res);
        }
    }

    private void handleReassign(HttpServletRequest req, HttpServletResponse res,
                                  User admin) throws ServletException, IOException {
        long noteId   = ValidationUtil.parseLongSafe(req.getParameter("noteId"));
        long newOwner = ValidationUtil.parseLongSafe(req.getParameter("newOwnerId"));

        if (!ValidationUtil.isValidId(noteId) || !ValidationUtil.isValidId(newOwner)) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameters");
            return;
        }
        try {
            getNoteService().reassignNote(noteId, newOwner);
            getAuditService().logNoteReassigned(admin.getId(), noteId, newOwner, getClientIp(req));
            redirect(res, req, "/admin/users");
        } catch (SQLException e) {
            log.error("Error reassigning note id={}", noteId, e);
            forwardWithError(req, res, REASSIGN_JSP, "Could not reassign note.");
        }
    }

    private List<User> loadUsers(String query, int page) throws SQLException {
        int pageSize = 25;
        if (query != null && !query.isBlank()) {
            return getUserService().searchUsers(query, pageSize);
        }
        return getUserService().listUsers(page, pageSize);
    }

    private long countUsers() throws SQLException {
        return getUserService().countUsers();
    }

    private String getAction(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) return "";
        return pathInfo.substring(1).split("/")[0];
    }
}
