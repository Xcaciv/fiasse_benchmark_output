package com.loosenotes.util;

import org.mindrot.jbcrypt.BCrypt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class SecurityUtils {
    private static final int BCRYPT_COST = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SecurityUtils() {}

    public static String hashPassword(String plaintext) {
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(BCRYPT_COST));
    }

    public static boolean verifyPassword(String plaintext, String hash) {
        try {
            return BCrypt.checkpw(plaintext, hash);
        } catch (Exception e) {
            return false;
        }
    }

    public static String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] aBytes = a.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }
}
