package com.loosenotes.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Password hashing and verification using BCrypt.
 * Work factor 12 balances security and response latency.
 * This class has no state — methods are pure functions.
 */
public final class PasswordUtil {

    private static final int WORK_FACTOR = 12;

    private PasswordUtil() {}

    /** Returns a BCrypt hash of the plaintext password. */
    public static String hashPassword(String plaintext) {
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(WORK_FACTOR));
    }

    /** Constant-time comparison via BCrypt. Returns true if plaintext matches hash. */
    public static boolean verifyPassword(String plaintext, String hash) {
        if (plaintext == null || hash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plaintext, hash);
        } catch (IllegalArgumentException e) {
            // Malformed hash — treat as mismatch, never expose hash details
            return false;
        }
    }

    /**
     * Validates password strength policy:
     * minimum 8 chars, at least 1 uppercase, 1 lowercase, 1 digit.
     */
    public static boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasUpper = false, hasLower = false, hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        return hasUpper && hasLower && hasDigit;
    }
}
