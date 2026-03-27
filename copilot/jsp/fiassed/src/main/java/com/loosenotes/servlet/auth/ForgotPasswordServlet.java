package com.loosenotes.servlet.auth;

import com.loosenotes.dao.PasswordResetDao;
import com.loosenotes.dao.UserDao;
import com.loosenotes.model.User;
import com.loosenotes.util.AuditLogger;
import com.loosenotes.util.DatabaseManager;
import com.loosenotes.util.SecurityUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Optional;

@WebServlet("/auth/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordServlet.class);
    private final UserDao userDao = new UserDao();
    private final PasswordResetDao passwordResetDao = new PasswordResetDao();
    private final AuditLogger auditLogger = AuditLogger.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/auth/forgot-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email");
        String ip = getClientIp(req);

        // Always show consistent message to prevent enumeration
        String successMsg = "If that email is registered, you will receive a reset link.";

        if (email == null || email.isBlank() || email.length() > 255) {
            req.setAttribute("success", successMsg);
            req.getRequestDispatcher("/WEB-INF/jsp/auth/forgot-password.jsp").forward(req, resp);
            return;
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            Optional<User> userOpt = userDao.findByEmail(conn, email.trim().toLowerCase());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String rawToken = SecurityUtils.generateSecureToken();
                String tokenHash = SecurityUtils.sha256Hex(rawToken);
                LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
                passwordResetDao.insert(conn, user.getId(), tokenHash, expiresAt);
                auditLogger.log("PASSWORD_RESET_REQUEST", user.getId(), null, ip, "SUCCESS", null, null);
                // In production, send email. For demo, log the reset link (never log the raw token in prod)
                logger.info("Password reset link (demo only): /auth/reset-password?token={}", rawToken);
            }
        } catch (Exception e) {
            logger.error("Error processing forgot password", e);
        }

        req.setAttribute("success", successMsg);
        req.getRequestDispatcher("/WEB-INF/jsp/auth/forgot-password.jsp").forward(req, resp);
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
