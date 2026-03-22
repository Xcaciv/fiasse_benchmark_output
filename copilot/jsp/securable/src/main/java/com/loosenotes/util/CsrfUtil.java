package com.loosenotes.util;

import javax.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * CSRF token generation and validation (Authenticity).
 * Token is stored in the HTTP session and validated on every state-changing request.
 */
public final class CsrfUtil {

    public static final String SESSION_KEY = "csrf_token";
    public static final String FORM_FIELD  = "_csrf";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private CsrfUtil() {}

    /** Generates a cryptographically random token and stores it in the session. */
    public static String generateToken(HttpSession session) {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_KEY, token);
        return token;
    }

    /** Returns existing token; creates one if absent (lazy init). */
    public static String getOrCreateToken(HttpSession session) {
        String token = (String) session.getAttribute(SESSION_KEY);
        if (token == null) {
            token = generateToken(session);
        }
        return token;
    }

    /**
     * Constant-time comparison to prevent timing attacks (Integrity).
     * Returns true only when the submitted token matches the session token.
     */
    public static boolean isValid(HttpSession session, String submittedToken) {
        if (session == null || submittedToken == null) {
            return false;
        }
        String sessionToken = (String) session.getAttribute(SESSION_KEY);
        if (sessionToken == null) {
            return false;
        }
        return constantTimeEquals(sessionToken, submittedToken);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
