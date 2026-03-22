package com.loosenotes.filter;

import com.loosenotes.util.CsrfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * CSRF token validation on state-changing requests (Authenticity).
 * Trust boundary: every POST/PUT/DELETE must carry a valid session-bound token.
 */
public final class CsrfFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(CsrfFilter.class);

    private static final Set<String> EXEMPT_PATHS = new HashSet<>(
        Arrays.asList("/register", "/login")
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if (isStateChangingMethod(req.getMethod()) && !isExempt(req)) {
            HttpSession session = req.getSession(false);
            String submitted = req.getParameter(CsrfUtil.FORM_FIELD);

            if (!CsrfUtil.isValid(session, submitted)) {
                log.warn("CSRF token mismatch: uri={} ip={}",
                        req.getRequestURI(), req.getRemoteAddr());
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isStateChangingMethod(String method) {
        return "POST".equalsIgnoreCase(method)
            || "PUT".equalsIgnoreCase(method)
            || "DELETE".equalsIgnoreCase(method);
    }

    private boolean isExempt(HttpServletRequest req) {
        String path = req.getServletPath();
        if (path == null) return false;
        // Share view is unauthenticated, no CSRF needed
        if (path.startsWith("/share/")) return true;
        return EXEMPT_PATHS.contains(path);
    }

    @Override public void init(FilterConfig fc) {}
    @Override public void destroy() {}
}
