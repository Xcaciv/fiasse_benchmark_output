package com.loosenotes.servlet;

import com.loosenotes.dao.ActivityLogDAO;
import com.loosenotes.dao.PasswordResetTokenDAO;
import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.PasswordResetToken;
import com.loosenotes.model.User;
import com.loosenotes.util.PasswordUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class PasswordResetServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final PasswordResetTokenDAO tokenDAO = new PasswordResetTokenDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");

        if ("reset".equals(action)) {
            String token = request.getParameter("token");
            if (token == null || token.trim().isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/password-reset");
                return;
            }

            try {
                PasswordResetToken resetToken = tokenDAO.findByToken(token);
                if (resetToken == null || !resetToken.isValid()) {
                    request.setAttribute("error", "This reset link is invalid or has expired.");
                    request.getRequestDispatcher("/WEB-INF/jsp/passwordReset.jsp").forward(request, response);
                    return;
                }
                request.setAttribute("token", token);
                request.getRequestDispatcher("/WEB-INF/jsp/passwordResetForm.jsp").forward(request, response);
            } catch (SQLException e) {
                getServletContext().log("Password reset form error", e);
                request.setAttribute("error", "An error occurred. Please try again.");
                request.getRequestDispatcher("/WEB-INF/jsp/passwordReset.jsp").forward(request, response);
            }
        } else {
            // Show request form
            request.getRequestDispatcher("/WEB-INF/jsp/passwordReset.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");

        if ("reset".equals(action)) {
            handleResetPassword(request, response);
        } else {
            handleRequestReset(request, response);
        }
    }

    private void handleRequestReset(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        if (username == null || username.trim().isEmpty()) {
            request.setAttribute("error", "Username is required.");
            request.getRequestDispatcher("/WEB-INF/jsp/passwordReset.jsp").forward(request, response);
            return;
        }

        username = username.trim();

        try {
            User user = userDAO.findByUsername(username);
            if (user == null) {
                // Don't reveal if user exists for security, but for demo we show the token
                request.setAttribute("error", "No user found with that username.");
                request.getRequestDispatcher("/WEB-INF/jsp/passwordReset.jsp").forward(request, response);
                return;
            }

            // Generate reset token
            String token = UUID.randomUUID().toString();
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
            tokenDAO.create(user.getId(), token, expiresAt);

            activityLogDAO.log(user.getId(), "PASSWORD_RESET_REQUEST", "Reset token generated for user: " + username);

            // Show token on screen (no email in demo mode)
            request.setAttribute("resetToken", token);
            request.setAttribute("username", username);
            request.setAttribute("success", "Password reset token generated. Use the link below within 1 hour.");
            request.getRequestDispatcher("/WEB-INF/jsp/passwordReset.jsp").forward(request, response);

        } catch (SQLException e) {
            getServletContext().log("Password reset request error", e);
            request.setAttribute("error", "An error occurred. Please try again.");
            request.getRequestDispatcher("/WEB-INF/jsp/passwordReset.jsp").forward(request, response);
        }
    }

    private void handleResetPassword(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String token = request.getParameter("token");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        if (token == null || newPassword == null || confirmPassword == null) {
            request.setAttribute("error", "All fields are required.");
            request.setAttribute("token", token);
            request.getRequestDispatcher("/WEB-INF/jsp/passwordResetForm.jsp").forward(request, response);
            return;
        }

        if (newPassword.length() < 6) {
            request.setAttribute("error", "Password must be at least 6 characters.");
            request.setAttribute("token", token);
            request.getRequestDispatcher("/WEB-INF/jsp/passwordResetForm.jsp").forward(request, response);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            request.setAttribute("error", "Passwords do not match.");
            request.setAttribute("token", token);
            request.getRequestDispatcher("/WEB-INF/jsp/passwordResetForm.jsp").forward(request, response);
            return;
        }

        try {
            PasswordResetToken resetToken = tokenDAO.findByToken(token);

            if (resetToken == null || !resetToken.isValid()) {
                request.setAttribute("error", "This reset link is invalid or has expired.");
                request.getRequestDispatcher("/WEB-INF/jsp/passwordReset.jsp").forward(request, response);
                return;
            }

            String newHash = PasswordUtil.hashPassword(newPassword);
            userDAO.updatePassword(resetToken.getUserId(), newHash);
            tokenDAO.markUsed(token);

            activityLogDAO.log(resetToken.getUserId(), "PASSWORD_RESET", "Password was reset via token");

            request.setAttribute("success", "Password reset successfully. You can now log in with your new password.");
            request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);

        } catch (SQLException e) {
            getServletContext().log("Password reset error", e);
            request.setAttribute("error", "An error occurred. Please try again.");
            request.setAttribute("token", token);
            request.getRequestDispatcher("/WEB-INF/jsp/passwordResetForm.jsp").forward(request, response);
        }
    }
}
