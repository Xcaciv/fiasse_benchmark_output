package com.loosenotes.servlet;

import com.loosenotes.dao.NoteDAO;
import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.User;
import com.loosenotes.util.CsrfUtil;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REQ-013, REQ-016 – Admin dashboard.
 *
 * GET  /admin/dashboard  – view stats + user list
 * POST /admin/reassign   – reassign note ownership
 * POST /admin/searchusers (via GET param)
 *
 * SSEM: Authorization — ADMIN role enforced at every entry point.
 * SSEM: Logging — all admin actions logged with actor identity.
 */
public class AdminServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AdminServlet.class.getName());
    private final UserDAO userDAO = new UserDAO();
    private final NoteDAO noteDAO = new NoteDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        if (!isAdmin(req)) { res.sendError(HttpServletResponse.SC_FORBIDDEN); return; }

        String path = req.getPathInfo();
        CsrfUtil.getOrCreateToken(req);
        req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));

        try {
            req.setAttribute("totalUsers", userDAO.countAll());
            req.setAttribute("totalNotes", noteDAO.countAll());
            req.setAttribute("recentNotes", noteDAO.findRecent(10));

            String query = req.getParameter("userSearch");
            if (query != null && !query.isBlank()) {
                req.setAttribute("users", userDAO.search(ValidationUtil.truncate(query, 100)));
                req.setAttribute("userSearch", query);
            } else {
                req.setAttribute("users", userDAO.findAll());
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Admin dashboard load error", e);
        }

        req.getRequestDispatcher("/jsp/admin/dashboard.jsp").forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        if (!isAdmin(req)) { res.sendError(HttpServletResponse.SC_FORBIDDEN); return; }

        long adminId = (long) req.getSession().getAttribute("userId");
        String path  = req.getPathInfo();

        if ("/reassign".equals(path)) {
            long noteId   = ValidationUtil.parseLong(req.getParameter("noteId"));
            long newOwner = ValidationUtil.parseLong(req.getParameter("newOwnerId"));

            if (noteId < 0 || newOwner < 0) {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            try {
                User targetUser = userDAO.findById(newOwner);
                if (targetUser == null) {
                    res.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                noteDAO.reassignOwner(noteId, newOwner);
                LOGGER.info("Admin " + adminId + " reassigned note " + noteId
                        + " to user " + newOwner);
                res.sendRedirect(req.getContextPath() + "/admin/dashboard?msg=reassigned");

            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Admin reassign error", e);
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private boolean isAdmin(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) return false;
        return "ADMIN".equalsIgnoreCase((String) session.getAttribute("role"));
    }
}
