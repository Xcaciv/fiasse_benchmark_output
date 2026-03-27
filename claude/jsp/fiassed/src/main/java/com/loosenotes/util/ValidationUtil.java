package com.loosenotes.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Input validation and sanitization utilities.
 *
 * <p>All methods are static and stateless. Validation methods return
 * {@code boolean} and never throw; callers are responsible for translating
 * failures into user-facing messages. Sanitization methods return a safe
 * version of the input (never null).</p>
 *
 * <p>Validation rules are intentionally conservative — they describe what the
 * application <em>accepts</em>, not what it rejects, which keeps the attack
 * surface small and reduces the chance of bypass via encoding tricks.</p>
 */
public final class ValidationUtil {

    private static final Logger log = LoggerFactory.getLogger(ValidationUtil.class);

    // -------------------------------------------------------------------------
    // Compiled patterns (thread-safe after class-loading)
    // -------------------------------------------------------------------------

    /**
     * RFC 5321-aligned email pattern. Intentionally kept simple:
     * local-part@domain where domain must have at least one dot.
     * Full RFC 5321 parsing is delegated to the mail-sending layer.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    );

    /** Alphanumeric plus underscore, 3–50 characters. */
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_]{3,50}$"
    );

    /**
     * Safe filename characters: letters, digits, hyphen, underscore, dot.
     * Used in the positive-allow-list approach for filename sanitization.
     */
    private static final Pattern SAFE_FILENAME_CHAR = Pattern.compile(
            "[^a-zA-Z0-9_\\-.]"
    );

    // -------------------------------------------------------------------------
    // Length constants
    // -------------------------------------------------------------------------

    private static final int EMAIL_MAX = 255;
    private static final int PASSWORD_MIN = 12;
    private static final int PASSWORD_MAX = 128;
    private static final int TITLE_MAX = 500;
    private static final int CONTENT_MAX = 100_000;
    private static final int SEARCH_MIN = 2;
    private static final int SEARCH_MAX = 500;
    private static final int COMMENT_MAX = 1_000;
    private static final int FILENAME_MAX = 255;

    private ValidationUtil() {
        // utility class — no instantiation
    }

    // -------------------------------------------------------------------------
    // Validation methods
    // -------------------------------------------------------------------------

    /**
     * Validate an email address against RFC 5321 basics.
     *
     * @param email candidate email; null returns false
     * @return {@code true} if the address is non-null, ≤255 characters, and
     *         matches the expected pattern
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.length() > EMAIL_MAX) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate a username: alphanumeric and underscore only, 3–50 characters.
     *
     * @param username candidate username; null returns false
     * @return {@code true} if valid
     */
    public static boolean isValidUsername(String username) {
        if (username == null) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * Validate a password: minimum 12, maximum 128 characters.
     * Length-only validation is intentional — character-set restrictions
     * reduce entropy and are counter-productive.
     *
     * @param password candidate password; null returns false
     * @return {@code true} if length is within bounds
     */
    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        int len = password.length();
        return len >= PASSWORD_MIN && len <= PASSWORD_MAX;
    }

    /**
     * Validate a note title: non-blank, maximum 500 characters.
     *
     * @param title candidate title; null returns false
     * @return {@code true} if valid
     */
    public static boolean isValidTitle(String title) {
        if (title == null || title.isBlank()) {
            return false;
        }
        return title.length() <= TITLE_MAX;
    }

    /**
     * Validate note content: non-blank, maximum 100,000 characters.
     *
     * @param content candidate content; null returns false
     * @return {@code true} if valid
     */
    public static boolean isValidContent(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        return content.length() <= CONTENT_MAX;
    }

    /**
     * Validate a search query string: minimum 2, maximum 500 characters.
     *
     * @param q candidate query; null returns false
     * @return {@code true} if valid
     */
    public static boolean isValidSearchQuery(String q) {
        if (q == null) {
            return false;
        }
        int len = q.trim().length();
        return len >= SEARCH_MIN && len <= SEARCH_MAX;
    }

    /**
     * Validate a rating value: must be an integer between 1 and 5 inclusive.
     *
     * @param value candidate rating value
     * @return {@code true} if in range [1, 5]
     */
    public static boolean isValidRatingValue(int value) {
        return value >= 1 && value <= 5;
    }

    /**
     * Validate an optional rating comment.
     *
     * <p>Null is explicitly allowed (the comment field is optional). Non-null
     * values must not exceed 1,000 characters.</p>
     *
     * @param comment candidate comment; null is accepted
     * @return {@code true} if null or within the maximum length
     */
    public static boolean isValidComment(String comment) {
        if (comment == null) {
            return true; // optional field
        }
        return comment.length() <= COMMENT_MAX;
    }

    // -------------------------------------------------------------------------
    // Sanitization methods
    // -------------------------------------------------------------------------

    /**
     * Sanitize a filename for safe storage.
     *
     * <p>Steps applied in order:
     * <ol>
     *   <li>Strip any path separator characters (both {@code /} and {@code \}).</li>
     *   <li>Remove all characters that are not letters, digits, hyphen,
     *       underscore, or dot.</li>
     *   <li>Truncate to a maximum of 255 characters.</li>
     *   <li>If the result is blank after sanitization, return {@code "file"}.</li>
     * </ol>
     * </p>
     *
     * @param filename original filename supplied by the client; null is treated
     *                 as an empty string
     * @return sanitized filename, never null, never empty
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }

        // Strip path separators first — these are the most dangerous characters.
        String sanitized = filename.replace("/", "").replace("\\", "");

        // Remove any character that is not in the positive allow-list.
        sanitized = SAFE_FILENAME_CHAR.matcher(sanitized).replaceAll("");

        // Enforce maximum length.
        if (sanitized.length() > FILENAME_MAX) {
            sanitized = sanitized.substring(0, FILENAME_MAX);
        }

        // Fall back to a generic name if nothing safe remains.
        if (sanitized.isBlank()) {
            log.debug("Filename sanitized to 'file' (original contained no safe characters)");
            return "file";
        }

        return sanitized;
    }
}
