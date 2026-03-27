package com.loosenotes.filter;

import com.loosenotes.model.User;
import com.loosenotes.util.AuditLogger;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Authorization filter restricting /admin/* to ADMIN role users only.
 * Returns HTTP 403 for authenticated non-admin users.
 * FIASSE Accountability: access denials are logged.
 */
@WebFilter(urlPatterns = {"/admin/*"})
public class AdminFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        res.setHeader("Pragma", "no-cache");
        res.setDateHeader("Expires", 0);

        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        if (!User.Role.ADMIN.equals(currentUser.getRole())) {
            AuditLogger.logSecurityEvent("ACCESS_DENIED_ADMIN",
                req.getRemoteAddr(),
                "userId=" + currentUser.getId() + " path=" + req.getRequestURI());
            res.sendError(HttpServletResponse.SC_FORBIDDEN,
                          "You do not have permission to access this resource.");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig config) {}

    @Override
    public void destroy() {}
}
