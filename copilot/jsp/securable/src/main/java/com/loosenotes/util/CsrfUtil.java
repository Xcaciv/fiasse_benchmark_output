package com.loosenotes.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * CSRF token generation and validation.
 *
 * FIASSE Authenticity: tokens are 256-bit SecureRandom values stored in the session.
 * Validation uses MessageDigest.isEqual for constant-time comparison (prevents timing attacks).
 */
public final class CsrfUtil {

    private static final String SESSION_ATTR = "csrfToken";
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CsrfUtil() {}

    /**
     * Generates a new CSRF token, stores it in the session, and returns it.
     * Call this on login or whenever a new token is needed.
     */
    public static String generateToken(HttpSession session) {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        String token = bytesToHex(bytes);
        session.setAttribute(SESSION_ATTR, token);
        return token;
    }

    /**
     * Validates the submitted token against the session-stored token.
     * Uses constant-time byte comparison to prevent timing-based extraction.
     */
    public static boolean validateToken(HttpSession session, String submittedToken) {
        if (session == null || submittedToken == null) {
            return false;
        }
        String storedToken = (String) session.getAttribute(SESSION_ATTR);
        if (storedToken == null) {
            return false;
        }
        return MessageDigest.isEqual(
            storedToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            submittedToken.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }

    /**
     * Returns the existing token from the session, or creates a new one.
     */
    public static String getTokenFromSession(HttpSession session) {
        String existing = (String) session.getAttribute(SESSION_ATTR);
        if (existing != null) {
            return existing;
        }
        return generateToken(session);
    }

    /** Alias for getTokenFromSession — injects token for use in request attributes. */
    public static String injectToken(HttpSession session, HttpServletRequest request) {
        String token = getTokenFromSession(session);
        request.setAttribute(SESSION_ATTR, token);
        return token;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
