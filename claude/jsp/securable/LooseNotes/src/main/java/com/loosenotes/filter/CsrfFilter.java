package com.loosenotes.filter;

import com.loosenotes.util.CsrfUtil;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/**
 * CSRF protection filter using the Synchronizer Token Pattern.
 * SSEM: Authenticity - validates CSRF token on all state-changing requests.
 * SSEM: Integrity - prevents cross-site request forgery.
 *
 * <p>Trust boundary: All POST/PUT/DELETE requests are validated here.
 */
@WebFilter("/*")
public class CsrfFilter implements Filter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    /** Paths exempt from CSRF validation (no state changes). */
    private static final Set<String> CSRF_EXEMPT = Set.of(
        "/share/"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req  = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if (SAFE_METHODS.contains(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String path = getRelativePath(req);
        if (isCsrfExempt(path)) {
            chain.doFilter(request, response);
            return;
        }

        if (!CsrfUtil.isTokenValid(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF token");
            return;
        }

        chain.doFilter(request, response);
    }

    private String getRelativePath(HttpServletRequest req) {
        String contextPath = req.getContextPath();
        String uri = req.getRequestURI();
        return uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;
    }

    private boolean isCsrfExempt(String path) {
        return CSRF_EXEMPT.stream().anyMatch(path::startsWith);
    }
}
