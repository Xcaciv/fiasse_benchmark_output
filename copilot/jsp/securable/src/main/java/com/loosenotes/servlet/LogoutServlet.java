package com.loosenotes.servlet;

import com.loosenotes.model.User;
import com.loosenotes.util.AuditLogger;
import com.loosenotes.util.CsrfUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Handles logout. POST-only to prevent CSRF-via-GET logout attacks.
 * Invalidates the session and redirects to login.
 */
@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            User currentUser = (User) session.getAttribute("currentUser");
            // Validate CSRF token before processing logout
            String csrfToken = req.getParameter("csrfToken");
            if (!CsrfUtil.validateToken(session, csrfToken)) {
                res.sendRedirect(req.getContextPath() + "/login");
                return;
            }
            String username = currentUser != null ? currentUser.getUsername() : "-";
            AuditLogger.logAuthEvent("LOGOUT", username, req.getRemoteAddr(), "");
            session.invalidate();
        }
        res.sendRedirect(req.getContextPath() + "/login");
    }

    /** Redirect GETs to login rather than exposing a logout page. */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.sendRedirect(req.getContextPath() + "/login");
    }
}
