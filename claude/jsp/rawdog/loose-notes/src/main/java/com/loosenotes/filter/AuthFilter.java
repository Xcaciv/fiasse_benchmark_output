package com.loosenotes.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        String contextPath = httpRequest.getContextPath();
        String requestURI = httpRequest.getRequestURI();
        String path = requestURI.substring(contextPath.length());

        // Allow public paths without authentication
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Check if user is logged in
        boolean loggedIn = session != null && session.getAttribute("userId") != null;

        if (!loggedIn) {
            httpResponse.sendRedirect(contextPath + "/login");
            return;
        }

        // Check admin-only paths
        if (isAdminPath(path)) {
            String role = (String) session.getAttribute("userRole");
            if (!"ADMIN".equals(role)) {
                httpResponse.sendRedirect(contextPath + "/dashboard");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return true;
        }
        String[] publicPaths = {
            "/login", "/register", "/share", "/top-rated", "/search",
            "/css/", "/password-reset", "/index.jsp"
        };
        for (String publicPath : publicPaths) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAdminPath(String path) {
        return path != null && path.startsWith("/admin");
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }
}
