package com.loosenotes.filter;

import com.loosenotes.util.CsrfUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * SSEM: CSRF Protection — validates the synchroniser token on every
 * non-idempotent request (POST/PUT/DELETE/PATCH).
 *
 * Excluded paths (public forms): /login, /register, /password-reset
 */
public class CsrfFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(CsrfFilter.class.getName());

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse res  = (HttpServletResponse) response;

        String method = req.getMethod();

        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {

            String path = req.getServletPath();
            // Allow CSRF-exempt public forms (user is not yet authenticated)
            boolean exempt = "/login".equals(path)
                    || "/register".equals(path)
                    || "/password-reset".equals(path);

            if (!exempt && !CsrfUtil.isValid(req)) {
                LOGGER.warning("CSRF token mismatch – blocking request to " + path
                        + " from IP " + req.getRemoteAddr());
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF token.");
                return;
            }
        }

        // For every request, ensure a CSRF token exists in the session
        // so JSPs can include it in forms
        if (req.getSession(false) != null) {
            CsrfUtil.getOrCreateToken(req);
            req.setAttribute(CsrfUtil.SESSION_KEY, req.getSession().getAttribute(CsrfUtil.SESSION_KEY));
        }

        chain.doFilter(request, response);
    }

    @Override public void init(FilterConfig fc) {}
    @Override public void destroy() {}
}
