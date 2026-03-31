package com.loosenotes.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ValidationUtil – input validation rules.
 *
 * SSEM: Testability – pure static methods, no dependencies.
 */
class ValidationUtilTest {

    // ── Email ────────────────────────────────────────────────────────────────

    @Test
    void isValidEmail_acceptsValidFormats() {
        assertTrue(ValidationUtil.isValidEmail("user@example.com"));
        assertTrue(ValidationUtil.isValidEmail("user+tag@sub.domain.org"));
    }

    @Test
    void isValidEmail_rejectsMissingAtSign() {
        assertFalse(ValidationUtil.isValidEmail("notanemail.com"));
    }

    @Test
    void isValidEmail_rejectsNullAndBlank() {
        assertFalse(ValidationUtil.isValidEmail(null));
        assertFalse(ValidationUtil.isValidEmail("   "));
    }

    // ── Username ─────────────────────────────────────────────────────────────

    @Test
    void isValidUsername_acceptsAlphanumericUnderscore() {
        assertTrue(ValidationUtil.isValidUsername("john_doe"));
        assertTrue(ValidationUtil.isValidUsername("user123"));
        assertTrue(ValidationUtil.isValidUsername("aaa"));
    }

    @Test
    void isValidUsername_rejectsTooShort() {
        assertFalse(ValidationUtil.isValidUsername("ab"));
    }

    @Test
    void isValidUsername_rejectsSpecialChars() {
        assertFalse(ValidationUtil.isValidUsername("user@name"));
        assertFalse(ValidationUtil.isValidUsername("<script>"));
    }

    // ── Rating ───────────────────────────────────────────────────────────────

    @Test
    void isValidRating_acceptsOneToFive() {
        for (int i = 1; i <= 5; i++) {
            assertTrue(ValidationUtil.isValidRating(i), "Expected " + i + " to be valid");
        }
    }

    @Test
    void isValidRating_rejectsZeroAndSix() {
        assertFalse(ValidationUtil.isValidRating(0));
        assertFalse(ValidationUtil.isValidRating(6));
    }

    // ── parseLongId ───────────────────────────────────────────────────────────

    @Test
    void parseLongId_parsesPositiveInteger() {
        assertEquals(42L, ValidationUtil.parseLongId("42"));
    }

    @Test
    void parseLongId_returnsNegativeOneForNonNumeric() {
        assertEquals(-1L, ValidationUtil.parseLongId("abc"));
        assertEquals(-1L, ValidationUtil.parseLongId(null));
        assertEquals(-1L, ValidationUtil.parseLongId("0"));
        assertEquals(-1L, ValidationUtil.parseLongId("-5"));
    }
}
