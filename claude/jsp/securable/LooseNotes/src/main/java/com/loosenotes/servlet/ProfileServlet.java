package com.loosenotes.servlet;

import com.loosenotes.model.User;
import com.loosenotes.service.ServiceException;
import com.loosenotes.util.InputSanitizer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Handles user profile viewing and editing.
 * URL patterns:
 *   GET  /profile/edit – show edit form
 *   POST /profile/edit – update profile (username, email, optionally password)
 *
 * SSEM notes:
 * - Integrity: updates only own profile (userId from session, never from request body).
 * - Confidentiality: current password required to change password.
 */
public class ProfileServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = getRequiredUser(req, resp);
        if (user == null) return;
        req.setAttribute("user", user);
        forward(req, resp, "profile/edit.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = getRequiredUser(req, resp);
        if (user == null) return;

        // Trust boundary: sanitize inputs; userId comes from session (Derived Integrity)
        String username    = InputSanitizer.sanitizeLine(req.getParameter("username"));
        String email       = InputSanitizer.sanitizeLine(req.getParameter("email"));
        String currentPwd  = req.getParameter("currentPassword");
        String newPwd      = req.getParameter("newPassword");
        String newPwd2     = req.getParameter("confirmPassword");

        try {
            // Always update profile fields
            getUserService().updateProfile(user.getId(), username, email, getClientIp(req));

            // Only change password if a new one is provided
            if (newPwd != null && !newPwd.isEmpty()) {
                if (!newPwd.equals(newPwd2)) {
                    req.setAttribute("error", "New passwords do not match");
                    req.setAttribute("user", user);
                    forward(req, resp, "profile/edit.jsp");
                    return;
                }
                getUserService().changePassword(user.getId(),
                        currentPwd != null ? currentPwd.toCharArray() : new char[0],
                        newPwd.toCharArray(), getClientIp(req));
            }

            // Refresh the session user object with updated data
            User updated = getUserService().findById(user.getId());
            HttpSession session = req.getSession(false);
            if (session != null) session.setAttribute("currentUser", updated);

            req.setAttribute("success", "Profile updated successfully");
            req.setAttribute("user", updated);
            forward(req, resp, "profile/edit.jsp");
        } catch (ServiceException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("user", user);
            forward(req, resp, "profile/edit.jsp");
        } catch (SQLException e) {
            log.error("Error updating profile for user {}", user.getId(), e);
            req.setAttribute("error", "A system error occurred");
            req.setAttribute("user", user);
            forward(req, resp, "profile/edit.jsp");
        }
    }
}
