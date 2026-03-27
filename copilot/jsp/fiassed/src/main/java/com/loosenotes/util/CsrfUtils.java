package com.loosenotes.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.UUID;

public final class CsrfUtils {
    public static final String CSRF_TOKEN_ATTR = "csrfToken";
    public static final String CSRF_PARAM = "_csrf";

    private CsrfUtils() {}

    public static String getOrCreateToken(HttpSession session) {
        String token = (String) session.getAttribute(CSRF_TOKEN_ATTR);
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute(CSRF_TOKEN_ATTR, token);
        }
        return token;
    }

    public static boolean isValidToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        String sessionToken = (String) session.getAttribute(CSRF_TOKEN_ATTR);
        String requestToken = request.getParameter(CSRF_PARAM);
        if (sessionToken == null || requestToken == null) return false;
        return SecurityUtils.constantTimeEquals(sessionToken, requestToken);
    }
}
