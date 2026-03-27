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
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Authentication enforcement filter.
 *
 * <p>Checks that the current HTTP session contains a {@code userId} attribute
 * (set by the login servlet upon successful authentication). If the attribute
 * is absent the request is redirected to the login page, preserving the
 * original URL as a {@code redirect} query parameter so that the user lands
 * on the intended page after authenticating.</p>
 *
 * <p>This filter should be mapped only over paths that require authentication
 * (e.g. {@code /notes/*}, {@code /profile/*}, {@code /admin/*}). Public paths
 * such as {@code /auth/*} and {@code /share/*} must not be covered.</p>
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = {
        "/notes/*",
        "/profile/*",
        "/ratings/*",
        "/attachments/*"
})
public class AuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    /** Session attribute key set by the login servlet upon successful authentication. */
    private static final String SESSION_USER_ID = "userId";

    /** Path to the login page. */
    private static final String LOGIN_PATH = "/auth/login";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("AuthFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  httpRequest  = (HttpServletRequest)  request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        HttpSession session = httpRequest.getSession(false);
        Long userId = (session != null) ? (Long) session.getAttribute(SESSION_USER_ID) : null;

        if (userId == null) {
            // Preserve the original request URL so the user can be redirected
            // back after successful login.
            String originalUrl = buildOriginalUrl(httpRequest);
            String encodedRedirect = URLEncoder.encode(originalUrl, StandardCharsets.UTF_8);
            String loginUrl = httpRequest.getContextPath() + LOGIN_PATH + "?redirect=" + encodedRedirect;

            log.debug("Unauthenticated request to {}: redirecting to login", httpRequest.getServletPath());
            httpResponse.sendRedirect(loginUrl);
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        log.debug("AuthFilter destroyed");
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Reconstruct the full request URL including query string for use as the
     * post-login redirect target. The query string is included so that deep
     * links (e.g. shared note URLs that require auth) continue to work.
     */
    private String buildOriginalUrl(HttpServletRequest request) {
        StringBuilder url = new StringBuilder(request.getRequestURI());
        String query = request.getQueryString();
        if (query != null && !query.isEmpty()) {
            url.append('?').append(query);
        }
        return url.toString();
    }
}
