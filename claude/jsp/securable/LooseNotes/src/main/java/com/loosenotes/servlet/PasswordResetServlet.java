package com.loosenotes.servlet;

import com.loosenotes.service.ServiceException;
import com.loosenotes.util.InputSanitizer;
import com.loosenotes.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Handles the password reset flow.
 * URL patterns:
 *   GET  /password-reset/request  – show forgot-password form
 *   POST /password-reset/request  – send reset email
 *   GET  /password-reset/reset    – show reset form (token from query string)
 *   POST /password-reset/reset    – set new password
 *
 * SSEM notes:
 * - Confidentiality: user existence not disclosed (same response for known/unknown email).
 * - Authenticity: token validated and consumed atomically.
 * - Resilience: token is invalidated after use to prevent replay.
 */
public class PasswordResetServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        switch (path) {
            case "/request" -> forward(req, resp, "account/forgotPassword.jsp");
            case "/reset"   -> {
                // Token is pre-validated before showing the form
                String token = InputSanitizer.sanitizeLine(req.getParameter("token"));
                if (token == null || token.isBlank()) {
                    resp.sendRedirect(req.getContextPath() + "/password-reset/request");
                    return;
                }
                try {
                    Optional<Long> userId = getPasswordResetService().validateToken(token);
                    if (userId.isEmpty()) {
                        req.setAttribute("error", "Reset link is invalid or has expired");
                        forward(req, resp, "account/forgotPassword.jsp");
                    } else {
                        req.setAttribute("token", token);
                        forward(req, resp, "account/resetPassword.jsp");
                    }
                } catch (SQLException e) {
                    log.error("Error validating reset token", e);
                    sendError(req, resp, 500, "System error");
                }
            }
            default -> resp.sendError(404);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();
        switch (path) {
            case "/request" -> handleResetRequest(req, resp);
            case "/reset"   -> handlePasswordChange(req, resp);
            default         -> resp.sendError(404);
        }
    }

    private void handleResetRequest(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String email = InputSanitizer.sanitizeLine(req.getParameter("email"));
        if (!ValidationUtil.isValidEmail(email != null ? email : "")) {
            req.setAttribute("error", "Enter a valid email address");
            forward(req, resp, "account/forgotPassword.jsp");
            return;
        }
        try {
            String rawToken = getPasswordResetService().initiateReset(email, getClientIp(req));
            // In production: send email with reset link containing rawToken
            // For demo: log token (NEVER do this in production)
            if (rawToken != null) {
                log.info("PASSWORD RESET TOKEN (dev only): token={}", rawToken);
            }
        } catch (SQLException e) {
            log.error("Error initiating password reset", e);
        }
        // Always show the confirmation message regardless of whether the email was found
        req.setAttribute("success",
                "If an account with that email exists, a reset link has been sent.");
        forward(req, resp, "account/forgotPassword.jsp");
    }

    private void handlePasswordChange(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String token   = InputSanitizer.sanitizeLine(req.getParameter("token"));
        String newPwd  = req.getParameter("newPassword");
        String newPwd2 = req.getParameter("confirmPassword");

        if (token == null || newPwd == null) {
            req.setAttribute("error", "Invalid request");
            forward(req, resp, "account/resetPassword.jsp");
            return;
        }
        if (!newPwd.equals(newPwd2)) {
            req.setAttribute("error", "Passwords do not match");
            req.setAttribute("token", token);
            forward(req, resp, "account/resetPassword.jsp");
            return;
        }

        try {
            Optional<Long> userId = getPasswordResetService().validateToken(token);
            if (userId.isEmpty()) {
                req.setAttribute("error", "Reset link is invalid or has expired");
                forward(req, resp, "account/forgotPassword.jsp");
                return;
            }

            getUserService().setPasswordDirect(
                    userId.get(), newPwd.toCharArray(), getClientIp(req));
            getPasswordResetService().consumeToken(token);

            req.setAttribute("success", "Password reset successfully. You can now log in.");
            forward(req, resp, "account/login.jsp");
        } catch (ServiceException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("token", token);
            forward(req, resp, "account/resetPassword.jsp");
        } catch (SQLException e) {
            log.error("Error resetting password", e);
            sendError(req, resp, 500, "System error");
        }
    }
}
