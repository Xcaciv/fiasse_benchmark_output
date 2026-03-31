package com.loosenotes.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Adds security-relevant HTTP response headers to every response.
 *
 * SSEM / ASVS alignment:
 * - ASVS V3.4 (HTTP Security Headers): X-Content-Type-Options, X-Frame-Options,
 *   Content-Security-Policy, Referrer-Policy.
 * - Availability: sets a restrictive CSP to reduce XSS attack surface.
 * - Analyzability: all header constants are named; no magic strings inline.
 */
public class SecurityHeadersFilter implements Filter {

    private static final String CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data:",
            "font-src 'self'",
            "frame-ancestors 'none'",
            "base-uri 'self'",
            "form-action 'self'"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpResp = (HttpServletResponse) response;

        httpResp.setHeader("X-Content-Type-Options",   "nosniff");
        httpResp.setHeader("X-Frame-Options",           "DENY");
        httpResp.setHeader("X-XSS-Protection",          "0");          // Deprecated; CSP is primary
        httpResp.setHeader("Referrer-Policy",            "strict-origin-when-cross-origin");
        httpResp.setHeader("Content-Security-Policy",   CSP);
        httpResp.setHeader("Permissions-Policy",        "geolocation=(), microphone=(), camera=()");
        // HSTS – uncomment when running behind TLS in production
        // httpResp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        chain.doFilter(request, response);
    }
}
