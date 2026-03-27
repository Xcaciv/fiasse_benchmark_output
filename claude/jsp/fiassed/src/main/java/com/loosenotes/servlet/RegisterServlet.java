package com.loosenotes.servlet;

import com.loosenotes.dao.AuditLogDao;
import com.loosenotes.dao.UserDao;
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
 * Handles new-account registration at {@code /auth/register}.
 *
 * <p><strong>Anti-enumeration</strong>: duplicate username/email failures are
 * reported with the same generic "Registration failed" message as validation
 * failures so callers cannot confirm whether an email address is already
 * registered.
 */
@WebServlet("/auth/register")
public class RegisterServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(RegisterServlet.class);
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
    // GET /auth/register — render registration form
    // -------------------------------------------------------------------------

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        MDC.put("correlationId", UUID.randomUUID().toString());
        try {
            HttpSession session = request.getSession(true);
            request.setAttribute("csrfToken", CsrfUtil.getOrCreateToken(session));
            request.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }

    // -------------------------------------------------------------------------
    // POST /auth/register — process registration
    // -------------------------------------------------------------------------

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        MDC.put("correlationId", UUID.randomUUID().toString());
        try {
            HttpSession session = request.getSession(true);

            // CSRF validation.
            if (!CsrfUtil.validateToken(session, request.getParameter("_csrf"))) {
                log.warn("CSRF token mismatch on registration. ip={}", request.getRemoteAddr());
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
                return;
            }

            String username = request.getParameter("username");
            String email    = request.getParameter("email");
            String password = request.getParameter("password");
            String ipAddress = request.getRemoteAddr();

            if (username == null) username = "";
            if (email    == null) email    = "";
            if (password == null) password = "";

            try {
                userService.register(username.trim(), email.trim(), password, ipAddress);
                CsrfUtil.rotateToken(session);
                response.sendRedirect(request.getContextPath() + "/auth/login?registered=true");

            } catch (ServiceException e) {
                // All failure codes — VALIDATION and DUPLICATE — receive the same
                // user-facing message to prevent email/username enumeration.
                log.info("Registration failed. code={} ip={}", e.getCode(), ipAddress);
                request.setAttribute("csrfToken", CsrfUtil.getOrCreateToken(session));
                request.setAttribute("error", "Registration failed. Please check your input and try again.");
                request.getRequestDispatcher("/WEB-INF/jsp/auth/register.jsp").forward(request, response);
            }

        } finally {
            MDC.remove("correlationId");
        }
    }
}
