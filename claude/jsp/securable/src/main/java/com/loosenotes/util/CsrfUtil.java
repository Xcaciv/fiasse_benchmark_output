package com.loosenotes.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * SSEM: CSRF Protection — synchroniser-token pattern.
 * A 256-bit random token is bound to each session and validated on every
 * state-changing (non-GET) request.
 */
public class CsrfUtil {

    public static final String SESSION_KEY = "csrfToken";
    public static final String PARAM_NAME  = "csrfToken";

    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfUtil() {}

    /** Return the existing session CSRF token, creating one if absent. */
    public static String getOrCreateToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            session = request.getSession(true);
        }
        String token = (String) session.getAttribute(SESSION_KEY);
        if (token == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(SESSION_KEY, token);
        }
        return token;
    }

    /** Validate the token submitted in the request against the session token. */
    public static boolean isValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;

        String sessionToken = (String) session.getAttribute(SESSION_KEY);
        String requestToken = request.getParameter(PARAM_NAME);

        return sessionToken != null
                && requestToken != null
                && timingSafeEquals(sessionToken, requestToken);
    }

    /** Constant-time string comparison to prevent timing attacks. */
    private static boolean timingSafeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
