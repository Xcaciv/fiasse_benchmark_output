package com.loosenotes.filter;

import com.loosenotes.model.User;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

/**
 * Authentication filter - protects all application routes except public ones.
 * SSEM: Authenticity - enforces session-based authentication.
 * SSEM: Analyzability - explicit allow-list of public paths.
 *
 * <p>Trust boundary: This filter is the primary authentication gate.
 */
@WebFilter("/*")
public class AuthenticationFilter implements Filter {

    /** Paths that do not require authentication. */
    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/auth/login",
        "/auth/register",
        "/auth/forgot-password",
        "/auth/reset-password",
        "/share"
    );

    /** Static resource prefixes that bypass the filter. */
    private static final Set<String> STATIC_PREFIXES = Set.of(
        "/css/", "/js/", "/images/", "/favicon.ico"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req  = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = getRelativePath(req);

        if (isStaticResource(path) || isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            String loginUrl = req.getContextPath() + "/auth/login";
            res.sendRedirect(loginUrl);
            return;
        }

        // Refresh user in request scope for use in servlets/JSPs
        req.setAttribute("currentUser", user);
        chain.doFilter(request, response);
    }

    private String getRelativePath(HttpServletRequest req) {
        String contextPath = req.getContextPath();
        String uri = req.getRequestURI();
        return uri.startsWith(contextPath) ? uri.substring(contextPath.length()) : uri;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean isStaticResource(String path) {
        return STATIC_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
