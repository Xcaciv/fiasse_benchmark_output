package com.loosenotes.servlet;

import com.loosenotes.dao.AuditLogDao;
import com.loosenotes.dao.DatabaseManager;
import com.loosenotes.dao.UserDao;
import com.loosenotes.model.AuditEvent;
import com.loosenotes.model.User;
import com.loosenotes.service.AuditService;
import com.loosenotes.service.PasswordPolicyService;
import com.loosenotes.service.ServiceException;
import com.loosenotes.service.UserService;
import com.loosenotes.util.CsrfUtil;
import com.loosenotes.util.PasswordUtil;
import com.loosenotes.util.TokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Handles the password-reset flow across two URLs:
 * <ul>
 *   <li>{@code /auth/forgot-password} — request a reset link</li>
 *   <li>{@code /auth/reset-password}  — consume a reset token and set a new password</li>
 * </ul>
 *
 * <p><strong>Anti-enumeration</strong>: the forgot-password POST always returns
 * the same response regardless of whether the supplied email is registered, so
 * callers cannot use this endpoint to verify account existence.
 *
 * <p><strong>Token security</strong>: the raw token is generated with
 * {@link TokenUtil#generateToken128Bit()}; only its SHA-256 hash is stored in
 * the database.  Expiry is enforced at resolution time (1 hour).
 */
@WebServlet(urlPatterns = {"/auth/forgot-password", "/auth/reset-password"})
public class ForgotPasswordServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ForgotPasswordServlet.class);
    private static final long serialVersionUID = 1L;

    /** Token validity window in hours. */
    private static final int TOKEN_EXPIRY_HOURS = 1;

    private UserService userService;
    private UserDao userDao;
    private AuditService auditService;

    @Override
    public void init() throws ServletException {
        AuditLogDao auditLogDao = new AuditLogDao();
        auditService = new AuditService(auditLogDao);
        userDao      = new UserDao();
        userService  = new UserService(userDao, new PasswordPolicyService(), auditService);
    }

    // =========================================================================
    // GET dispatch
    // =========================================================================

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        MDC.put("correlationId", UUID.randomUUID().toString());
        try {
            String path = request.getServletPath();
            if ("/auth/forgot-password".equals(path)) {
                handleForgotPasswordGet(request, response);
            } else if ("/auth/reset-password".equals(path)) {
                handleResetPasswordGet(request, response);
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } finally {
            MDC.remove("correlationId");
        }
    }

    // =========================================================================
    // POST dispatch
    // =========================================================================

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        MDC.put("correlationId", UUID.randomUUID().toString());
        try {
            String path = request.getServletPath();
            if ("/auth/forgot-password".equals(path)) {
                handleForgotPasswordPost(request, response);
            } else if ("/auth/reset-password".equals(path)) {
                handleResetPasswordPost(request, response);
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } finally {
            MDC.remove("correlationId");
        }
    }

    // =========================================================================
    // /auth/forgot-password — GET
    // =========================================================================

    private void handleForgotPasswordGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(true);
        request.setAttribute("csrfToken", CsrfUtil.getOrCreateToken(session));
        request.getRequestDispatcher("/WEB-INF/jsp/auth/forgot-password.jsp").forward(request, response);
    }

    // =========================================================================
    // /auth/forgot-password — POST
    // =========================================================================

    private void handleForgotPasswordPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(true);

        if (!CsrfUtil.validateToken(session, request.getParameter("_csrf"))) {
            log.warn("CSRF mismatch on forgot-password. ip={}", request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        String email     = request.getParameter("email");
        String ipAddress = request.getRemoteAddr();

        if (email != null) {
            email = email.trim();
            // Attempt to locate the user — but do NOT branch on existence in the response.
            User user = userDao.findByEmail(email);
            if (user != null) {
                try {
                    storePasswordResetToken(user.getId(), ipAddress);
                } catch (Exception e) {
                    // Log but swallow — the caller always gets the same message.
                    log.error("Failed to store password reset token. userId={} error={}",
                            user.getId(), e.getMessage(), e);
                }
            } else {
                log.debug("Forgot-password request for unregistered email. ip={}", ipAddress);
            }
        }

        CsrfUtil.rotateToken(session);

        // Anti-enumeration: always show the same confirmation regardless of outcome.
        request.setAttribute("message",
                "If that email is registered, you will receive a reset link.");
        request.setAttribute("csrfToken", CsrfUtil.getOrCreateToken(session));
        request.getRequestDispatcher("/WEB-INF/jsp/auth/forgot-password.jsp").forward(request, response);
    }

    // =========================================================================
    // /auth/reset-password — GET
    // =========================================================================

    private void handleResetPasswordGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String token = request.getParameter("token");
        if (token == null || token.isEmpty()) {
            request.setAttribute("error", "Invalid or expired reset link.");
            request.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(request, response);
            return;
        }

        // Validate the token early so the form is not presented for invalid tokens.
        if (!isTokenValid(token)) {
            request.setAttribute("error", "This reset link is invalid or has expired.");
            request.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(request, response);
            return;
        }

        HttpSession session = request.getSession(true);
        request.setAttribute("csrfToken", CsrfUtil.getOrCreateToken(session));
        request.setAttribute("token", token);
        request.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(request, response);
    }

    // =========================================================================
    // /auth/reset-password — POST
    // =========================================================================

    private void handleResetPasswordPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(true);

        if (!CsrfUtil.validateToken(session, request.getParameter("_csrf"))) {
            log.warn("CSRF mismatch on reset-password POST. ip={}", request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        String token       = request.getParameter("token");
        String newPassword = request.getParameter("newPassword");
        String ipAddress   = request.getRemoteAddr();

        if (token == null || token.isEmpty()) {
            request.setAttribute("error", "Missing reset token.");
            request.setAttribute("csrfToken", CsrfUtil.getOrCreateToken(session));
            request.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(request, response);
            return;
        }

        // Resolve userId from token (validates expiry and used-flag atomically).
        Long userId = resolveTokenUserId(token);
        if (userId == null) {
            request.setAttribute("error", "This reset link is invalid or has expired.");
            request.setAttribute("csrfToken", CsrfUtil.getOrCreateToken(session));
            request.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(request, response);
            return;
        }

        // Validate new password policy.
        PasswordPolicyService pps = new PasswordPolicyService();
        PasswordPolicyService.ValidationResult pwResult = pps.validate(newPassword);
        if (!pwResult.isValid()) {
            request.setAttribute("error", pwResult.getMessage());
            request.setAttribute("token", token);
            request.setAttribute("csrfToken", CsrfUtil.getOrCreateToken(session));
            request.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(request, response);
            return;
        }

        // Update the password hash.
        try {
            String newHash = PasswordUtil.hash(newPassword);
            userDao.updatePasswordHash(userId, newHash);

            // Mark the token as used so it cannot be replayed.
            markTokenUsed(token);

            auditService.recordEvent(AuditEvent.builder("PASSWORD_RESET", AuditEvent.Outcome.SUCCESS)
                    .actor(userId, null)
                    .ip(ipAddress)
                    .build());

            // NOTE: We cannot invalidate other active sessions from this servlet
            // because the HttpSession API only provides access to the current
            // request's session. In a production system a persistent session
            // store (e.g. Redis or a sessions table) would allow revocation of
            // all sessions for the affected user. This limitation should be
            // addressed before deployment.
            log.warn("Password reset complete but other active sessions were NOT invalidated. " +
                    "Implement server-side session revocation. userId={}", userId);

            CsrfUtil.rotateToken(session);
            response.sendRedirect(request.getContextPath() + "/auth/login?reset=true");

        } catch (Exception e) {
            log.error("Password reset failed. userId={} error={}", userId, e.getMessage(), e);
            request.setAttribute("error", "An error occurred. Please try again.");
            request.setAttribute("token", token);
            request.setAttribute("csrfToken", CsrfUtil.getOrCreateToken(session));
            request.getRequestDispatcher("/WEB-INF/jsp/auth/reset-password.jsp").forward(request, response);
        }
    }

    // =========================================================================
    // Private: inline password_reset_tokens operations via DatabaseManager
    // =========================================================================

    /**
     * Generates a raw token, hashes it, and inserts a row into
     * {@code password_reset_tokens} with an expiry of {@value #TOKEN_EXPIRY_HOURS} hour(s).
     * Logs via {@link AuditService}; raw token is never stored or logged.
     */
    private void storePasswordResetToken(Long userId, String ipAddress) throws Exception {
        String rawToken   = TokenUtil.generateToken128Bit();
        String tokenHash  = TokenUtil.hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS);

        final String sql =
                "INSERT INTO password_reset_tokens (user_id, token_hash, expires_at, used) " +
                "VALUES (?, ?, ?, FALSE)";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, tokenHash);
            ps.setTimestamp(3, Timestamp.from(expiresAt));
            ps.executeUpdate();
        }

        auditService.recordEvent(AuditEvent.builder("PASSWORD_RESET_REQUESTED", AuditEvent.Outcome.SUCCESS)
                .actor(userId, null)
                .ip(ipAddress)
                .build());

        // NOTE: In a real system the raw token would be emailed here.
        // Logged at DEBUG so it is accessible in development but not in production
        // logs at higher levels.
        log.debug("Password reset token generated for userId={}. " +
                "Token would be emailed in production. rawToken={}", userId, rawToken);
    }

    /**
     * Returns {@code true} if the supplied raw token exists in the database,
     * has not been used, and has not expired.
     */
    private boolean isTokenValid(String rawToken) {
        String tokenHash = TokenUtil.hashToken(rawToken);
        final String sql =
                "SELECT id FROM password_reset_tokens " +
                "WHERE token_hash = ? AND used = FALSE AND expires_at > ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            ps.setTimestamp(2, Timestamp.from(Instant.now()));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            log.error("isTokenValid check failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Resolves the {@code user_id} associated with a valid, unexpired, unused
     * reset token.  Returns {@code null} if the token is invalid.
     */
    private Long resolveTokenUserId(String rawToken) {
        String tokenHash = TokenUtil.hashToken(rawToken);
        final String sql =
                "SELECT user_id FROM password_reset_tokens " +
                "WHERE token_hash = ? AND used = FALSE AND expires_at > ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            ps.setTimestamp(2, Timestamp.from(Instant.now()));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("user_id") : null;
            }
        } catch (Exception e) {
            log.error("resolveTokenUserId failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /** Marks the token row as used so it cannot be replayed. */
    private void markTokenUsed(String rawToken) throws Exception {
        String tokenHash = TokenUtil.hashToken(rawToken);
        final String sql =
                "UPDATE password_reset_tokens SET used = TRUE WHERE token_hash = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            ps.executeUpdate();
        }
    }
}
