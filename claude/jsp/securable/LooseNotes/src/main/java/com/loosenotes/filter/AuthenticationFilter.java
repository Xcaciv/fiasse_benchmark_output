package com.loosenotes.filter;

import com.loosenotes.model.User;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Guards protected URL patterns by verifying an authenticated session.
 * Redirects unauthenticated requests to the login page.
 * Also enforces admin-only access for /admin/* paths.
 *
 * SSEM / ASVS alignment:
 * - ASVS V4.1 (Access Control): centralized authentication gate.
 * - Authenticity: relies only on the server-side session attribute set by AuthServlet.
 * - Analyzability: single responsibility – redirect if not logged in (and admin check).
 */
public class AuthenticationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            // Preserve the originally requested URL so we can redirect back after login
            String origUrl = req.getRequestURI();
            String query   = req.getQueryString();
            String redirect = origUrl + (query != null ? "?" + query : "");
            resp.sendRedirect(req.getContextPath() + "/auth/login?redirect="
                    + java.net.URLEncoder.encode(redirect, "UTF-8"));
            return;
        }

        // Enforce admin-only access for /admin/* paths
        if (req.getServletPath().startsWith("/admin") && !currentUser.isAdmin()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin access required");
            return;
        }

        chain.doFilter(request, response);
    }
}
