package com.loosenotes.servlet;

import com.loosenotes.dao.AuditLogDao;
import com.loosenotes.dao.UserDao;
import com.loosenotes.model.User;
import com.loosenotes.service.AuditService;
import com.loosenotes.service.PasswordPolicyService;
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
 * Handles user authentication at {@code /auth/login}.
 *
 * <p><strong>Session fixation prevention</strong>: on successful login the
 * existing session is invalidated and a brand-new session is created before
 * any identity attributes are written to it.
 *
 * <p><strong>Anti-enumeration</strong>: the same generic "Invalid credentials"
 * message is returned for unknown usernames, wrong passwords, and locked
 * accounts so callers cannot infer account existence from the response.
 */
@WebServlet("/auth/login")
public class LoginServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(LoginServlet.class);
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

    // -------------------------------------------------------------------------
    // GET /auth/login — render login form
    // -------------------------------------------------------------------------

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        MDC.put("correlationId", UUID.randomUUID().toString());
        try {
            HttpSession session = request.getSession(true);
            request.setAttribute("csrfToken", CsrfUtil.getOrCreateToken(session));
            request.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }

    // -------------------------------------------------------------------------
    // POST /auth/login — process credentials
    // -------------------------------------------------------------------------

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        MDC.put("correlationId", UUID.randomUUID().toString());
        try {
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            String ipAddress = request.getRemoteAddr();

            // Null-safe blank guards before passing to service.
            if (username == null) username = "";
            if (password == null) password = "";

            User user = userService.authenticate(username.trim(), password, ipAddress);

            if (user == null) {
                // Generic message — does not reveal lockout state or account existence.
                HttpSession existingSession = request.getSession(false);
                if (existingSession != null) {
                    request.setAttribute("csrfToken", CsrfUtil.getOrCreateToken(existingSession));
                } else {
                    HttpSession newSess = request.getSession(true);
                    request.setAttribute("csrfToken", CsrfUtil.getOrCreateToken(newSess));
                }
                request.setAttribute("error", "Invalid credentials");
                request.getRequestDispatcher("/WEB-INF/jsp/auth/login.jsp").forward(request, response);
                return;
            }

            // --- Session fixation prevention ---
            // Invalidate the pre-authentication session, then create a fresh one.
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = request.getSession(true);

            // Bind identity to the new session. userId comes from the authenticated
            // User object (server-owned) — never from request parameters.
            session.setAttribute("userId", user.getId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("userRole", user.getRole() != null ? user.getRole().name() : "USER");

            log.info("Login successful. userId={} ip={}", user.getId(), ipAddress);
            response.sendRedirect(request.getContextPath() + "/notes");

        } finally {
            MDC.remove("correlationId");
        }
    }
}
