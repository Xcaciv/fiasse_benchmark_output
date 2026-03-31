package com.loosenotes.servlet;

import com.loosenotes.model.AuditLog;
import com.loosenotes.model.User;
import com.loosenotes.service.ServiceException;
import com.loosenotes.util.InputSanitizer;
import com.loosenotes.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Admin dashboard: user management and note reassignment.
 * All routes require ADMIN role (enforced by AuthenticationFilter).
 *
 * SSEM notes:
 * - Accountability: all admin actions logged by services.
 * - Authenticity: admin role verified in AuthenticationFilter, confirmed here too.
 */
public class AdminServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User admin = getRequiredUser(req, resp);
        if (admin == null) return;
        if (!admin.isAdmin()) { resp.sendError(403); return; }

        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();

        switch (path) {
            case "/", "/dashboard" -> showDashboard(req, resp);
            case "/users"          -> showUsers(req, resp);
            default -> {
                if (path.startsWith("/reassign/")) {
                    showReassignForm(req, resp);
                } else {
                    resp.sendError(404);
                }
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User admin = getRequiredUser(req, resp);
        if (admin == null) return;
        if (!admin.isAdmin()) { resp.sendError(403); return; }

        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        if (path.startsWith("/reassign/")) {
            processReassign(req, resp, admin);
        } else {
            resp.sendError(404);
        }
    }

    private void showDashboard(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            int userCount  = getUserService().countAll();
            int noteCount  = getNoteService().countAll();
            List<AuditLog> activity = getAuditService().getRecentActivity(20);
            req.setAttribute("userCount",  userCount);
            req.setAttribute("noteCount",  noteCount);
            req.setAttribute("activity",   activity);
            forward(req, resp, "admin/dashboard.jsp");
        } catch (SQLException e) {
            log.error("Error loading admin dashboard", e);
            sendError(req, resp, 500, "Could not load dashboard");
        }
    }

    private void showUsers(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String q = InputSanitizer.sanitizeLine(req.getParameter("q"));
        try {
            List<User> users = (q != null && !q.isBlank())
                    ? getUserService().search(q)
                    : getUserService().findAll();
            req.setAttribute("users", users);
            req.setAttribute("query", q != null ? q : "");
            forward(req, resp, "admin/users.jsp");
        } catch (SQLException e) {
            log.error("Error loading user list", e);
            sendError(req, resp, 500, "Could not load users");
        }
    }

    private void showReassignForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long noteId = parseNoteIdFromAdminPath(req);
        if (noteId < 0) { resp.sendError(404); return; }
        try {
            req.setAttribute("noteId", noteId);
            req.setAttribute("users", getUserService().findAll());
            forward(req, resp, "admin/reassignNote.jsp");
        } catch (SQLException e) {
            log.error("Error loading reassign form", e);
            sendError(req, resp, 500, "Could not load form");
        }
    }

    private void processReassign(HttpServletRequest req, HttpServletResponse resp, User admin)
            throws IOException {
        long noteId = parseNoteIdFromAdminPath(req);
        if (noteId < 0) { resp.sendError(404); return; }

        String newOwnerParam = InputSanitizer.sanitizeLine(req.getParameter("newOwnerId"));
        long newOwnerId = ValidationUtil.parseLongId(newOwnerParam);
        if (newOwnerId < 0) { resp.sendError(400, "Invalid owner ID"); return; }

        try {
            getNoteService().reassignNote(noteId, newOwnerId, admin.getId(), getClientIp(req));
            resp.sendRedirect(req.getContextPath() + "/admin/users");
        } catch (ServiceException e) {
            sendError(req, resp, 400, e.getMessage());
        } catch (SQLException e) {
            log.error("Error reassigning note {}", noteId, e);
            sendError(req, resp, 500, "Could not reassign note");
        }
    }

    private long parseNoteIdFromAdminPath(HttpServletRequest req) {
        // /admin/reassign/{noteId}
        String path = req.getPathInfo();
        if (path == null) return -1;
        String[] parts = path.split("/");
        if (parts.length < 3) return -1;
        return ValidationUtil.parseLongId(parts[2]);
    }
}
