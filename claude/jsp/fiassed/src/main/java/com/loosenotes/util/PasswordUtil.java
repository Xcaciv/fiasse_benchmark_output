package com.loosenotes.util;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BCrypt password hashing utility.
 *
 * <p>Uses a work factor of 12 to provide a strong resistance to brute-force
 * attacks while remaining practical on commodity hardware. Passwords and their
 * hashes are <strong>never</strong> written to any log or audit trail.</p>
 *
 * <p>Callers must treat the return value of {@link #hash(String)} as opaque;
 * never attempt to parse or compare it outside of {@link #verify(String, String)}.</p>
 */
public final class PasswordUtil {

    private static final Logger log = LoggerFactory.getLogger(PasswordUtil.class);

    /**
     * BCrypt cost factor. 12 rounds ≈ 300 ms on a modern CPU which is a
     * pragmatic balance between security and user-facing latency.
     */
    private static final int BCRYPT_COST = 12;

    private PasswordUtil() {
        // utility class — no instantiation
    }

    /**
     * Hash a plaintext password using BCrypt.
     *
     * @param plaintext the user-supplied password; must not be null or blank
     * @return BCrypt hash string (60 characters, includes algorithm, cost, and
     *         salt as a single encoded value)
     * @throws IllegalArgumentException if {@code plaintext} is null or empty
     */
    public static String hash(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Password plaintext must not be null or empty");
        }
        // Intentionally no logging of the plaintext or resulting hash value.
        String hashed = BCrypt.hashpw(plaintext, BCrypt.gensalt(BCRYPT_COST));
        log.debug("Password hashed successfully (cost={})", BCRYPT_COST);
        return hashed;
    }

    /**
     * Verify a plaintext password against a previously stored BCrypt hash.
     *
     * <p>This method is safe to call even when {@code hash} is null or
     * malformed; it will return {@code false} without throwing an exception,
     * preventing information leakage about the existence of a stored credential.</p>
     *
     * @param plaintext the candidate password supplied by the user
     * @param hash      the stored BCrypt hash to compare against
     * @return {@code true} if the plaintext matches the hash; {@code false}
     *         otherwise (including when either argument is null)
     */
    public static boolean verify(String plaintext, String hash) {
        if (plaintext == null || hash == null || hash.isEmpty()) {
            // Return false rather than throwing — prevents timing side-channels
            // from distinguishing "no account" vs "wrong password".
            log.debug("Password verification skipped: null or empty argument");
            return false;
        }
        try {
            boolean result = BCrypt.checkpw(plaintext, hash);
            // Log only the boolean outcome, never the password or hash value.
            log.debug("Password verification completed: match={}", result);
            return result;
        } catch (IllegalArgumentException e) {
            // BCrypt throws if the hash string is not a valid BCrypt value.
            // Treat this as a non-match — do not propagate the exception.
            log.warn("BCrypt verification failed due to invalid hash format: {}", e.getMessage());
            return false;
        }
    }
}
