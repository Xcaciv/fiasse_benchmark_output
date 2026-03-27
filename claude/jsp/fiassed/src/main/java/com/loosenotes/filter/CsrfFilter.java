package com.loosenotes.filter;

import com.loosenotes.util.CsrfUtil;
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
import java.util.Set;

/**
 * CSRF protection filter (GR-06).
 *
 * <p>All state-changing HTTP methods (POST, PUT, DELETE, PATCH) must carry a
 * valid {@code _csrf} request parameter that matches the token stored in the
 * current HTTP session. Requests that fail validation are rejected with HTTP
 * 400 and the filter chain is not invoked.</p>
 *
 * <p>The following paths are exempt from CSRF validation because they are
 * unauthenticated entry-points or read-only endpoints where a session token
 * cannot be guaranteed to exist:</p>
 * <ul>
 *   <li>{@code /auth/login}</li>
 *   <li>{@code /auth/register}</li>
 *   <li>{@code /auth/forgot-password}</li>
 *   <li>{@code /auth/reset-password}</li>
 *   <li>{@code /share/} (token-based public share links)</li>
 *   <li>{@code /top-rated}</li>
 *   <li>{@code /search}</li>
 * </ul>
 */
@WebFilter(filterName = "CsrfFilter", urlPatterns = "/*")
public class CsrfFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(CsrfFilter.class);

    /** Request parameter name that carries the CSRF token. */
    private static final String CSRF_PARAM = "_csrf";

    /**
     * HTTP methods that do not alter server state and are therefore exempt from
     * CSRF validation per RFC 7231.
     */
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    /**
     * Path prefixes (and exact paths) that are exempt from CSRF validation.
     * These paths are either unauthenticated or publicly accessible.
     */
    private static final Set<String> EXEMPT_PATH_PREFIXES = Set.of(
            "/auth/login",
            "/auth/register",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/share/",
            "/top-rated",
            "/search"
    );

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("CsrfFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  httpRequest  = (HttpServletRequest)  request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod().toUpperCase();
        String path   = httpRequest.getServletPath();

        // Safe HTTP methods are not subject to CSRF validation.
        if (SAFE_METHODS.contains(method)) {
            chain.doFilter(request, response);
            return;
        }

        // Exempt paths bypass CSRF validation.
        if (isExemptPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // For all other state-changing requests, validate the token.
        HttpSession session = httpRequest.getSession(false);
        String submittedToken = httpRequest.getParameter(CSRF_PARAM);

        if (!CsrfUtil.validateToken(session, submittedToken)) {
            log.warn("CSRF validation failed: method={} path={} remoteAddr={}",
                    method, path, httpRequest.getRemoteAddr());

            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            httpResponse.setContentType("text/plain;charset=UTF-8");
            httpResponse.getWriter().write("Invalid CSRF token");
            // Do NOT continue the filter chain.
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        log.debug("CsrfFilter destroyed");
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Return {@code true} if the request path matches any of the exempt prefixes.
     * Comparison is case-sensitive to avoid bypass through case manipulation.
     */
    private boolean isExemptPath(String path) {
        if (path == null) {
            return false;
        }
        for (String prefix : EXEMPT_PATH_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
