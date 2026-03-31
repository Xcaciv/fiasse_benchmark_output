package com.loosenotes.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * CSRF token management utilities.
 *
 * SSEM / ASVS alignment:
 * - ASVS V4.3 (CSRF): synchronizer-token pattern.
 * - Integrity: token is bound to the user's session; cross-origin requests lack it.
 * - Authenticity: token generated with SecureTokenUtil (256-bit entropy).
 * - Analyzability: two focused methods – get/validate – no hidden state.
 */
public final class CsrfUtil {

    /** Session attribute key for the CSRF token. */
    public static final String SESSION_ATTR = "csrfToken";
    /** HTML hidden-field name expected on forms. */
    public static final String PARAM_NAME  = "_csrf";

    private CsrfUtil() {}

    /**
     * Returns the CSRF token for the current session, generating one if absent.
     * Safe to call on every GET render.
     *
     * @param session the current HTTP session (must not be null)
     * @return the CSRF token string
     */
    public static String getOrCreate(HttpSession session) {
        String token = (String) session.getAttribute(SESSION_ATTR);
        if (token == null || token.isBlank()) {
            token = SecureTokenUtil.generateCsrfToken();
            session.setAttribute(SESSION_ATTR, token);
        }
        return token;
    }

    /**
     * Validates the CSRF token submitted with a POST/PUT/DELETE request
     * against the token stored in the session.
     * Uses MessageDigest.isEqual via String.equals for constant-time comparison
     * on equal-length inputs.
     *
     * @param request the incoming HTTP request
     * @return true if the submitted token matches the session token
     */
    public static boolean isValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        String sessionToken  = (String) session.getAttribute(SESSION_ATTR);
        String submittedToken = request.getParameter(PARAM_NAME);
        if (sessionToken == null || submittedToken == null) {
            return false;
        }
        // Constant-time comparison (both are same-length hex strings)
        return MessageDigestUtil.constantTimeEquals(sessionToken, submittedToken);
    }
}
