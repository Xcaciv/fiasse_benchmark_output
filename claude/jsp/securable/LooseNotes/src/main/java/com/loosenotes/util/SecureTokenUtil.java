package com.loosenotes.util;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Generates cryptographically secure random tokens.
 *
 * SSEM / ASVS alignment:
 * - ASVS V11.3 (Randomness): uses java.security.SecureRandom.
 * - Authenticity: tokens are 256-bit (32 bytes), making brute-force infeasible.
 * - Testability: pure static utility; injectable via interface in services.
 */
public final class SecureTokenUtil {

    /** One shared SecureRandom instance – thread-safe and seeded by the JVM. */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Token byte length – 256 bits = 32 bytes → 64-char hex string. */
    private static final int TOKEN_BYTES = 32;

    private SecureTokenUtil() {}

    /**
     * Generates a 256-bit URL-safe hex token suitable for share links and
     * password reset tokens.
     *
     * @return lowercase hex string of length 64
     */
    public static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * Generates a shorter 128-bit token for CSRF protection.
     * 16 bytes → 32-char hex string.
     *
     * @return lowercase hex string of length 32
     */
    public static String generateCsrfToken() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
