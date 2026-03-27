package com.loosenotes.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * CSRF protection via the Synchronizer Token Pattern.
 * SSEM: Authenticity - state-changing requests require a valid session-bound token.
 * SSEM: Integrity - token is cryptographically random and tied to session.
 *
 * <p>Trust boundary: every POST, PUT, DELETE request must be validated.
 */
public final class CsrfUtil {

    public static final String SESSION_TOKEN_KEY = "_csrf_token";
    public static final String REQUEST_PARAM_KEY = "_csrf";
    public static final String REQUEST_HEADER_KEY = "X-CSRF-Token";

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CsrfUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns the CSRF token for the current session, generating one if absent.
     *
     * @param session current HTTP session (must be non-null)
     * @return Base64URL-encoded CSRF token
     */
    public static String getOrCreateToken(HttpSession session) {
        String token = (String) session.getAttribute(SESSION_TOKEN_KEY);
        if (token == null) {
            token = generateToken();
            session.setAttribute(SESSION_TOKEN_KEY, token);
        }
        return token;
    }

    /**
     * Validates the CSRF token from the request against the session token.
     * Checks both the request parameter and the X-CSRF-Token header.
     *
     * @param request current HTTP request
     * @return true if the token is valid
     */
    public static boolean isTokenValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        String sessionToken = (String) session.getAttribute(SESSION_TOKEN_KEY);
        if (sessionToken == null) {
            return false;
        }
        String requestToken = extractRequestToken(request);
        return timingSafeEquals(sessionToken, requestToken);
    }

    private static String extractRequestToken(HttpServletRequest request) {
        String paramToken = request.getParameter(REQUEST_PARAM_KEY);
        if (paramToken != null) {
            return paramToken;
        }
        return request.getHeader(REQUEST_HEADER_KEY);
    }

    /**
     * Generates a cryptographically random URL-safe Base64 token.
     */
    private static String generateToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Timing-safe string comparison to prevent timing oracle attacks.
     */
    private static boolean timingSafeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] aBytes = a.getBytes();
        byte[] bBytes = b.getBytes();
        if (aBytes.length != bBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }
}
