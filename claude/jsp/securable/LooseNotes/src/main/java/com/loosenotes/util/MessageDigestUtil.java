package com.loosenotes.util;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * Constant-time comparison helpers to prevent timing-side-channel attacks.
 *
 * SSEM: Integrity, Authenticity – used by CsrfUtil and token validation.
 */
public final class MessageDigestUtil {

    private MessageDigestUtil() {}

    /**
     * Compares two strings in constant time.
     * Returns false immediately only when lengths differ – length itself is
     * not a secret for CSRF tokens (fixed 32-char hex).
     *
     * @param a first string
     * @param b second string
     * @return true if strings are identical
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
