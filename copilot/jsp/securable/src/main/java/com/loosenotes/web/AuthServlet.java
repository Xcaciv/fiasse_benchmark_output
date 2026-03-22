package com.loosenotes.web;

import com.loosenotes.dao.ActivityLogDao;
import com.loosenotes.dao.PasswordResetDao;
import com.loosenotes.dao.UserDao;
import com.loosenotes.model.User;
import com.loosenotes.security.SecurityUtil;
import com.loosenotes.service.EmailService;
import com.loosenotes.util.AppUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.OptionalLong;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(name = "AuthServlet", urlPatterns = {"/auth/*"})
public class AuthServlet extends BaseServlet {
    private final UserDao userDao = new UserDao();
    private final PasswordResetDao passwordResetDao = new PasswordResetDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = path(request);
        try {
            switch (path) {
                case "/login":
                    render(request, response, "auth/login.jsp", "Login");
                    break;
                case "/register":
                    render(request, response, "auth/register.jsp", "Register");
                    break;
                case "/forgot-password":
                    render(request, response, "auth/forgot-password.jsp", "Forgot password");
                    break;
                case "/reset-password":
                    String token = AppUtil.trimToEmpty(request.getParameter("token"));
                    request.setAttribute("tokenValid", !token.isBlank() && passwordResetDao.tokenIsUsable(SecurityUtil.sha256Base64(token)));
                    request.setAttribute("token", token);
                    render(request, response, "auth/reset-password.jsp", "Reset password");
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = path(request);
        try {
            switch (path) {
                case "/register":
                    handleRegister(request, response);
                    break;
                case "/login":
                    handleLogin(request, response);
                    break;
                case "/logout":
                    handleLogout(request, response);
                    break;
                case "/forgot-password":
                    handleForgotPassword(request, response);
                    break;
                case "/reset-password":
                    handleResetPassword(request, response);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException ex) {
            throw new ServletException(ex);
        }
    }

    private void handleRegister(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, SQLException {
        if (!requireCsrf(request, response)) {
            return;
        }
        String username = AppUtil.trimToEmpty(request.getParameter("username"));
        String email = AppUtil.trimToEmpty(request.getParameter("email"));
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        List<String> errors = AppUtil.validateRegistration(username, email, password, confirmPassword);
        if (userDao.usernameExists(username, null)) {
            errors.add("That username is already in use.");
        }
        if (userDao.emailExists(email, null)) {
            errors.add("That email address is already registered.");
        }
        if (!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            request.setAttribute("formUsername", username);
            request.setAttribute("formEmail", email);
            render(request, response, "auth/register.jsp", "Register");
            return;
        }

        SecurityUtil.PasswordHash hash = SecurityUtil.hashPassword(password);
        long userId = userDao.createUser(username, email, hash.getHashBase64(), hash.getSaltBase64(), "USER");
        activityLogDao.log(userId, "auth.register", "User account registered.");
        setFlash(request, "success", "Registration complete. Please log in.");
        response.sendRedirect(request.getContextPath() + "/auth/login");
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, SQLException {
        if (!requireCsrf(request, response)) {
            return;
        }
        String username = AppUtil.trimToEmpty(request.getParameter("username"));
        String password = request.getParameter("password");
        User user = userDao.findByUsername(username).orElse(null);
        if (user == null || !SecurityUtil.verifyPassword(password == null ? "" : password, user.getPasswordSalt(), user.getPasswordHash())) {
            activityLogDao.log(user == null ? null : user.getId(), "auth.login_failed", "Failed login attempt for username: " + username);
            request.setAttribute("errors", List.of("Invalid username or password."));
            request.setAttribute("formUsername", username);
            render(request, response, "auth/login.jsp", "Login");
            return;
        }
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.getId());
        session.setMaxInactiveInterval(30 * 60);
        SecurityUtil.csrfToken(session);
        activityLogDao.log(user.getId(), "auth.login", "Successful login.");
        setFlash(request, "success", "Welcome back, " + user.getUsername() + ".");
        response.sendRedirect(request.getContextPath() + "/notes");
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response) throws IOException, SQLException {
        if (!requireCsrf(request, response)) {
            return;
        }
        User user = currentUser(request);
        HttpSession session = request.getSession(false);
        if (user != null) {
            activityLogDao.log(user.getId(), "auth.logout", "User logged out.");
        }
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(request.getContextPath() + "/");
    }

    private void handleForgotPassword(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, SQLException {
        if (!requireCsrf(request, response)) {
            return;
        }
        String email = AppUtil.trimToEmpty(request.getParameter("email"));
        List<String> errors = new java.util.ArrayList<>();
        AppUtil.validateEmail(email, errors);
        if (!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            request.setAttribute("formEmail", email);
            render(request, response, "auth/forgot-password.jsp", "Forgot password");
            return;
        }
        User user = userDao.findByEmail(email).orElse(null);
        if (user != null) {
            String rawToken = SecurityUtil.newToken();
            passwordResetDao.createToken(user.getId(), SecurityUtil.sha256Base64(rawToken));
            EmailService.writePasswordResetEmail(user.getEmail(), user.getUsername(), appUrl(request, "/auth/reset-password?token=" + rawToken));
            activityLogDao.log(user.getId(), "auth.password_reset_requested", "Password reset requested.");
        }
        setFlash(request, "success", "If an account exists for that address, a reset email has been written to the local outbox.");
        response.sendRedirect(request.getContextPath() + "/auth/forgot-password");
    }

    private void handleResetPassword(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, SQLException {
        if (!requireCsrf(request, response)) {
            return;
        }
        String token = AppUtil.trimToEmpty(request.getParameter("token"));
        String newPassword = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        List<String> errors = new java.util.ArrayList<>();
        if (token.isBlank() || !passwordResetDao.tokenIsUsable(SecurityUtil.sha256Base64(token))) {
            errors.add("The reset link is invalid or has expired.");
        }
        AppUtil.validatePassword(newPassword, errors);
        if (!(newPassword == null ? "" : newPassword).equals(confirmPassword == null ? "" : confirmPassword)) {
            errors.add("Password confirmation does not match.");
        }
        if (!errors.isEmpty()) {
            request.setAttribute("errors", errors);
            request.setAttribute("tokenValid", false);
            request.setAttribute("token", token);
            render(request, response, "auth/reset-password.jsp", "Reset password");
            return;
        }
        OptionalLong userId = passwordResetDao.consumeToken(SecurityUtil.sha256Base64(token));
        if (userId.isEmpty()) {
            request.setAttribute("errors", List.of("The reset link is invalid or has already been used."));
            request.setAttribute("tokenValid", false);
            request.setAttribute("token", token);
            render(request, response, "auth/reset-password.jsp", "Reset password");
            return;
        }
        SecurityUtil.PasswordHash hash = SecurityUtil.hashPassword(newPassword);
        userDao.updatePassword(userId.getAsLong(), hash.getHashBase64(), hash.getSaltBase64());
        activityLogDao.log(userId.getAsLong(), "auth.password_reset_completed", "Password reset completed.");
        setFlash(request, "success", "Your password has been reset. Please log in.");
        response.sendRedirect(request.getContextPath() + "/auth/login");
    }

    private String path(HttpServletRequest request) {
        String path = request.getPathInfo();
        return path == null || path.isBlank() ? "/login" : path;
    }
}
