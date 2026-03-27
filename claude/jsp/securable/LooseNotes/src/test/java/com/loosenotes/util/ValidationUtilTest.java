package com.loosenotes.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ValidationUtil.
 * SSEM: Integrity - validates boundary enforcement.
 */
class ValidationUtilTest {

    @ParameterizedTest
    @ValueSource(strings = {"alice", "bob_123", "user-name", "ABC"})
    void isValidUsername_valid(String username) {
        assertTrue(ValidationUtil.isValidUsername(username));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ab", "user name", "user@name", "a".repeat(51)})
    void isValidUsername_invalid(String username) {
        assertFalse(ValidationUtil.isValidUsername(username));
    }

    @Test
    void isValidUsername_null_returnsFalse() {
        assertFalse(ValidationUtil.isValidUsername(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"user@example.com", "a.b+c@sub.domain.org"})
    void isValidEmail_valid(String email) {
        assertTrue(ValidationUtil.isValidEmail(email));
    }

    @ParameterizedTest
    @ValueSource(strings = {"notanemail", "missing@", "@domain.com", "space @a.com"})
    void isValidEmail_invalid(String email) {
        assertFalse(ValidationUtil.isValidEmail(email));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void isValidRatingValue_valid(int value) {
        assertTrue(ValidationUtil.isValidRatingValue(value));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6, -1, Integer.MAX_VALUE})
    void isValidRatingValue_invalid(int value) {
        assertFalse(ValidationUtil.isValidRatingValue(value));
    }

    @Test
    void isValidToken_validToken() {
        String token = com.loosenotes.util.SecureTokenUtil.generateToken();
        assertTrue(ValidationUtil.isValidToken(token));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "short", "../etc/passwd", "token with spaces", "<script>"})
    void isValidToken_invalid(String token) {
        assertFalse(ValidationUtil.isValidToken(token));
    }

    @Test
    void parseLongSafe_validNumber_returnsValue() {
        assertEquals(42L, ValidationUtil.parseLongSafe("42"));
    }

    @Test
    void parseLongSafe_invalid_returnsMinus1() {
        assertEquals(-1L, ValidationUtil.parseLongSafe("abc"));
        assertEquals(-1L, ValidationUtil.parseLongSafe(null));
        assertEquals(-1L, ValidationUtil.parseLongSafe(""));
    }
}
