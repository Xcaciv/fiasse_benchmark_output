package com.loosenotes.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);

        String path = req.getServletPath();
        String contextPath = req.getContextPath();

        // Paths that don't require authentication
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        boolean loggedIn = (session != null && session.getAttribute("userId") != null);

        if (!loggedIn) {
            res.sendRedirect(contextPath + "/login");
            return;
        }

        // Admin-only paths
        if (path.startsWith("/admin")) {
            Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
            if (isAdmin == null || !isAdmin) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path.equals("/login") ||
               path.equals("/register") ||
               path.equals("/forgot-password") ||
               path.equals("/reset-password") ||
               path.startsWith("/share") ||
               path.equals("/top-rated") ||
               path.equals("/search") ||
               path.startsWith("/css/") ||
               path.equals("/index.jsp") ||
               path.equals("/error.jsp");
    }

    @Override
    public void destroy() {}
}
