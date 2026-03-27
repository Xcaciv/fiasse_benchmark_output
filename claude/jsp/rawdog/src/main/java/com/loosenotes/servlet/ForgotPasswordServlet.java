package com.loosenotes.servlet;

import com.loosenotes.dao.PasswordResetTokenDAO;
import com.loosenotes.dao.UserDAO;
import com.loosenotes.model.User;
import com.loosenotes.util.TokenUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.logging.Logger;

@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ForgotPasswordServlet.class.getName());
    private final UserDAO userDAO = new UserDAO();
    private final PasswordResetTokenDAO tokenDAO = new PasswordResetTokenDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/auth/forgot-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String email = req.getParameter("email");
        if (email == null || email.isBlank()) {
            req.setAttribute("error", "Email is required.");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/forgot-password.jsp").forward(req, resp);
            return;
        }

        User user = userDAO.findByEmail(email.trim());
        // Always show success to prevent email enumeration
        if (user != null) {
            String token = TokenUtil.generateResetToken();
            long expiryMs = System.currentTimeMillis() + 3600_000L; // 1 hour
            Timestamp expiresAt = new Timestamp(expiryMs);
            tokenDAO.create(user.getId(), token, expiresAt);

            String resetUrl = req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
                + req.getContextPath() + "/reset-password?token=" + token;

            // Simulate email via log
            LOGGER.warning("PASSWORD RESET TOKEN for " + email + ": " + token);
            LOGGER.warning("PASSWORD RESET URL: " + resetUrl);
            System.out.println("=== PASSWORD RESET ===");
            System.out.println("Email: " + email);
            System.out.println("Reset URL: " + resetUrl);
            System.out.println("======================");
        }

        req.setAttribute("success", "If that email is registered, a reset link has been logged to the server console.");
        req.getRequestDispatcher("/WEB-INF/jsp/auth/forgot-password.jsp").forward(req, resp);
    }
}
