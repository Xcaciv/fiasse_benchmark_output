package com.loosenotes.filter;

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
import java.io.IOException;
import java.util.Set;

/**
 * Servlet filter that applies a hardened set of HTTP security response headers
 * to every response served by the application.
 *
 * <p>Headers applied unconditionally:
 * <ul>
 *   <li>{@code X-Content-Type-Options: nosniff}</li>
 *   <li>{@code X-Frame-Options: DENY}</li>
 *   <li>{@code Referrer-Policy: strict-origin-when-cross-origin}</li>
 *   <li>{@code Content-Security-Policy} (restrictive default)</li>
 *   <li>{@code Strict-Transport-Security} (HTTPS enforcement)</li>
 * </ul>
 * </p>
 *
 * <p>Additionally, {@code Cache-Control: no-store} is set for authenticated
 * application paths to prevent sensitive data from being cached by
 * intermediaries or the browser.</p>
 */
@WebFilter(filterName = "SecurityHeadersFilter", urlPatterns = "/*")
public class SecurityHeadersFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SecurityHeadersFilter.class);

    /**
     * Path prefixes that carry authenticated/sensitive content and therefore
     * require aggressive cache suppression.
     */
    private static final Set<String> AUTHENTICATED_PATH_PREFIXES = Set.of(
            "/notes", "/admin", "/profile", "/ratings"
    );

    // -------------------------------------------------------------------------
    // Header values (defined as constants to aid future policy updates)
    // -------------------------------------------------------------------------

    private static final String CSP_VALUE =
            "default-src 'self'; " +
            "script-src 'self'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data:; " +
            "frame-ancestors 'none'";

    private static final String HSTS_VALUE =
            "max-age=31536000; includeSubDomains";

    // -------------------------------------------------------------------------
    // Filter lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("SecurityHeadersFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  httpRequest  = (HttpServletRequest)  request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        applySecurityHeaders(httpRequest, httpResponse);

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        log.debug("SecurityHeadersFilter destroyed");
    }

    // -------------------------------------------------------------------------
    // Header application logic
    // -------------------------------------------------------------------------

    private void applySecurityHeaders(HttpServletRequest req, HttpServletResponse res) {

        // Prevent MIME-type sniffing attacks.
        res.setHeader("X-Content-Type-Options", "nosniff");

        // Prevent clickjacking via iframe embedding.
        res.setHeader("X-Frame-Options", "DENY");

        // Control the Referer header on cross-origin navigation.
        res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Restrict resource loading to first-party origins.
        res.setHeader("Content-Security-Policy", CSP_VALUE);

        // Enforce HTTPS for one year (only meaningful over TLS, harmless over HTTP).
        res.setHeader("Strict-Transport-Security", HSTS_VALUE);

        // Suppress caching for authenticated paths to prevent sensitive data
        // from persisting in browser caches or shared proxies.
        String path = req.getServletPath();
        if (path != null && isAuthenticatedPath(path)) {
            res.setHeader("Cache-Control", "no-store");
        }

        // Enforce SameSite=Lax on the session cookie via an additional
        // Set-Cookie header attribute. Tomcat < 10 does not natively support
        // the SameSite attribute on session cookies, so we patch it here.
        String sessionCookieName = req.getSession(false) != null
                ? "JSESSIONID"
                : null;

        if (sessionCookieName != null) {
            // Only emit the SameSite modifier if the container has already set
            // a JSESSIONID cookie in this response.
            String existingSetCookie = res.getHeader("Set-Cookie");
            if (existingSetCookie != null && existingSetCookie.contains(sessionCookieName)) {
                // Append SameSite if not already present.
                if (!existingSetCookie.contains("SameSite")) {
                    res.addHeader("Set-Cookie", existingSetCookie + "; SameSite=Lax");
                }
            }
        }
    }

    /**
     * Return {@code true} if the servlet path begins with one of the
     * authenticated-content path prefixes.
     */
    private boolean isAuthenticatedPath(String path) {
        for (String prefix : AUTHENTICATED_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
