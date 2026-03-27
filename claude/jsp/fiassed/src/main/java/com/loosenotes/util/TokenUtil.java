package com.loosenotes.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Cryptographic utility for secure token generation and hashing.
 *
 * <p>All tokens are generated using {@link SecureRandom} and returned as
 * lowercase hexadecimal strings. Hashing uses SHA-256 via the JCA standard
 * library — no raw bytes are ever exposed to callers.</p>
 */
public final class TokenUtil {

    private static final Logger log = LoggerFactory.getLogger(TokenUtil.class);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String HEX_CHARS = "0123456789abcdef";

    private TokenUtil() {
        // utility class — no instantiation
    }

    /**
     * Generate a cryptographically secure random token.
     *
     * @param bytesOfEntropy number of random bytes to use (e.g. 16 = 128 bits)
     * @return lowercase hexadecimal string of length {@code bytesOfEntropy * 2}
     * @throws IllegalArgumentException if {@code bytesOfEntropy} is less than 1
     */
    public static String generateSecureToken(int bytesOfEntropy) {
        if (bytesOfEntropy < 1) {
            throw new IllegalArgumentException("bytesOfEntropy must be >= 1");
        }

        byte[] bytes = new byte[bytesOfEntropy];
        SECURE_RANDOM.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    /**
     * Compute the SHA-256 digest of a token string and return it as a lowercase
     * hexadecimal string. Useful for storing token representations in the
     * database without retaining the cleartext value.
     *
     * @param token the raw token to hash; must not be null
     * @return 64-character lowercase hex SHA-256 digest
     * @throws IllegalArgumentException if {@code token} is null
     * @throws RuntimeException         if SHA-256 is unavailable (should never
     *                                  happen on a compliant JVM)
     */
    public static String hashToken(String token) {
        if (token == null) {
            throw new IllegalArgumentException("token must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required by the Java SE specification; this path is unreachable
            // on any compliant JVM, but we must handle the checked exception.
            log.error("SHA-256 algorithm unavailable — JVM may be non-compliant", e);
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }

    /**
     * Convenience factory: generate a 128-bit (16-byte) secure token returned as
     * a 32-character hexadecimal string.
     *
     * @return 32-character lowercase hexadecimal token
     */
    public static String generateToken128Bit() {
        return generateSecureToken(16);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(HEX_CHARS.charAt((b >> 4) & 0x0F));
            sb.append(HEX_CHARS.charAt(b & 0x0F));
        }
        return sb.toString();
    }
}
