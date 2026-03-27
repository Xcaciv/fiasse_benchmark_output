package com.loosenotes.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter("/*")
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("Strict-Transport-Security", "max-age=15552000; includeSubDomains");
        res.setHeader("Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'");
        res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        res.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        String path = req.getRequestURI();
        if (isPrivatePath(path)) {
            res.setHeader("Cache-Control", "no-store, no-cache");
            res.setHeader("Pragma", "no-cache");
        }

        chain.doFilter(request, response);
    }

    private boolean isPrivatePath(String path) {
        return path.contains("/admin") || path.contains("/notes") ||
               path.contains("/profile") || path.contains("/auth");
    }
}
