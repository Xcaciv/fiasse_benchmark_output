package com.loosenotes.web;

import com.loosenotes.model.User;
import com.loosenotes.util.PasswordUtil;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@WebServlet("/profile")
public class ProfileServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireLogin(request, response)) {
            return;
        }
        request.setAttribute("pageTitle", "Profile");
        render(request, response, "profile");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!requireLogin(request, response) || !requireCsrf(request, response)) {
            return;
        }

        User currentUser = currentUser(request);
        String username = ValidationUtil.normalizeUsername(request.getParameter("username"));
        String email = ValidationUtil.normalizeEmail(request.getParameter("email"));
        String currentPassword = request.getParameter("currentPassword") == null ? "" : request.getParameter("currentPassword");
        String newPassword = request.getParameter("newPassword") == null ? "" : request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword") == null ? "" : request.getParameter("confirmPassword");

        request.setAttribute("pageTitle", "Profile");
        request.setAttribute("usernameValue", username);
        request.setAttribute("emailValue", email);

        try {
            ValidationUtil.requireUsername(username);
            ValidationUtil.requireEmail(email);

            Optional<User> byUsername = app().getUserDao().findByUsername(username);
            if (byUsername.isPresent() && byUsername.get().getId() != currentUser.getId()) {
                throw new IllegalArgumentException("That username is already taken.");
            }
            Optional<User> byEmail = app().getUserDao().findByEmail(email);
            if (byEmail.isPresent() && byEmail.get().getId() != currentUser.getId()) {
                throw new IllegalArgumentException("That email address is already in use.");
            }

            app().getUserDao().updateProfile(currentUser.getId(), username, email);
            if (newPassword != null && !newPassword.isBlank()) {
                if (currentPassword.isBlank()) {
                    throw new IllegalArgumentException("Enter your current password to set a new password.");
                }
                if (!PasswordUtil.matches(currentPassword, currentUser.getPasswordHash())) {
                    throw new IllegalArgumentException("Current password is incorrect.");
                }
                ValidationUtil.requirePassword(newPassword);
                if (!newPassword.equals(confirmPassword)) {
                    throw new IllegalArgumentException("New passwords do not match.");
                }
                app().getUserDao().updatePassword(currentUser.getId(), PasswordUtil.hash(newPassword));
            }

            User refreshedUser = app().getUserDao().findById(currentUser.getId()).orElseThrow();
            request.getSession().setAttribute("authUser", refreshedUser);
            app().getActivityLogDao().log(refreshedUser.getId(), "profile.updated", "Updated profile details for " + refreshedUser.getUsername() + '.');
            setFlash(request, "success", "Profile updated successfully.");
            redirect(request, response, "/profile");
        } catch (IllegalArgumentException ex) {
            request.setAttribute("errorMessage", ex.getMessage());
            render(request, response, "profile");
        }
    }
}
