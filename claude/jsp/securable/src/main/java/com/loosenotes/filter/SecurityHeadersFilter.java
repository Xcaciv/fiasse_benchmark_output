package com.loosenotes.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * SSEM: Secure Communication — adds defence-in-depth HTTP security headers
 * on every response to mitigate XSS, clickjacking, MIME-sniffing, and
 * information-disclosure attacks.
 */
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletResponse res = (HttpServletResponse) response;

        // Prevent MIME-type sniffing
        res.setHeader("X-Content-Type-Options", "nosniff");

        // Block framing (clickjacking)
        res.setHeader("X-Frame-Options", "DENY");

        // Legacy XSS filter (belt-and-suspenders for older browsers)
        res.setHeader("X-XSS-Protection", "1; mode=block");

        // Content-Security-Policy — allow Bootstrap from CDN, block inline scripts
        res.setHeader("Content-Security-Policy",
            "default-src 'self'; " +
            "script-src 'self' https://cdn.jsdelivr.net; " +
            "style-src 'self' https://cdn.jsdelivr.net; " +
            "img-src 'self' data:; " +
            "font-src 'self' https://cdn.jsdelivr.net; " +
            "frame-ancestors 'none'; " +
            "object-src 'none';"
        );

        // Do not send Referer to cross-origin pages
        res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Prevent sensitive pages from being cached
        res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        res.setHeader("Pragma", "no-cache");

        chain.doFilter(request, response);
    }

    @Override public void init(FilterConfig fc) {}
    @Override public void destroy() {}
}
