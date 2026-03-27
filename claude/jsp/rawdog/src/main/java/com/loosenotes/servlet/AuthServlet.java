package com.loosenotes.servlet;

import com.loosenotes.dao.AuditLogDAO;
import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.User;
import com.loosenotes.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

public class AuthServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(AuthServlet.class);
    private final UserDAO userDAO = new UserDAO();
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        switch (path) {
            case "/login" -> req.getRequestDispatcher("/WEB-INF/views/account/login.jsp").forward(req, resp);
            case "/register" -> req.getRequestDispatcher("/WEB-INF/views/account/register.jsp").forward(req, resp);
            case "/forgot-password" -> req.getRequestDispatcher("/WEB-INF/views/account/forgotPassword.jsp").forward(req, resp);
            case "/reset-password" -> handleResetPasswordGet(req, resp);
            case "/logout" -> handleLogout(req, resp);
            default -> resp.sendRedirect(req.getContextPath() + "/");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        switch (path) {
            case "/login" -> handleLogin(req, resp);
            case "/register" -> handleRegister(req, resp);
            case "/forgot-password" -> handleForgotPassword(req, resp);
            case "/reset-password" -> handleResetPasswordPost(req, resp);
            default -> resp.sendRedirect(req.getContextPath() + "/");
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String returnUrl = req.getParameter("returnUrl");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            req.setAttribute("error", "Username and password are required.");
            req.getRequestDispatcher("/WEB-INF/views/account/login.jsp").forward(req, resp);
            return;
        }

        User user = userDAO.findByUsername(username.trim());
        if (user == null || !PasswordUtil.verify(password, user.getPasswordHash())) {
            logger.warn("Failed login attempt for username: {}", username);
            auditLogDAO.log(null, "LOGIN_FAILED", "Username: " + username);
            req.setAttribute("error", "Invalid username or password.");
            req.getRequestDispatcher("/WEB-INF/views/account/login.jsp").forward(req, resp);
            return;
        }

        HttpSession session = req.getSession(true);
        session.setAttribute("currentUser", user);
        logger.info("User logged in: {}", username);
        auditLogDAO.log(user.getId(), "LOGIN", "User logged in");

        if (returnUrl != null && !returnUrl.isBlank() && returnUrl.startsWith("/")) {
            resp.sendRedirect(returnUrl);
        } else {
            resp.sendRedirect(req.getContextPath() + "/notes");
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");

        if (username == null || username.isBlank() || email == null || email.isBlank()
                || password == null || password.isBlank()) {
            req.setAttribute("error", "All fields are required.");
            req.getRequestDispatcher("/WEB-INF/views/account/register.jsp").forward(req, resp);
            return;
        }

        if (!password.equals(confirmPassword)) {
            req.setAttribute("error", "Passwords do not match.");
            req.getRequestDispatcher("/WEB-INF/views/account/register.jsp").forward(req, resp);
            return;
        }

        if (!PasswordUtil.meetsRequirements(password)) {
            req.setAttribute("error", "Password must be at least 6 characters.");
            req.getRequestDispatcher("/WEB-INF/views/account/register.jsp").forward(req, resp);
            return;
        }

        if (userDAO.findByUsername(username.trim()) != null) {
            req.setAttribute("error", "Username already taken.");
            req.getRequestDispatcher("/WEB-INF/views/account/register.jsp").forward(req, resp);
            return;
        }

        if (userDAO.findByEmail(email.trim()) != null) {
            req.setAttribute("error", "Email already registered.");
            req.getRequestDispatcher("/WEB-INF/views/account/register.jsp").forward(req, resp);
            return;
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(email.trim());
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setRole("USER");

        if (userDAO.create(user)) {
            auditLogDAO.log(user.getId(), "REGISTER", "New user registered: " + username);
            HttpSession session = req.getSession(true);
            session.setAttribute("currentUser", user);
            resp.sendRedirect(req.getContextPath() + "/notes");
        } else {
            req.setAttribute("error", "Registration failed. Please try again.");
            req.getRequestDispatcher("/WEB-INF/views/account/register.jsp").forward(req, resp);
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            User user = (User) session.getAttribute("currentUser");
            if (user != null) {
                auditLogDAO.log(user.getId(), "LOGOUT", "User logged out");
                logger.info("User logged out: {}", user.getUsername());
            }
            session.invalidate();
        }
        resp.sendRedirect(req.getContextPath() + "/login");
    }

    private void handleForgotPassword(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String email = req.getParameter("email");
        if (email == null || email.isBlank()) {
            req.setAttribute("error", "Email is required.");
            req.getRequestDispatcher("/WEB-INF/views/account/forgotPassword.jsp").forward(req, resp);
            return;
        }

        User user = userDAO.findByEmail(email.trim());
        // Always show success to prevent email enumeration
        String token = UUID.randomUUID().toString();
        if (user != null) {
            LocalDateTime expiry = LocalDateTime.now().plusHours(1);
            userDAO.setResetToken(user.getId(), token, expiry);
            // In production, send email. For development, log the token.
            logger.info("PASSWORD RESET TOKEN for {}: {} (expires: {})", email, token, expiry);
            System.out.println("=== PASSWORD RESET TOKEN for " + email + " ===");
            System.out.println("Token: " + token);
            System.out.println("Reset URL: /reset-password?token=" + token);
            System.out.println("Expires: " + expiry);
        }

        req.setAttribute("success", "If that email is registered, a reset link has been sent. Check server logs for the token.");
        req.getRequestDispatcher("/WEB-INF/views/account/forgotPassword.jsp").forward(req, resp);
    }

    private void handleResetPasswordGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String token = req.getParameter("token");
        if (token == null || token.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        User user = userDAO.findByResetToken(token);
        if (user == null || user.getResetTokenExpiry() == null
                || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            req.setAttribute("error", "Invalid or expired reset token.");
            req.getRequestDispatcher("/WEB-INF/views/account/resetPassword.jsp").forward(req, resp);
            return;
        }
        req.setAttribute("token", token);
        req.getRequestDispatcher("/WEB-INF/views/account/resetPassword.jsp").forward(req, resp);
    }

    private void handleResetPasswordPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String token = req.getParameter("token");
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");

        if (token == null || token.isBlank()) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        User user = userDAO.findByResetToken(token);
        if (user == null || user.getResetTokenExpiry() == null
                || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            req.setAttribute("error", "Invalid or expired reset token.");
            req.getRequestDispatcher("/WEB-INF/views/account/resetPassword.jsp").forward(req, resp);
            return;
        }

        if (password == null || !password.equals(confirmPassword)) {
            req.setAttribute("error", "Passwords do not match.");
            req.setAttribute("token", token);
            req.getRequestDispatcher("/WEB-INF/views/account/resetPassword.jsp").forward(req, resp);
            return;
        }

        if (!PasswordUtil.meetsRequirements(password)) {
            req.setAttribute("error", "Password must be at least 6 characters.");
            req.setAttribute("token", token);
            req.getRequestDispatcher("/WEB-INF/views/account/resetPassword.jsp").forward(req, resp);
            return;
        }

        user.setPasswordHash(PasswordUtil.hash(password));
        userDAO.update(user);
        userDAO.clearResetToken(user.getId());
        auditLogDAO.log(user.getId(), "PASSWORD_RESET", "Password reset completed");

        req.setAttribute("success", "Password reset successfully. You can now log in.");
        req.getRequestDispatcher("/WEB-INF/views/account/resetPasswordConfirmation.jsp").forward(req, resp);
    }
}
