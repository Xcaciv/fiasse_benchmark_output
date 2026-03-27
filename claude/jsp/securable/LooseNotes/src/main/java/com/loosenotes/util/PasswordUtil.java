package com.loosenotes.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.util.regex.Pattern;

/**
 * Password hashing and validation utilities.
 * SSEM: Confidentiality - BCrypt with cost factor 12.
 * SSEM: Integrity - password complexity enforcement.
 * SSEM: Resilience - timing-safe comparison via BCrypt.verifyer().
 */
public final class PasswordUtil {

    /** BCrypt cost factor - increase as hardware improves. */
    private static final int BCRYPT_COST = 12;

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    /**
     * Regex: ≥8 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special char.
     */
    private static final Pattern COMPLEXITY_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{" + MIN_LENGTH + "," + MAX_LENGTH + "}$"
    );

    private PasswordUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Hashes a password with BCrypt.
     * The input char array is zeroed after hashing to minimize memory exposure.
     *
     * @param rawPassword plaintext password characters
     * @return BCrypt hash string
     */
    public static String hash(char[] rawPassword) {
        try {
            return BCrypt.withDefaults().hashToString(BCRYPT_COST, rawPassword);
        } finally {
            // SSEM: Confidentiality - clear password from memory
            java.util.Arrays.fill(rawPassword, '\0');
        }
    }

    /**
     * Hashes a password from a String (convenience overload).
     * Prefer the char[] overload when possible to reduce GC exposure.
     *
     * @param rawPassword plaintext password string
     * @return BCrypt hash string
     */
    public static String hash(String rawPassword) {
        return hash(rawPassword.toCharArray());
    }

    /**
     * Verifies a password against a BCrypt hash using timing-safe comparison.
     *
     * @param rawPassword plaintext password to verify
     * @param hash        BCrypt hash from storage
     * @return true if the password matches
     */
    public static boolean verify(String rawPassword, String hash) {
        if (rawPassword == null || hash == null) {
            return false;
        }
        BCrypt.Result result = BCrypt.verifyer().verify(rawPassword.toCharArray(), hash);
        return result.verified;
    }

    /**
     * Validates password complexity requirements.
     *
     * @param password candidate password
     * @return true if password meets complexity requirements
     */
    public static boolean meetsComplexity(String password) {
        if (password == null) {
            return false;
        }
        return COMPLEXITY_PATTERN.matcher(password).matches();
    }

    /**
     * Returns a human-readable complexity requirement description.
     */
    public static String getComplexityRequirements() {
        return "Password must be 8-128 characters and include at least one uppercase letter, "
            + "one lowercase letter, one digit, and one special character.";
    }
}
