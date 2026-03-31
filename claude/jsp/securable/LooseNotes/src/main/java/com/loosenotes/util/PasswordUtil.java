package com.loosenotes.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Password hashing and verification utilities using BCrypt.
 *
 * SSEM / ASVS alignment:
 * - ASVS V6.2 (Algorithms): BCrypt with cost factor ≥ 10 recommended.
 * - Confidentiality: raw passwords never retained in memory beyond this call.
 * - Testability: static utility; can be tested in isolation without container.
 * - Resilience: BCrypt handles its own salt generation; no external dependency.
 */
public final class PasswordUtil {

    /**
     * BCrypt cost factor.
     * Cost 12 ≈ 250ms on modern hardware – balances security with usability.
     * Increase to 13+ as hardware improves; BCrypt is inherently upgradeable.
     */
    private static final int BCRYPT_COST = 12;

    private PasswordUtil() {
        // Utility class – no instances
    }

    /**
     * Hashes a plaintext password with BCrypt.
     *
     * @param plaintext the raw password character array (caller clears it after use)
     * @return the BCrypt hash string (includes salt and cost factor)
     */
    public static String hash(char[] plaintext) {
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, plaintext);
    }

    /**
     * Verifies a plaintext password against a stored BCrypt hash.
     * Uses constant-time comparison internally to prevent timing attacks.
     *
     * @param plaintext the raw password character array
     * @param hash      the stored BCrypt hash
     * @return true if the password matches the hash
     */
    public static boolean verify(char[] plaintext, String hash) {
        if (hash == null || hash.isBlank()) {
            return false;
        }
        BCrypt.Result result = BCrypt.verifyer().verify(plaintext, hash);
        return result.verified;
    }

    /**
     * Validates that a password meets minimum policy requirements.
     * Rule: 8–128 characters. Callers may add entropy checks.
     *
     * @param plaintext the raw password to evaluate
     * @return true if the password meets policy
     */
    public static boolean meetsPolicy(char[] plaintext) {
        if (plaintext == null) {
            return false;
        }
        int length = plaintext.length;
        return length >= 8 && length <= 128;
    }
}
