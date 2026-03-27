package com.loosenotes.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Centralized, single-responsibility password policy engine (F-01, Modifiability).
 *
 * <p>All policy thresholds are expressed as named constants so they can be
 * located and changed in one place without hunting through business logic.
 * The {@link #validate(String)} entry-point composes individual checks and
 * returns a structured {@link ValidationResult} rather than throwing, keeping
 * callers free from exception-handling boilerplate for routine policy violations.
 *
 * <p>Resilience note (GR-03): blocklist look-ups that throw unexpectedly are
 * logged at WARN and treated as "not on blocklist" (fail-open) so that an
 * infrastructure hiccup never prevents a legitimate registration.
 */
public class PasswordPolicyService {

    private static final Logger log = LoggerFactory.getLogger(PasswordPolicyService.class);

    // --- Policy constants (F-01 Modifiability: change here, nowhere else) ---

    /** Minimum acceptable password length (inclusive). */
    public static final int MIN_LENGTH = 12;

    /** Maximum acceptable password length (inclusive). Prevents DoS via bcrypt. */
    public static final int MAX_LENGTH = 128;

    /**
     * Built-in common-password blocklist.  Stored lower-cased for O(1) lookup.
     * Extend this set to increase breadth without touching policy logic.
     */
    private static final Set<String> COMMON_PASSWORD_BLOCKLIST = Set.of(
            "password123456",
            "123456789012",
            "qwertyuiop12",
            "iloveyou1234",
            "admin123456789",
            "letmein123456",
            "welcome123456",
            "monkey123456",
            "dragon123456",
            "master123456"
    );

    // -------------------------------------------------------------------------
    // Individual policy predicates
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} when {@code password} meets the minimum length.
     *
     * @param password candidate password; {@code null} returns {@code false}
     */
    public boolean meetsMinLength(String password) {
        return password != null && password.length() >= MIN_LENGTH;
    }

    /**
     * Returns {@code true} when {@code password} does not exceed the maximum
     * length.
     *
     * @param password candidate password; {@code null} returns {@code false}
     */
    public boolean meetsMaxLength(String password) {
        return password != null && password.length() <= MAX_LENGTH;
    }

    /**
     * Returns {@code true} when {@code password} appears on the common-password
     * blocklist (case-insensitive comparison).
     *
     * <p>On any unexpected exception the failure is logged at WARN level and
     * {@code true} is returned (fail-open) so that infrastructure problems
     * never silently block legitimate users.
     *
     * @param password candidate password; {@code null} returns {@code false}
     */
    public boolean isOnBlocklist(String password) {
        if (password == null) {
            return false;
        }
        try {
            return COMMON_PASSWORD_BLOCKLIST.contains(password.toLowerCase());
        } catch (Exception e) {
            log.warn("Blocklist check failed unexpectedly; treating as not blocked. error={}", e.getMessage(), e);
            return false; // fail-open: do not impede registration on infrastructure fault
        }
    }

    // -------------------------------------------------------------------------
    // Composite validation entry-point
    // -------------------------------------------------------------------------

    /**
     * Validates {@code password} against all active policy rules and returns a
     * structured {@link ValidationResult}.
     *
     * <p>Checks are applied in order: null/empty → min length → max length →
     * blocklist.  The first failing check wins so that the message is specific
     * and actionable.
     *
     * @param password candidate password (may be {@code null})
     * @return {@link ValidationResult} with {@code valid=true} on full compliance
     */
    public ValidationResult validate(String password) {
        if (password == null || password.isEmpty()) {
            return ValidationResult.failure("Password must not be empty.");
        }

        if (!meetsMinLength(password)) {
            return ValidationResult.failure(
                    "Password must be at least " + MIN_LENGTH + " characters long.");
        }

        if (!meetsMaxLength(password)) {
            return ValidationResult.failure(
                    "Password must not exceed " + MAX_LENGTH + " characters.");
        }

        if (isOnBlocklist(password)) {
            return ValidationResult.failure(
                    "Password is too common. Please choose a less predictable password.");
        }

        return ValidationResult.success();
    }

    // -------------------------------------------------------------------------
    // Inner result type
    // -------------------------------------------------------------------------

    /**
     * Value object returned by {@link #validate(String)}.
     *
     * <p>Using a result object (rather than throwing an exception for expected
     * failures) keeps the common path allocation-cheap and avoids misusing
     * exceptions for flow control.
     */
    public static final class ValidationResult {

        /** {@code true} when the password satisfies all active policy rules. */
        public final boolean valid;

        /**
         * Human-readable explanation of the policy violation, or an empty string
         * when {@code valid} is {@code true}.
         */
        public final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = (message != null) ? message : "";
        }

        static ValidationResult success() {
            return new ValidationResult(true, "");
        }

        static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }

        /** Convenience alias for {@code valid}. */
        public boolean isValid() {
            return valid;
        }

        /** Returns the policy-violation message, or empty string on success. */
        public String getMessage() {
            return message;
        }
    }
}
