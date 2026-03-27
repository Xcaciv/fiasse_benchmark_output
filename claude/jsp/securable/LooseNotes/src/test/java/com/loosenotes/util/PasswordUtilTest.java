package com.loosenotes.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PasswordUtil.
 * SSEM: Testability - password utility is a pure function with no dependencies.
 */
class PasswordUtilTest {

    @Test
    void hashAndVerify_shouldMatch() {
        String password = "P@ssw0rd!";
        String hash = PasswordUtil.hash(password);
        assertTrue(PasswordUtil.verify(password, hash));
    }

    @Test
    void verify_wrongPassword_shouldReturnFalse() {
        String hash = PasswordUtil.hash("CorrectP@ss1");
        assertFalse(PasswordUtil.verify("WrongP@ss1", hash));
    }

    @Test
    void verify_nullInputs_shouldReturnFalse() {
        assertFalse(PasswordUtil.verify(null, null));
        assertFalse(PasswordUtil.verify("P@ssw0rd!", null));
        assertFalse(PasswordUtil.verify(null, "$2a$12$somehash"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "P@ssw0rd!",        // valid
        "MyC0mpl3x!Pass",   // valid
        "Aa1!aaaa"          // valid - minimal
    })
    void meetsComplexity_validPasswords_shouldReturnTrue(String password) {
        assertTrue(PasswordUtil.meetsComplexity(password));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "short1!",      // too short
        "alllowercase1!", // no uppercase
        "ALLUPPERCASE1!", // no lowercase
        "NoDigitAtAll!",  // no digit
        "NoSpecialChar1", // no special char
        ""               // empty
    })
    void meetsComplexity_invalidPasswords_shouldReturnFalse(String password) {
        assertFalse(PasswordUtil.meetsComplexity(password));
    }

    @Test
    void meetsComplexity_nullPassword_shouldReturnFalse() {
        assertFalse(PasswordUtil.meetsComplexity(null));
    }

    @Test
    void hash_charArray_shouldClearArray() {
        char[] password = "P@ssw0rd!".toCharArray();
        PasswordUtil.hash(password);
        // After hashing, the array should be zeroed
        for (char c : password) {
            assertEquals('\0', c, "Password char array should be zeroed after hashing");
        }
    }
}
