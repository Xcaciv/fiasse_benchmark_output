package com.loosenotes.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.UUID;

/**
 * Handles session termination at {@code /auth/logout}.
 *
 * <p>Only POST is honoured for logout so that cross-origin GET requests
 * (e.g. {@code <img src="/auth/logout">}) cannot log a user out.  GET
 * requests are redirected to the home page without any session mutation.
 *
 * <p>No CSRF check is required here: the worst a CSRF attack can achieve
 * against a logout endpoint is logging the user out, which is not a
 * security-sensitive operation.
 */
@WebServlet("/auth/logout")
public class LogoutServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(LogoutServlet.class);
    private static final long serialVersionUID = 1L;

    // -------------------------------------------------------------------------
    // GET /auth/logout — redirect to home (no session change)
    // -------------------------------------------------------------------------

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        MDC.put("correlationId", UUID.randomUUID().toString());
        try {
            response.sendRedirect(request.getContextPath() + "/");
        } finally {
            MDC.remove("correlationId");
        }
    }

    // -------------------------------------------------------------------------
    // POST /auth/logout — invalidate session and redirect to login
    // -------------------------------------------------------------------------

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        MDC.put("correlationId", UUID.randomUUID().toString());
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Long userId = (Long) session.getAttribute("userId");
                log.info("User logged out. userId={} ip={}", userId, request.getRemoteAddr());
                // Invalidate the session — no attributes survive across this call.
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/auth/login");
        } finally {
            MDC.remove("correlationId");
        }
    }
}
