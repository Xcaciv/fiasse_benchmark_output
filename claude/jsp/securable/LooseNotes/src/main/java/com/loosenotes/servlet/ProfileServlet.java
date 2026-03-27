package com.loosenotes.servlet;

import com.loosenotes.model.User;
import com.loosenotes.util.InputSanitizer;
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
 * Handles user profile management.
 * SSEM: Authenticity - only allows users to modify their own profile.
 * SSEM: Integrity - validates all input before update.
 */
@WebServlet("/profile/*")
public class ProfileServlet extends BaseServlet {

    private static final Logger log = LoggerFactory.getLogger(ProfileServlet.class);
    private static final String EDIT_JSP = "/WEB-INF/jsp/profile/edit.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        forward(req, res, EDIT_JSP);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String action = getAction(req);
        switch (action) {
            case "update"          -> handleUpdateProfile(req, res);
            case "change-password" -> handleChangePassword(req, res);
            default                -> forward(req, res, EDIT_JSP);
        }
    }

    private void handleUpdateProfile(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        User user       = getCurrentUser(req);
        String username = InputSanitizer.sanitizeSingleLine(req.getParameter("username"));
        String email    = InputSanitizer.canonicalizeEmail(req.getParameter("email"));

        try {
            boolean success = getUserService().updateProfile(user.getId(), username, email);
            if (!success) {
                forwardWithError(req, res, EDIT_JSP, "Username is already taken.");
                return;
            }
            // Refresh user in session
            Optional<User> updated = getUserService().findById(user.getId());
            updated.ifPresent(u -> {
                HttpSession session = req.getSession(false);
                if (session != null) session.setAttribute("user", u);
            });
            req.setAttribute("success", "Profile updated successfully.");
            forward(req, res, EDIT_JSP);
        } catch (IllegalArgumentException e) {
            forwardWithError(req, res, EDIT_JSP, e.getMessage());
        } catch (SQLException e) {
            log.error("Error updating profile for user id={}", user.getId(), e);
            forwardWithError(req, res, EDIT_JSP, "Could not update profile.");
        }
    }

    private void handleChangePassword(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        User user       = getCurrentUser(req);
        String current  = req.getParameter("currentPassword");
        String newPwd   = req.getParameter("newPassword");
        String confirm  = req.getParameter("confirmPassword");

        if (!newPwd.equals(confirm)) {
            forwardWithError(req, res, EDIT_JSP, "New passwords do not match.");
            return;
        }
        try {
            boolean changed = getUserService().changePassword(user.getId(), current, newPwd);
            if (!changed) {
                forwardWithError(req, res, EDIT_JSP, "Current password is incorrect.");
                return;
            }
            req.setAttribute("success", "Password changed successfully.");
            forward(req, res, EDIT_JSP);
        } catch (IllegalArgumentException e) {
            forwardWithError(req, res, EDIT_JSP, e.getMessage());
        } catch (SQLException e) {
            log.error("Error changing password for user id={}", user.getId(), e);
            forwardWithError(req, res, EDIT_JSP, "Could not change password.");
        }
    }

    private String getAction(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) return "";
        return pathInfo.substring(1).split("/")[0];
    }
}
