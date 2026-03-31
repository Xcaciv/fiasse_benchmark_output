package com.loosenotes.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PasswordUtil – ensures BCrypt round-trip and policy enforcement.
 *
 * SSEM: Testability – utility methods are pure functions, no container needed.
 */
class PasswordUtilTest {

    @Test
    void hash_producesNonNullBcryptString() {
        String hash = PasswordUtil.hash("password123".toCharArray());
        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$"), "Expected BCrypt prefix");
    }

    @Test
    void verify_returnsTrueForMatchingPassword() {
        char[] password = "S3cur3P@ss!".toCharArray();
        String hash = PasswordUtil.hash(password);
        assertTrue(PasswordUtil.verify("S3cur3P@ss!".toCharArray(), hash));
    }

    @Test
    void verify_returnsFalseForWrongPassword() {
        String hash = PasswordUtil.hash("correct".toCharArray());
        assertFalse(PasswordUtil.verify("wrong".toCharArray(), hash));
    }

    @Test
    void verify_returnsFalseForNullHash() {
        assertFalse(PasswordUtil.verify("any".toCharArray(), null));
    }

    @Test
    void meetsPolicy_acceptsValidPassword() {
        assertTrue(PasswordUtil.meetsPolicy("12345678".toCharArray()));
        assertTrue(PasswordUtil.meetsPolicy("a".repeat(128).toCharArray()));
    }

    @Test
    void meetsPolicy_rejectsTooShort() {
        assertFalse(PasswordUtil.meetsPolicy("1234567".toCharArray()));
    }

    @Test
    void meetsPolicy_rejectsTooLong() {
        assertFalse(PasswordUtil.meetsPolicy("a".repeat(129).toCharArray()));
    }

    @Test
    void meetsPolicy_rejectsNull() {
        assertFalse(PasswordUtil.meetsPolicy(null));
    }
}
