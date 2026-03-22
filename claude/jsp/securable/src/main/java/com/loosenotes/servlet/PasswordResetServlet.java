package com.loosenotes.servlet;

import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.User;
import com.loosenotes.util.CsrfUtil;
import com.loosenotes.util.PasswordUtil;
import com.loosenotes.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REQ-003 – Password reset (request + confirmation).
 * SSEM: Cryptography (secure token), Logging (token logged instead of emailed).
 *
 * GET  /password-reset           – show request form
 * POST /password-reset           – send reset token (logged as email substitute)
 * GET  /password-reset?token=XXX – show new-password form
 * POST /password-reset?action=reset – apply new password
 */
public class PasswordResetServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(PasswordResetServlet.class.getName());
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        CsrfUtil.getOrCreateToken(req);
        req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));

        String token = req.getParameter("token");
        if (token != null && !token.isBlank()) {
            // Show the new-password form
            req.setAttribute("resetToken", token);
            req.getRequestDispatcher("/jsp/passwordresetform.jsp").forward(req, res);
        } else {
            req.getRequestDispatcher("/jsp/passwordreset.jsp").forward(req, res);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        CsrfUtil.getOrCreateToken(req);
        req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));

        String action = req.getParameter("action");

        if ("reset".equals(action)) {
            handleReset(req, res);
        } else {
            handleRequest(req, res);
        }
    }

    /** Step 1: user submits their email to receive a reset link. */
    private void handleRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String email = ValidationUtil.truncate(req.getParameter("email"), 100);

        if (!ValidationUtil.isValidEmail(email)) {
            req.setAttribute("error", "Please enter a valid email address.");
            req.getRequestDispatcher("/jsp/passwordreset.jsp").forward(req, res);
            return;
        }

        try {
            User user = userDAO.findByEmail(email);
            if (user != null) {
                // Generate a time-limited token (1 hour)
                byte[] bytes = new byte[32];
                RANDOM.nextBytes(bytes);
                String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                LocalDateTime expiry = LocalDateTime.now().plusHours(1);

                userDAO.setResetToken(user.getId(), token, expiry);

                // In production this would be emailed; here we log it for demo purposes
                LOGGER.info("[PASSWORD RESET] Token for user " + user.getUsername()
                        + ": " + token + " (expires " + expiry + ")");
            }
            // Always show the same message to prevent user enumeration
            req.setAttribute("info", "If that email is registered, a reset link has been sent.");
            req.getRequestDispatcher("/jsp/passwordreset.jsp").forward(req, res);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Password reset request error", e);
            req.setAttribute("error", "An error occurred. Please try again.");
            req.getRequestDispatcher("/jsp/passwordreset.jsp").forward(req, res);
        }
    }

    /** Step 2: user submits the token + new password. */
    private void handleReset(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String token    = req.getParameter("resetToken");
        String password = req.getParameter("password");
        String confirm  = req.getParameter("confirmPassword");

        if (ValidationUtil.isBlank(token)) {
            res.sendRedirect(req.getContextPath() + "/password-reset");
            return;
        }

        String pwError = PasswordUtil.validateStrength(password);
        if (pwError != null) {
            req.setAttribute("error", pwError);
            req.setAttribute("resetToken", token);
            req.getRequestDispatcher("/jsp/passwordresetform.jsp").forward(req, res);
            return;
        }
        if (!password.equals(confirm)) {
            req.setAttribute("error", "Passwords do not match.");
            req.setAttribute("resetToken", token);
            req.getRequestDispatcher("/jsp/passwordresetform.jsp").forward(req, res);
            return;
        }

        try {
            User user = userDAO.findByResetToken(token);
            if (user == null) {
                req.setAttribute("error", "Invalid or expired reset token.");
                req.getRequestDispatcher("/jsp/passwordresetform.jsp").forward(req, res);
                return;
            }
            userDAO.updatePassword(user.getId(), PasswordUtil.hash(password));
            userDAO.clearResetToken(user.getId());

            LOGGER.info("Password reset completed for user: " + user.getUsername());
            res.sendRedirect(req.getContextPath() + "/login?reset=1");

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Password reset error", e);
            req.setAttribute("error", "An error occurred. Please try again.");
            req.setAttribute("resetToken", token);
            req.getRequestDispatcher("/jsp/passwordresetform.jsp").forward(req, res);
        }
    }
}
