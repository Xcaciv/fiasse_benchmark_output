package com.loosenotes.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Sets UTF-8 encoding on every request and response (Integrity: character encoding).
 * Must be the first filter in the chain.
 */
public final class EncodingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        // Security headers on every response (Availability, Integrity)
        HttpServletResponse httpResp = (HttpServletResponse) response;
        httpResp.setHeader("X-Content-Type-Options", "nosniff");
        httpResp.setHeader("X-Frame-Options", "DENY");
        httpResp.setHeader("X-XSS-Protection", "1; mode=block");
        httpResp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        chain.doFilter(request, response);
    }

    @Override public void init(FilterConfig fc) {}
    @Override public void destroy() {}
}
