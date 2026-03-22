package com.loosenotes.servlet;

import com.loosenotes.audit.AuditLogger;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;

/** Handles logout: invalidates session and redirects to login. */
public final class LogoutServlet extends HttpServlet {

    private AuditLogger auditLogger;

    @Override
    public void init() {
        this.auditLogger = (AuditLogger) getServletContext().getAttribute("auditLogger");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            Long userId   = (Long)   session.getAttribute("userId");
            String name   = (String) session.getAttribute("username");
            auditLogger.log(userId, name, "LOGOUT", "USER",
                    userId != null ? String.valueOf(userId) : null,
                    req.getRemoteAddr(), "SUCCESS", null);
            session.invalidate();
        }
        resp.sendRedirect(req.getContextPath() + "/login");
    }

    /** Support GET logout for convenience links. */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doPost(req, resp);
    }
}
