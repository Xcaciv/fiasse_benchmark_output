package com.loosenotes.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * SSEM: Authentication — ensures every protected resource requires a valid
 * authenticated session.  Unauthenticated requests are redirected to /login.
 */
public class AuthenticationFilter implements Filter {

    /** Paths accessible without authentication. */
    private static final Set<String> PUBLIC_PATHS = new HashSet<>(Arrays.asList(
            "/login",
            "/register",
            "/password-reset",
            "/error.jsp"
    ));

    /** Path prefix that is always public (share links, static assets). */
    private static final String[] PUBLIC_PREFIXES = {
            "/share/",
            "/static/"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse res  = (HttpServletResponse) response;

        String path = req.getServletPath();

        // Allow public paths and prefixes
        if (PUBLIC_PATHS.contains(path) || isPublicPrefix(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Allow root/index
        if ("/".equals(path) || "/index.jsp".equals(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        boolean authenticated = session != null && session.getAttribute("userId") != null;

        if (!authenticated) {
            res.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicPrefix(String path) {
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    @Override public void init(FilterConfig fc) {}
    @Override public void destroy() {}
}
