package com.loosenotes.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for PasswordUtil — hashing and constant-time verification (Testability). */
class PasswordUtilTest {

    @Test
    void hash_produces_different_salts() {
        String hash1 = PasswordUtil.hash("samePassword");
        String hash2 = PasswordUtil.hash("samePassword");
        assertNotEquals(hash1, hash2, "BCrypt should produce different salts each time");
    }

    @Test
    void verify_correct_password_returnsTrue() {
        String hash = PasswordUtil.hash("MySecurePass1");
        assertTrue(PasswordUtil.verify("MySecurePass1", hash));
    }

    @Test
    void verify_wrong_password_returnsFalse() {
        String hash = PasswordUtil.hash("RightPassword");
        assertFalse(PasswordUtil.verify("WrongPassword", hash));
    }

    @Test
    void verify_null_password_returnsFalse() {
        String hash = PasswordUtil.hash("AnyPassword");
        assertFalse(PasswordUtil.verify(null, hash));
    }

    @Test
    void verify_null_hash_returnsFalse() {
        assertFalse(PasswordUtil.verify("password", null));
    }

    @Test
    void hash_blank_throws() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(""));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(null));
    }

    @Test
    void hash_starts_with_bcrypt_prefix() {
        String hash = PasswordUtil.hash("testPass99");
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"),
                "BCrypt hash should start with $2a$ or $2b$");
    }
}
