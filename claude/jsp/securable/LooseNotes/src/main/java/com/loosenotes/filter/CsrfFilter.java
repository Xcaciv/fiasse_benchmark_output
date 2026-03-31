package com.loosenotes.filter;

import com.loosenotes.util.CsrfUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

/**
 * Enforces CSRF protection on all state-changing (non-idempotent) requests.
 *
 * SSEM / ASVS alignment:
 * - ASVS V4.3 (CSRF): synchronizer token pattern.
 * - Integrity: rejects POST/PUT/DELETE without a valid session-bound token.
 * - Analyzability: skip list of methods is explicit and named.
 *
 * Excluded: GET, HEAD, OPTIONS, TRACE (safe methods per RFC 7231).
 * Also excluded: /share/view/* (public, unauthenticated view – no session required).
 */
public class CsrfFilter implements Filter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String method = req.getMethod().toUpperCase();
        if (SAFE_METHODS.contains(method)) {
            // Ensure a CSRF token exists in the session for upcoming form renders
            HttpSession session = req.getSession(false);
            if (session != null) {
                CsrfUtil.getOrCreate(session);
            }
            chain.doFilter(request, response);
            return;
        }

        // State-changing request – validate the submitted CSRF token
        if (!CsrfUtil.isValid(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
            return;
        }

        chain.doFilter(request, response);
    }
}
