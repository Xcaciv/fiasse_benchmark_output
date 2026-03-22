package com.loosenotes.servlet;

import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.User;
import com.loosenotes.util.CsrfUtil;
import com.loosenotes.util.PasswordUtil;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REQ-014 – Profile management (update username, email, password).
 * SSEM: Authorization (only own profile), Input Validation, Logging.
 */
public class ProfileServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ProfileServlet.class.getName());
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        long userId = (long) req.getSession().getAttribute("userId");
        CsrfUtil.getOrCreateToken(req);
        req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));

        try {
            User user = userDAO.findById(userId);
            req.setAttribute("user", user);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading profile", e);
        }
        req.getRequestDispatcher("/jsp/profile.jsp").forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        long userId  = (long) req.getSession().getAttribute("userId");
        String action = req.getParameter("action");
        CsrfUtil.getOrCreateToken(req);
        req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));

        try {
            if ("updateProfile".equals(action)) {
                updateProfile(req, res, userId);
            } else if ("updatePassword".equals(action)) {
                updatePassword(req, res, userId);
            } else {
                res.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Profile update error for user " + userId, e);
            req.setAttribute("error", "Update failed. Please try again.");
            req.getRequestDispatcher("/jsp/profile.jsp").forward(req, res);
        }
    }

    private void updateProfile(HttpServletRequest req, HttpServletResponse res, long userId)
            throws SQLException, ServletException, IOException {

        String username = ValidationUtil.truncate(req.getParameter("username"), 50);
        String email    = ValidationUtil.truncate(req.getParameter("email"), 100);

        if (!ValidationUtil.isValidUsername(username)) {
            req.setAttribute("error", "Invalid username.");
            req.getRequestDispatcher("/jsp/profile.jsp").forward(req, res);
            return;
        }
        if (!ValidationUtil.isValidEmail(email)) {
            req.setAttribute("error", "Invalid email address.");
            req.getRequestDispatcher("/jsp/profile.jsp").forward(req, res);
            return;
        }

        // Check if username taken by another user
        User existing = userDAO.findByUsername(username);
        if (existing != null && existing.getId() != userId) {
            req.setAttribute("error", "Username already taken.");
            req.getRequestDispatcher("/jsp/profile.jsp").forward(req, res);
            return;
        }

        userDAO.updateProfile(userId, username, email);

        // Update session attributes
        req.getSession().setAttribute("username", username);
        LOGGER.info("Profile updated for user " + userId);

        req.setAttribute("success", "Profile updated successfully.");
        req.setAttribute("user", userDAO.findById(userId));
        req.getRequestDispatcher("/jsp/profile.jsp").forward(req, res);
    }

    private void updatePassword(HttpServletRequest req, HttpServletResponse res, long userId)
            throws SQLException, ServletException, IOException {

        String current  = req.getParameter("currentPassword");
        String newPass  = req.getParameter("newPassword");
        String confirm  = req.getParameter("confirmPassword");

        User user = userDAO.findById(userId);
        if (!PasswordUtil.verify(current, user.getPasswordHash())) {
            req.setAttribute("error", "Current password is incorrect.");
            req.setAttribute("user", user);
            req.getRequestDispatcher("/jsp/profile.jsp").forward(req, res);
            return;
        }

        String pwError = PasswordUtil.validateStrength(newPass);
        if (pwError != null) {
            req.setAttribute("error", pwError);
            req.setAttribute("user", user);
            req.getRequestDispatcher("/jsp/profile.jsp").forward(req, res);
            return;
        }
        if (!newPass.equals(confirm)) {
            req.setAttribute("error", "New passwords do not match.");
            req.setAttribute("user", user);
            req.getRequestDispatcher("/jsp/profile.jsp").forward(req, res);
            return;
        }

        userDAO.updatePassword(userId, PasswordUtil.hash(newPass));
        LOGGER.info("Password changed for user " + userId);

        req.setAttribute("success", "Password updated successfully.");
        req.setAttribute("user", userDAO.findById(userId));
        req.getRequestDispatcher("/jsp/profile.jsp").forward(req, res);
    }
}
