package com.loosenotes.filter;

import com.loosenotes.model.AuditEvent;
import com.loosenotes.util.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Instant;

/**
 * Admin authorization enforcement filter.
 *
 * <p>Validates that the requesting user has the {@code ADMIN} role before
 * permitting access to any path under {@code /admin/*}. Unauthorized access
 * attempts are responded to with HTTP 403 rather than a redirect to prevent
 * information leakage about the existence or structure of the admin panel.</p>
 *
 * <p>The following logic is applied in order:
 * <ol>
 *   <li>If the session has no authenticated {@code userId}, redirect to
 *       {@code /auth/login} (the user is not logged in at all).</li>
 *   <li>If the session has a {@code userId} but the {@code userRole} is not
 *       {@code "ADMIN"}, return HTTP 403 without any redirect.</li>
 *   <li>In both unauthorized cases, emit an audit log event for
 *       security monitoring.</li>
 * </ol>
 * </p>
 */
@WebFilter(filterName = "AdminFilter", urlPatterns = "/admin/*")
public class AdminFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AdminFilter.class);

    private static final String SESSION_USER_ID   = "userId";
    private static final String SESSION_USER_ROLE = "userRole";
    private static final String SESSION_USERNAME  = "username";

    private static final String ADMIN_ROLE  = "ADMIN";
    private static final String LOGIN_PATH  = "/auth/login";
    private static final String ADMIN_RESOURCE_TYPE = "ADMIN_PANEL";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("AdminFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  httpRequest  = (HttpServletRequest)  request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        HttpSession session = httpRequest.getSession(false);
        Long   userId   = (session != null) ? (Long)   session.getAttribute(SESSION_USER_ID)   : null;
        String userRole = (session != null) ? (String) session.getAttribute(SESSION_USER_ROLE) : null;
        String username = (session != null) ? (String) session.getAttribute(SESSION_USERNAME)  : null;

        // Case 1: Not authenticated at all — redirect to login.
        if (userId == null) {
            logUnauthorizedAccess(httpRequest, null, null, "UNAUTHENTICATED");
            String loginUrl = httpRequest.getContextPath() + LOGIN_PATH;
            httpResponse.sendRedirect(loginUrl);
            return;
        }

        // Case 2: Authenticated but not an admin — return 403, no redirect.
        if (!ADMIN_ROLE.equals(userRole)) {
            logUnauthorizedAccess(httpRequest, userId, username, "INSUFFICIENT_PRIVILEGE");
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            // Return no body to avoid leaking information about the admin panel.
            return;
        }

        // Authorized admin — proceed.
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        log.debug("AdminFilter destroyed");
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void logUnauthorizedAccess(HttpServletRequest request,
                                       Long userId,
                                       String username,
                                       String reason) {

        String path = request.getServletPath();
        String ip   = getClientIp(request);

        // Application-level warning (not the audit log — that is separate).
        log.warn("Unauthorized admin access attempt: userId={} path={} reason={} ip={}",
                userId, path, reason, ip);

        // Structured audit record for security monitoring and compliance.
        AuditEvent event = new AuditEvent();
        event.setTimestamp(Instant.now());
        event.setEventType("ADMIN_ACCESS_DENIED");
        event.setActorId(userId);
        event.setActorUsername(username);
        event.setIpAddress(ip);
        event.setResourceType(ADMIN_RESOURCE_TYPE);
        event.setResourceId(null);
        event.setOutcome("DENIED");
        event.setDetail("reason=" + reason + " path=" + path);

        AuditLogger.log(event);
    }

    /**
     * Extract the client IP address, respecting the {@code X-Forwarded-For}
     * header when the application is deployed behind a trusted proxy.
     *
     * <p>Only the first address in the header is used; the full chain is not
     * trusted because intermediate hops may be forged by an attacker.</p>
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            // Use only the first (leftmost) address — the client's reported IP.
            String firstIp = forwarded.split(",")[0].trim();
            if (!firstIp.isEmpty()) {
                return firstIp;
            }
        }
        return request.getRemoteAddr();
    }
}
