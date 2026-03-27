package com.loosenotes.filter;

import com.loosenotes.model.User;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);

        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            String loginUrl = req.getContextPath() + "/login?returnUrl=" +
                URLEncoder.encode(req.getRequestURI(), StandardCharsets.UTF_8);
            res.sendRedirect(loginUrl);
            return;
        }

        // Admin-only areas
        String path = req.getServletPath();
        if (path.startsWith("/admin") && !currentUser.isAdmin()) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void destroy() {}
}
