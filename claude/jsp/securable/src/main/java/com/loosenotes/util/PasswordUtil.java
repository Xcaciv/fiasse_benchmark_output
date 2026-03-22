package com.loosenotes.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * SSEM: Cryptography — BCrypt with work-factor 12 for all password storage.
 * Passwords are NEVER logged or stored in plaintext.
 */
public class PasswordUtil {

    /** BCrypt cost factor (2^12 = 4096 iterations). */
    private static final int BCRYPT_ROUNDS = 12;

    private PasswordUtil() {}

    /** Hash a plaintext password. */
    public static String hash(String plaintext) {
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /** Verify a plaintext password against a stored hash. */
    public static boolean verify(String plaintext, String hash) {
        if (plaintext == null || hash == null) return false;
        try {
            return BCrypt.checkpw(plaintext, hash);
        } catch (Exception e) {
            // Malformed hash — treat as mismatch, do not propagate details
            return false;
        }
    }

    /**
     * Enforce minimum password requirements.
     * Returns null on success, or an error message on failure.
     */
    public static String validateStrength(String password) {
        if (password == null || password.length() < 8) {
            return "Password must be at least 8 characters.";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter.";
        }
        if (!password.matches(".*[0-9].*")) {
            return "Password must contain at least one digit.";
        }
        return null;
    }
}
