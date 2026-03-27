package com.loosenotes.servlet.auth;

import com.loosenotes.dao.PasswordResetDao;
import com.loosenotes.dao.SessionDao;
import com.loosenotes.dao.UserDao;
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
import java.util.Optional;

@WebServlet("/auth/reset-password")
public class ResetPasswordServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ResetPasswordServlet.class);
    private final PasswordResetDao passwordResetDao = new PasswordResetDao();
    private final UserDao userDao = new UserDao();
    private final SessionDao sessionDao = new SessionDao();
    private final AuditLogger auditLogger = AuditLogger.getInstance();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        if (token == null || token.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid reset link");
            return;
        }
        req.setAttribute("token", token);
        req.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        String newPassword = req.getParameter("newPassword");
        String confirmPassword = req.getParameter("confirmPassword");
        String ip = getClientIp(req);

        if (token == null || token.isBlank()) {
            req.setAttribute("error", "Invalid reset link");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(req, resp);
            return;
        }

        if (newPassword == null || newPassword.length() < 12 || newPassword.length() > 64) {
            req.setAttribute("error", "Password must be between 12 and 64 characters");
            req.setAttribute("token", token);
            req.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(req, resp);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            req.setAttribute("error", "Passwords do not match");
            req.setAttribute("token", token);
            req.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(req, resp);
            return;
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String tokenHash = SecurityUtils.sha256Hex(token);
            Optional<Long> userIdOpt = passwordResetDao.findValidUserId(conn, tokenHash);
            if (userIdOpt.isEmpty()) {
                req.setAttribute("error", "Invalid or expired reset link");
                req.setAttribute("token", token);
                req.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(req, resp);
                return;
            }
            long userId = userIdOpt.get();
            userDao.updatePassword(conn, userId, SecurityUtils.hashPassword(newPassword));
            passwordResetDao.markUsed(conn, tokenHash);
            sessionDao.deleteByUserId(conn, userId);
            auditLogger.log("PASSWORD_RESET_COMPLETE", userId, null, ip, "SUCCESS", null, null);
            req.setAttribute("success", "Password reset successful. Please log in.");
            req.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(req, resp);
        } catch (Exception e) {
            logger.error("Error resetting password", e);
            req.setAttribute("error", "An error occurred. Please try again.");
            req.setAttribute("token", token);
            req.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(req, resp);
        }
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
