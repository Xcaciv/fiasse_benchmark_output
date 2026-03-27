package com.loosenotes.servlet;

import com.loosenotes.model.User;
import com.loosenotes.service.UserService;
import com.loosenotes.service.UserService.AuthResult;
import com.loosenotes.util.InputSanitizer;
import com.loosenotes.util.RateLimiter;
import com.loosenotes.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Handles user authentication: login, logout, and registration.
 * Trust boundary: validates all inputs before service calls.
 * SSEM: Authenticity - session invalidation on logout, ID rotation on login.
 * SSEM: Accountability - all auth events delegated to AuditService.
 * SSEM: Availability - rate limiting on login endpoint.
 */
@WebServlet("/auth/*")
public class AuthServlet extends BaseServlet {

    private static final Logger log = LoggerFactory.getLogger(AuthServlet.class);

    private static final String LOGIN_JSP    = "/WEB-INF/jsp/account/login.jsp";
    private static final String REGISTER_JSP = "/WEB-INF/jsp/account/register.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String action = getAction(req);
        switch (action) {
            case "login"    -> forward(req, res, LOGIN_JSP);
            case "register" -> forward(req, res, REGISTER_JSP);
            case "logout"   -> handleLogout(req, res);
            default         -> sendNotFound(res);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String action = getAction(req);
        switch (action) {
            case "login"    -> handleLogin(req, res);
            case "register" -> handleRegister(req, res);
            default         -> sendNotFound(res);
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String ip = getClientIp(req);
        String username = InputSanitizer.sanitizeSingleLine(req.getParameter("username"));
        String password = req.getParameter("password"); // Not sanitized - passed to BCrypt

        if (!checkLoginRateLimit(ip, username, req, res)) return;

        try {
            AuthResult result = getUserService().authenticate(username, password);
            if (result == AuthResult.SUCCESS) {
                completeLogin(req, res, username, ip);
            } else {
                handleFailedLogin(req, res, username, ip, result);
            }
        } catch (SQLException e) {
            log.error("Database error during login", e);
            forwardWithError(req, res, LOGIN_JSP, "A system error occurred. Please try again.");
        }
    }

    private boolean checkLoginRateLimit(String ip, String username,
                                         HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        RateLimiter limiter = getLoginRateLimiter();
        String key = ip + ":" + username;
        if (!limiter.tryAcquire(key)) {
            getAuditService().logLoginFailure(username, ip);
            forwardWithError(req, res, LOGIN_JSP,
                "Too many login attempts. Please wait before trying again.");
            return false;
        }
        return true;
    }

    private void completeLogin(HttpServletRequest req, HttpServletResponse res,
                                String username, String ip)
            throws IOException, ServletException {
        try {
            Optional<User> user = getUserService().findByUsername(username);
            if (user.isEmpty()) {
                forwardWithError(req, res, LOGIN_JSP, "Authentication failed.");
                return;
            }
            // SSEM: Authenticity - invalidate old session, create new one (session fixation prevention)
            HttpSession oldSession = req.getSession(false);
            if (oldSession != null) oldSession.invalidate();
            HttpSession newSession = req.getSession(true);
            newSession.setAttribute("user", user.get());
            newSession.setMaxInactiveInterval(30 * 60); // 30 minutes

            getLoginRateLimiter().reset(ip + ":" + username);
            getAuditService().logLoginSuccess(user.get().getId(), username, ip);
            redirect(res, req, "/notes");
        } catch (SQLException e) {
            log.error("Error loading user after login", e);
            forwardWithError(req, res, LOGIN_JSP, "A system error occurred.");
        }
    }

    private void handleFailedLogin(HttpServletRequest req, HttpServletResponse res,
                                    String username, String ip, AuthResult result)
            throws ServletException, IOException {
        getAuditService().logLoginFailure(username, ip);
        String msg = switch (result) {
            case ACCOUNT_LOCKED -> "Account is temporarily locked due to too many failed attempts.";
            default -> "Invalid username or password."; // Deliberate ambiguity
        };
        forwardWithError(req, res, LOGIN_JSP, msg);
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String username = InputSanitizer.sanitizeSingleLine(req.getParameter("username"));
        String email    = InputSanitizer.canonicalizeEmail(req.getParameter("email"));
        String password = req.getParameter("password");
        String confirm  = req.getParameter("confirmPassword");

        if (!password.equals(confirm)) {
            forwardWithError(req, res, REGISTER_JSP, "Passwords do not match.");
            return;
        }

        try {
            Optional<User> created = getUserService().register(username, email, password);
            if (created.isEmpty()) {
                forwardWithError(req, res, REGISTER_JSP,
                    "Username or email is already in use.");
                return;
            }
            getAuditService().logRegistration(created.get().getId(), username, getClientIp(req));
            redirect(res, req, "/auth/login?registered=true");
        } catch (IllegalArgumentException e) {
            forwardWithError(req, res, REGISTER_JSP, e.getMessage());
        } catch (SQLException e) {
            log.error("Database error during registration", e);
            forwardWithError(req, res, REGISTER_JSP, "A system error occurred.");
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            User user = (User) session.getAttribute("user");
            if (user != null) {
                getAuditService().logLogout(user.getId(), user.getUsername(), getClientIp(req));
            }
            session.invalidate();
        }
        redirect(res, req, "/auth/login");
    }

    private String getAction(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) return "";
        return pathInfo.substring(1).split("/")[0];
    }
}
