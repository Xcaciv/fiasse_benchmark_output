package com.loosenotes.servlet;

import com.loosenotes.dao.AuditLogDao;
import com.loosenotes.model.Note;
import com.loosenotes.model.User;
import com.loosenotes.service.NoteService;
import com.loosenotes.service.ServiceException;
import com.loosenotes.service.UserService;
import com.loosenotes.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Admin dashboard and management (REQ-013, REQ-016).
 * GET  /admin          → dashboard
 * GET  /admin/users    → user list (with optional search)
 * POST /admin/notes/reassign → reassign note ownership
 */
public final class AdminServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(AdminServlet.class);

    private UserService  userService;
    private NoteService  noteService;
    private AuditLogDao  auditLogDao;

    @Override
    public void init() {
        this.userService = (UserService)  getServletContext().getAttribute("userService");
        this.noteService = (NoteService)  getServletContext().getAttribute("noteService");
        this.auditLogDao = (AuditLogDao)  getServletContext().getAttribute("auditLogDao");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            showDashboard(req, resp);
        } else if ("/users".equals(pathInfo)) {
            showUsers(req, resp);
        } else if ("/notes".equals(pathInfo)) {
            showNotes(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if ("/notes/reassign".equals(pathInfo)) {
            reassignNote(req, resp);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void showDashboard(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setAttribute("userCount",     userService.countAll());
        req.setAttribute("noteCount",     noteService.countAll());
        req.setAttribute("recentActivity", auditLogDao.findRecent(20));
        req.getRequestDispatcher("/WEB-INF/jsp/admin/dashboard.jsp").forward(req, resp);
    }

    private void showUsers(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String query = req.getParameter("q");
        List<User> users;
        if (query != null && !query.trim().isEmpty()) {
            users = userService.searchUsers(query.trim());
            req.setAttribute("query", query.trim());
        } else {
            users = userService.findAll();
        }
        req.setAttribute("users", users);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/dashboard.jsp").forward(req, resp);
    }

    private void showNotes(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long noteId = parseLong(req.getParameter("noteId"), -1);
        if (noteId > 0) {
            noteService.findById(noteId).ifPresent(n -> req.setAttribute("note", n));
            req.setAttribute("allUsers", userService.findAll());
        }
        req.getRequestDispatcher("/WEB-INF/jsp/admin/dashboard.jsp").forward(req, resp);
    }

    private void reassignNote(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        long noteId     = parseLong(req.getParameter("noteId"),    -1);
        long newOwnerId = parseLong(req.getParameter("newOwnerId"), -1);
        long adminId    = (Long) req.getSession().getAttribute("userId");

        if (noteId < 0 || newOwnerId < 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid parameters");
            return;
        }
        try {
            noteService.reassignNote(noteId, newOwnerId, adminId);
            resp.sendRedirect(req.getContextPath() + "/admin?reassigned=1");
        } catch (ServiceException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private long parseLong(String s, long defaultVal) {
        if (s == null) return defaultVal;
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) { return defaultVal; }
    }
}
