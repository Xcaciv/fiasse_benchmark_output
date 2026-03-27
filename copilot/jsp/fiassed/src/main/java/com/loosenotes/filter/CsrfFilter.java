package com.loosenotes.filter;

import com.loosenotes.util.CsrfUtils;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter("/*")
public class CsrfFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String method = req.getMethod();
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
            // Skip CSRF for static resources
            String path = req.getRequestURI();
            if (!path.startsWith(req.getContextPath() + "/static/")) {
                HttpSession session = req.getSession(false);
                if (session != null) {
                    if (!CsrfUtils.isValidToken(req)) {
                        res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
                        return;
                    }
                }
            }
        } else if ("GET".equalsIgnoreCase(method)) {
            // Ensure CSRF token exists in session for GET requests (creates it if missing)
            HttpSession session = req.getSession(false);
            if (session != null) {
                CsrfUtils.getOrCreateToken(session);
            }
        }

        chain.doFilter(request, response);
    }
}
