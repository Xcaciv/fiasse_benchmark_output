package com.loosenotes.servlet;

import com.loosenotes.model.User;
import com.loosenotes.service.ServiceException;
import com.loosenotes.service.UserService;
import com.loosenotes.util.InputSanitizer;
import com.loosenotes.util.RateLimiter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Handles user authentication: login, logout, and registration.
 * URL patterns (dispatched by path info):
 *   GET  /auth/login     – show login form
 *   POST /auth/login     – process credentials
 *   GET  /auth/logout    – invalidate session
 *   GET  /auth/register  – show registration form
 *   POST /auth/register  – create new account
 *
 * SSEM / ASVS alignment:
 * - ASVS V6.1 (Passwords): BCrypt via UserService.
 * - ASVS V8.1 (Brute Force): rate limiting in UserService.
 * - Authenticity: session invalidated before establishing a new one (session fixation prevention).
 * - Accountability: login/logout events audited by UserService.
 */
public class AuthServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null) path = "/";

        switch (path) {
            case "/login"    -> forward(req, resp, "account/login.jsp");
            case "/logout"   -> handleLogout(req, resp);
            case "/register" -> forward(req, resp, "account/register.jsp");
            default          -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null) path = "/";

        switch (path) {
            case "/login"    -> handleLogin(req, resp);
            case "/register" -> handleRegister(req, resp);
            default          -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    // ── Login ────────────────────────────────────────────────────────────────

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Trust boundary: sanitize inputs before processing
        String username = InputSanitizer.sanitizeLine(req.getParameter("username"));
        String rawPwd   = req.getParameter("password");

        if (username == null || rawPwd == null || rawPwd.isEmpty()) {
            req.setAttribute("error", "Username and password are required");
            forward(req, resp, "account/login.jsp");
            return;
        }

        char[] password = rawPwd.toCharArray();
        try {
            User user = getUserService().authenticate(username, password, getClientIp(req));

            // Prevent session fixation: invalidate old session, create new one
            HttpSession old = req.getSession(false);
            if (old != null) old.invalidate();
            HttpSession newSession = req.getSession(true);
            newSession.setAttribute("currentUser", user);

            // Redirect to originally requested URL if present
            String redirect = InputSanitizer.sanitizeLine(req.getParameter("redirect"));
            if (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//")) {
                resp.sendRedirect(req.getContextPath() + redirect);
            } else {
                resp.sendRedirect(req.getContextPath() + "/notes");
            }
        } catch (ServiceException e) {
            req.setAttribute("error", e.getMessage());
            forward(req, resp, "account/login.jsp");
        } catch (SQLException e) {
            log.error("Database error during login", e);
            req.setAttribute("error", "A system error occurred. Please try again.");
            forward(req, resp, "account/login.jsp");
        }
    }

    // ── Logout ───────────────────────────────────────────────────────────────

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        User user = getCurrentUser(req);
        if (user != null) {
            getAuditService().record(user.getId(),
                    com.loosenotes.model.AuditLog.EventType.AUTH,
                    "logout username=" + user.getUsername(), getClientIp(req));
        }
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        resp.sendRedirect(req.getContextPath() + "/auth/login");
    }

    // ── Register ─────────────────────────────────────────────────────────────

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Trust boundary: sanitize all inputs
        String username  = InputSanitizer.sanitizeLine(req.getParameter("username"));
        String email     = InputSanitizer.sanitizeLine(req.getParameter("email"));
        String rawPwd    = req.getParameter("password");
        String rawPwd2   = req.getParameter("confirmPassword");

        if (username == null || email == null || rawPwd == null) {
            req.setAttribute("error", "All fields are required");
            forward(req, resp, "account/register.jsp");
            return;
        }
        if (!rawPwd.equals(rawPwd2)) {
            req.setAttribute("error", "Passwords do not match");
            forward(req, resp, "account/register.jsp");
            return;
        }

        char[] password = rawPwd.toCharArray();
        try {
            long newUserId = getUserService().register(username, email, password, getClientIp(req));
            // Auto-login after registration
            User user = getUserService().findById(newUserId);
            HttpSession session = req.getSession(true);
            session.setAttribute("currentUser", user);
            resp.sendRedirect(req.getContextPath() + "/notes");
        } catch (ServiceException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("username", username);
            req.setAttribute("email", email);
            forward(req, resp, "account/register.jsp");
        } catch (SQLException e) {
            log.error("Database error during registration", e);
            req.setAttribute("error", "A system error occurred. Please try again.");
            forward(req, resp, "account/register.jsp");
        }
    }
}
