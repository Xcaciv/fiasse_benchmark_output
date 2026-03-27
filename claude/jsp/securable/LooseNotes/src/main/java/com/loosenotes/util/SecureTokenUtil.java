package com.loosenotes.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Cryptographically secure token generation and hashing.
 * SSEM: Authenticity - tokens for share links and password reset.
 * SSEM: Confidentiality - tokens are hashed before storage (like passwords).
 */
public final class SecureTokenUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private SecureTokenUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Generates a cryptographically random URL-safe token string.
     * The raw token is returned to the caller (e.g., for inclusion in a URL).
     * Only the hash should be stored.
     *
     * @return Base64URL-encoded random token (43 chars)
     */
    public static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Hashes a token with SHA-256 for storage.
     * The hash is stored in the database; the raw token goes in the URL.
     * This prevents token disclosure via DB read access.
     *
     * @param rawToken plaintext token
     * @return hex-encoded SHA-256 hash
     */
    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes());
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by JVM spec
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    /**
     * Timing-safe comparison of a raw token against a stored hash.
     *
     * @param rawToken    plaintext token from URL
     * @param storedHash  SHA-256 hash from database
     * @return true if the raw token matches the stored hash
     */
    public static boolean verify(String rawToken, String storedHash) {
        if (rawToken == null || storedHash == null) {
            return false;
        }
        String computed = hashToken(rawToken);
        return MessageDigest.isEqual(computed.getBytes(), storedHash.getBytes());
    }
}
