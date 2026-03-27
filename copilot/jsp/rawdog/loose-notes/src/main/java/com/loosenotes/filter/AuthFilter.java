package com.loosenotes.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String contextPath = req.getContextPath();
        String uri         = req.getRequestURI();
        String relative    = uri.substring(contextPath.length());

        // Public paths – no authentication required
        if (relative.isEmpty() || relative.equals("/")
                || relative.startsWith("/login")
                || relative.startsWith("/register")
                || relative.startsWith("/shared/")
                || relative.startsWith("/password-reset")
                || relative.endsWith(".css")
                || relative.endsWith(".js")
                || relative.endsWith(".png")
                || relative.endsWith(".jpg")
                || relative.endsWith(".jpeg")
                || relative.endsWith(".gif")
                || relative.endsWith(".ico")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            resp.sendRedirect(contextPath + "/login");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
