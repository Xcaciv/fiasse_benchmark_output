package com.loosenotes.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    private static final int BCRYPT_ROUNDS = 12;

    /**
     * Hash a plaintext password using BCrypt.
     *
     * @param plaintext the plaintext password
     * @return the BCrypt hash
     */
    public static String hashPassword(String plaintext) {
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * Verify a plaintext password against a stored BCrypt hash.
     *
     * @param plaintext the plaintext password to check
     * @param hash      the stored BCrypt hash
     * @return true if the password matches the hash
     */
    public static boolean verifyPassword(String plaintext, String hash) {
        if (plaintext == null || hash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plaintext, hash);
        } catch (Exception e) {
            return false;
        }
    }
}
