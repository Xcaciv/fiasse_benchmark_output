package com.loosenotes.servlet;

import com.loosenotes.dao.AuditLogDao;
import com.loosenotes.dao.UserDao;
import com.loosenotes.model.User;
import com.loosenotes.service.AuditService;
import com.loosenotes.service.PasswordPolicyService;
import com.loosenotes.service.ServiceException;
import com.loosenotes.service.UserService;
import com.loosenotes.util.CsrfUtil;
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
import java.util.UUID;

/**
 * Handles the authenticated user's profile page and self-service account
 * mutations at {@code /profile/*}.
 *
 * <p>Routes:
 * <ul>
 *   <li>{@code GET  /profile}                   — display profile</li>
 *   <li>{@code POST /profile/change-password}   — update password</li>
 *   <li>{@code POST /profile/change-username}   — update username</li>
 *   <li>{@code POST /profile/change-email}      — update email</li>
 * </ul>
 *
 * <p>All mutations require the current password for re-verification, enforced
 * by {@link UserService}.
 */
@WebServlet("/profile/*")
public class ProfileServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ProfileServlet.class);
    private static final long serialVersionUID = 1L;

    private UserService userService;

    @Override
    public void init() throws ServletException {
        userService = new UserService(
                new UserDao(),
                new PasswordPolicyService(),
                new AuditService(new AuditLogDao())
        );
    }

    // =========================================================================
    // GET dispatch
    // =========================================================================

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        MDC.put("correlationId", UUID.randomUUID().toString());
        try {
            Long userId = getRequiredUserId(request, response);
            if (userId == null) return;

            String pathInfo = request.getPathInfo();

            // GET /profile  or  GET /profile/
            if (pathInfo == null || pathInfo.equals("/")) {
                handleProfileView(request, response, userId);
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
            Long userId = getRequiredUserId(request, response);
            if (userId == null) return;

            HttpSession session = request.getSession(false);
            if (!CsrfUtil.validateToken(session, request.getParameter("_csrf"))) {
                log.warn("CSRF mismatch on profile action. userId={} ip={}", userId, request.getRemoteAddr());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
                return;
            }

            String pathInfo = request.getPathInfo();
            if (pathInfo == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            switch (pathInfo) {
                case "/change-password":
                    handleChangePassword(request, response, session, userId);
                    break;
                case "/change-username":
                    handleChangeUsername(request, response, session, userId);
                    break;
                case "/change-email":
                    handleChangeEmail(request, response, session, userId);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }

        } finally {
            MDC.remove("correlationId");
        }
    }

    // =========================================================================
    // Route handlers
    // =========================================================================

    private void handleProfileView(HttpServletRequest request, HttpServletResponse response,
                                   Long userId) throws ServletException, IOException {

        User user = userService.findById(userId);
        if (user == null) {
            // Session references a deleted account — force logout.
            HttpSession session = request.getSession(false);
            if (session != null) session.invalidate();
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        HttpSession session = request.getSession(false);
        request.setAttribute("user", user);
        request.setAttribute("csrfToken", CsrfUtil.getOrCreateToken(session));
        request.getRequestDispatcher("/WEB-INF/jsp/user/profile.jsp").forward(request, response);
    }

    private void handleChangePassword(HttpServletRequest request, HttpServletResponse response,
                                      HttpSession session, Long userId)
            throws ServletException, IOException {

        String currentPassword = request.getParameter("currentPassword");
        String newPassword     = request.getParameter("newPassword");

        try {
            userService.changePassword(userId, currentPassword, newPassword);
            CsrfUtil.rotateToken(session);
            log.info("Password changed via profile. userId={}", userId);
            response.sendRedirect(request.getContextPath() + "/profile?success=password");

        } catch (ServiceException e) {
            forwardToProfileWithError(request, response, session, userId, e.getMessage());
        }
    }

    private void handleChangeUsername(HttpServletRequest request, HttpServletResponse response,
                                      HttpSession session, Long userId)
            throws ServletException, IOException {

        String currentPassword = request.getParameter("currentPassword");
        String newUsername     = request.getParameter("newUsername");

        try {
            userService.changeUsername(userId, currentPassword, newUsername);
            // Update the username in the session so the navbar stays consistent.
            session.setAttribute("username", newUsername);
            CsrfUtil.rotateToken(session);
            log.info("Username changed via profile. userId={}", userId);
            response.sendRedirect(request.getContextPath() + "/profile?success=username");

        } catch (ServiceException e) {
            forwardToProfileWithError(request, response, session, userId, e.getMessage());
        }
    }

    private void handleChangeEmail(HttpServletRequest request, HttpServletResponse response,
                                   HttpSession session, Long userId)
            throws ServletException, IOException {

        String currentPassword = request.getParameter("currentPassword");
        String newEmail        = request.getParameter("newEmail");

        try {
            userService.changeEmail(userId, currentPassword, newEmail);
            CsrfUtil.rotateToken(session);
            log.info("Email changed via profile. userId={}", userId);
            response.sendRedirect(request.getContextPath() + "/profile?success=email");

        } catch (ServiceException e) {
            forwardToProfileWithError(request, response, session, userId, e.getMessage());
        }
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void forwardToProfileWithError(HttpServletRequest request, HttpServletResponse response,
                                           HttpSession session, Long userId, String errorMessage)
            throws ServletException, IOException {

        User user = userService.findById(userId);
        request.setAttribute("user", user);
        request.setAttribute("error", errorMessage);
        request.setAttribute("csrfToken", CsrfUtil.getOrCreateToken(session));
        request.getRequestDispatcher("/WEB-INF/jsp/user/profile.jsp").forward(request, response);
    }

    private Long getRequiredUserId(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Long userId = (Long) session.getAttribute("userId");
            if (userId != null) return userId;
        }
        response.sendRedirect(request.getContextPath() + "/auth/login");
        return null;
    }
}
