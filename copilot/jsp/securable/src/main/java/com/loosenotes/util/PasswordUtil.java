package com.loosenotes.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Password hashing and verification using BCrypt (Confidentiality).
 * Work factor 12 balances security and performance.
 */
public final class PasswordUtil {

    private static final int BCRYPT_WORK_FACTOR = 12;

    private PasswordUtil() {}

    /** Hashes a plaintext password; caller must not log the result. */
    public static String hash(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(BCRYPT_WORK_FACTOR));
    }

    /**
     * Constant-time comparison to prevent timing attacks (Integrity).
     * Returns false if either argument is null rather than throwing.
     */
    public static boolean verify(String plaintext, String hash) {
        if (plaintext == null || hash == null || hash.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plaintext, hash);
        } catch (IllegalArgumentException e) {
            // Malformed hash stored; treat as mismatch
            return false;
        }
    }
}
