package com.loosenotes.util;

import java.security.SecureRandom;

/**
 * Generates cryptographically random tokens using SecureRandom.
 * All tokens are 32 bytes (256 bits) encoded as 64 lowercase hex characters.
 */
public final class TokenUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private TokenUtil() {}

    /** Generates a 64-character hex-encoded random token for password reset links. */
    public static String generateToken() {
        return generateHexToken();
    }

    /** Generates a 64-character hex-encoded random token for note share links. */
    public static String generateShareToken() {
        return generateHexToken();
    }

    private static String generateHexToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(TOKEN_BYTES * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
