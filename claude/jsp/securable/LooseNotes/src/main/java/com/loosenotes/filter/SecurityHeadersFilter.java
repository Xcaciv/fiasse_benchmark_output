package com.loosenotes.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Adds security HTTP headers to every response.
 * SSEM: Integrity - prevents MIME sniffing, clickjacking.
 * SSEM: Confidentiality - limits information leakage via headers.
 * SSEM: Availability - cache-control for sensitive pages.
 */
@WebFilter("/*")
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletResponse res = (HttpServletResponse) response;

        // Prevent MIME type sniffing
        res.setHeader("X-Content-Type-Options", "nosniff");

        // Prevent clickjacking
        res.setHeader("X-Frame-Options", "DENY");

        // Enable XSS filter in older browsers
        res.setHeader("X-XSS-Protection", "1; mode=block");

        // Strict referrer policy
        res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions policy - restrict browser features
        res.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        // Content Security Policy
        res.setHeader("Content-Security-Policy",
            "default-src 'self'; "
            + "script-src 'self' 'unsafe-inline'; "
            + "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; "
            + "font-src 'self' https://cdn.jsdelivr.net; "
            + "img-src 'self' data:; "
            + "object-src 'none'; "
            + "base-uri 'self'; "
            + "form-action 'self'");

        // No caching for dynamic content
        res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        res.setHeader("Pragma", "no-cache");

        chain.doFilter(request, response);
    }
}
