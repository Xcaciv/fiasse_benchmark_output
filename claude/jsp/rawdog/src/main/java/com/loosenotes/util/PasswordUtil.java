package com.loosenotes.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    private PasswordUtil() {}

    public static String hash(String plaintext) {
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(12));
    }

    public static boolean verify(String plaintext, String hashed) {
        if (plaintext == null || hashed == null) return false;
        try {
            return BCrypt.checkpw(plaintext, hashed);
        } catch (Exception e) {
            return false;
        }
    }
}
