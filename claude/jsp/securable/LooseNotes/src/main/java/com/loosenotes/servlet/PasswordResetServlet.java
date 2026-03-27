package com.loosenotes.servlet;

import com.loosenotes.util.InputSanitizer;
import com.loosenotes.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Handles password reset request and completion.
 * SSEM: Authenticity - time-limited, single-use cryptographic tokens.
 * SSEM: Confidentiality - consistent response prevents user enumeration.
 */
@WebServlet("/auth/forgot-password")
public class PasswordResetServlet extends BaseServlet {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetServlet.class);

    private static final String FORGOT_JSP = "/WEB-INF/jsp/account/forgotPassword.jsp";
    private static final String RESET_JSP  = "/WEB-INF/jsp/account/resetPassword.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String token = req.getParameter("token");
        if (token != null && !token.isBlank()) {
            showResetForm(req, res, token);
        } else {
            forward(req, res, FORGOT_JSP);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String token = req.getParameter("token");
        if (token != null && !token.isBlank()) {
            handleResetPassword(req, res, token);
        } else {
            handleForgotPassword(req, res);
        }
    }

    private void handleForgotPassword(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String email = InputSanitizer.canonicalizeEmail(req.getParameter("email"));
        if (!ValidationUtil.isValidEmail(email)) {
            forwardWithError(req, res, FORGOT_JSP, "Please enter a valid email address.");
            return;
        }

        try {
            Optional<String> rawToken = getPasswordResetService().requestReset(email);
            // SSEM: Confidentiality - always show the same confirmation regardless
            // In production: send email with rawToken if present
            if (rawToken.isPresent()) {
                getAuditService().logPasswordResetRequested(email, getClientIp(req));
                log.info("Password reset token generated for domain={}", emailDomain(email));
                // TODO: integrate email service to send token in email body
                // For demo: log the reset URL (remove in production!)
                String baseUrl = getAppConfig().getProperty("app.baseUrl", "http://localhost:8080/loose-notes");
                log.info("[DEMO ONLY] Reset URL: {}/auth/forgot-password?token={}",
                    baseUrl, rawToken.get());
            }
            // Always show confirmation (SSEM: Confidentiality - no enumeration)
            req.setAttribute("emailSent", true);
            forward(req, res, FORGOT_JSP);
        } catch (SQLException e) {
            log.error("Error processing password reset request", e);
            req.setAttribute("emailSent", true); // Still show success to prevent enumeration
            forward(req, res, FORGOT_JSP);
        }
    }

    private void showResetForm(HttpServletRequest req, HttpServletResponse res, String token)
            throws ServletException, IOException {
        if (!ValidationUtil.isValidToken(token)) {
            forwardWithError(req, res, RESET_JSP, "Invalid or expired reset link.");
            return;
        }
        try {
            Optional<Long> userId = getPasswordResetService().validateToken(token);
            if (userId.isEmpty()) {
                forwardWithError(req, res, RESET_JSP, "This reset link has expired or already been used.");
                return;
            }
            req.setAttribute("token", token);
            forward(req, res, RESET_JSP);
        } catch (SQLException e) {
            log.error("Error validating reset token", e);
            forwardWithError(req, res, RESET_JSP, "A system error occurred.");
        }
    }

    private void handleResetPassword(HttpServletRequest req, HttpServletResponse res, String token)
            throws ServletException, IOException {
        if (!ValidationUtil.isValidToken(token)) {
            forwardWithError(req, res, RESET_JSP, "Invalid reset link.");
            return;
        }

        String newPassword = req.getParameter("newPassword");
        String confirm     = req.getParameter("confirmPassword");

        if (!newPassword.equals(confirm)) {
            req.setAttribute("token", token);
            forwardWithError(req, res, RESET_JSP, "Passwords do not match.");
            return;
        }

        try {
            boolean success = getPasswordResetService().completeReset(token, newPassword);
            if (!success) {
                forwardWithError(req, res, RESET_JSP,
                    "This reset link has expired or already been used.");
                return;
            }
            redirect(res, req, "/auth/login?passwordReset=true");
        } catch (IllegalArgumentException e) {
            req.setAttribute("token", token);
            forwardWithError(req, res, RESET_JSP, e.getMessage());
        } catch (SQLException e) {
            log.error("Error completing password reset", e);
            forwardWithError(req, res, RESET_JSP, "A system error occurred.");
        }
    }

    private String emailDomain(String email) {
        if (email == null) return "unknown";
        int at = email.indexOf('@');
        return at >= 0 ? email.substring(at + 1) : "unknown";
    }
}
