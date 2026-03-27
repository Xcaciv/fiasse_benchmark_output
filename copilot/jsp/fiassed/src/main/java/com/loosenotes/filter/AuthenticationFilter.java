package com.loosenotes.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

@WebFilter("/*")
public class AuthenticationFilter implements Filter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/login", "/auth/register", "/auth/forgot-password", "/auth/reset-password",
            "/share/", "/top-rated", "/static/", "/error/"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getServletPath();

        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            res.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath) || path.equals(publicPath.stripTrailing())) {
                return true;
            }
        }
        return path.equals("/") || path.equals("/index.jsp");
    }
}
