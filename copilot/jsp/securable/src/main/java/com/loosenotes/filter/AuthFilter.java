package com.loosenotes.filter;

import com.loosenotes.model.User;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Authentication filter protecting all user-facing routes.
 * Redirects unauthenticated requests to /login with the original URL preserved.
 * Also sets no-cache headers to prevent sensitive pages being stored in browser cache.
 */
@WebFilter(urlPatterns = {"/notes/*", "/profile", "/search", "/toprated",
                           "/ratings/*", "/attachments/*"})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Prevent caching of authenticated content
        res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        res.setHeader("Pragma", "no-cache");
        res.setDateHeader("Expires", 0);

        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            // Preserve original URL for post-login redirect
            String originalUrl = req.getRequestURI();
            String query = req.getQueryString();
            if (query != null) {
                originalUrl += "?" + query;
            }
            res.sendRedirect(req.getContextPath() + "/login?next=" +
                             java.net.URLEncoder.encode(originalUrl, "UTF-8"));
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig config) {}

    @Override
    public void destroy() {}
}
