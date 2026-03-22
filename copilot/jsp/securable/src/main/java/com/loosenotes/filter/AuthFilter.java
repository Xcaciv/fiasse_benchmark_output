package com.loosenotes.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Enforces authentication on protected URL patterns (Authenticity).
 * Redirects unauthenticated users to /login.
 * Admin paths additionally require role=ADMIN.
 */
public final class AuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);
        Long userId = session != null ? (Long) session.getAttribute("userId") : null;

        if (userId == null) {
            log.debug("Unauthenticated access to {}, redirecting to /login",
                    req.getRequestURI());
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // Admin path requires ADMIN role
        if (req.getServletPath().startsWith("/admin")) {
            String role = (String) session.getAttribute("userRole");
            if (!"ADMIN".equalsIgnoreCase(role)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Admin access required");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override public void init(FilterConfig fc) {}
    @Override public void destroy() {}
}
