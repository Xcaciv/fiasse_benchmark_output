package com.loosenotes.servlet;

import com.loosenotes.dao.DatabaseManager;
import com.loosenotes.dao.UserDao;
import com.loosenotes.model.User;
import com.loosenotes.util.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Handles user registration.
 * Trust boundary: all inputs canonicalized, sanitized, and validated before use.
 */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private UserDao userDao;

    @Override
    public void init() {
        this.userDao = new UserDao(DatabaseManager.getInstance());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("text/html;charset=UTF-8");
        req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("text/html;charset=UTF-8");
        req.setCharacterEncoding("UTF-8");

        // Trust boundary: sanitize all inputs before validation
        String username = ValidationUtil.sanitizeString(req.getParameter("username"));
        String email = ValidationUtil.sanitizeString(req.getParameter("email"));
        String password = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");

        String error = validateRegistrationInputs(username, email, password, confirmPassword);
        if (error != null) {
            req.setAttribute("error", error);
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, res);
            return;
        }

        try {
            if (userDao.findByUsername(username).isPresent()) {
                req.setAttribute("error", "Username is already taken.");
                req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, res);
                return;
            }
            if (userDao.findByEmail(email).isPresent()) {
                req.setAttribute("error", "Email is already registered.");
                req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, res);
                return;
            }
            User user = buildNewUser(username, email, password);
            userDao.save(user);
            AuditLogger.logAuthEvent("REGISTER", username, req.getRemoteAddr(), "registration successful");
            res.sendRedirect(req.getContextPath() + "/login?registered=true");
        } catch (SQLException e) {
            getServletContext().log("Registration error", e);
            req.setAttribute("error", "Registration failed. Please try again.");
            req.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(req, res);
        }
    }

    private String validateRegistrationInputs(String username, String email,
                                               String password, String confirmPassword) {
        if (!ValidationUtil.isValidUsername(username)) {
            return "Username must be 3–30 characters (letters, numbers, underscore only).";
        }
        if (!ValidationUtil.isValidEmail(email)) {
            return "Please enter a valid email address.";
        }
        if (!PasswordUtil.isPasswordStrong(password)) {
            return "Password must be at least 8 characters with uppercase, lowercase, and a digit.";
        }
        if (!password.equals(confirmPassword)) {
            return "Passwords do not match.";
        }
        return null;
    }

    private User buildNewUser(String username, String email, String password) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(PasswordUtil.hashPassword(password));
        user.setRole(User.Role.USER);
        user.setCreatedAt(System.currentTimeMillis());
        return user;
    }
}
