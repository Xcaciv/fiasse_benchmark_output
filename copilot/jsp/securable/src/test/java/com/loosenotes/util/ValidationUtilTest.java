package com.loosenotes.util;

import com.loosenotes.util.ValidationUtil.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for ValidationUtil — cover trust boundary enforcement (Testability). */
class ValidationUtilTest {

    @Test
    void validateUsername_valid() {
        assertEquals("alice", ValidationUtil.validateUsername("alice"));
        assertEquals("Bob_99", ValidationUtil.validateUsername("Bob_99"));
    }

    @Test
    void validateUsername_tooShort_throws() {
        assertThrows(ValidationException.class,
                () -> ValidationUtil.validateUsername("ab"));
    }

    @Test
    void validateUsername_specialChars_throws() {
        assertThrows(ValidationException.class,
                () -> ValidationUtil.validateUsername("alice!"));
    }

    @Test
    void validateEmail_valid() {
        assertEquals("user@example.com", ValidationUtil.validateEmail("User@Example.COM"));
    }

    @Test
    void validateEmail_invalid_throws() {
        assertThrows(ValidationException.class,
                () -> ValidationUtil.validateEmail("not-an-email"));
    }

    @Test
    void validatePassword_tooShort_throws() {
        assertThrows(ValidationException.class,
                () -> ValidationUtil.validatePassword("short"));
    }

    @Test
    void validatePassword_valid() {
        assertDoesNotThrow(() -> ValidationUtil.validatePassword("validPass1"));
    }

    @Test
    void validateStars_outOfRange_throws() {
        assertThrows(ValidationException.class,
                () -> ValidationUtil.validateStars("0"));
        assertThrows(ValidationException.class,
                () -> ValidationUtil.validateStars("6"));
    }

    @Test
    void validateStars_valid() {
        assertEquals(3, ValidationUtil.validateStars("3"));
    }

    @Test
    void validateFileExtension_disallowed_throws() {
        assertThrows(ValidationException.class,
                () -> ValidationUtil.validateFileExtension("malware.exe"));
    }

    @Test
    void validateFileExtension_allowed() {
        assertDoesNotThrow(() -> ValidationUtil.validateFileExtension("report.pdf"));
        assertDoesNotThrow(() -> ValidationUtil.validateFileExtension("image.PNG"));
    }

    @Test
    void requireNonBlank_null_throws() {
        assertThrows(ValidationException.class,
                () -> ValidationUtil.requireNonBlank(null, "field", 100));
    }

    @Test
    void requireNonBlank_tooLong_throws() {
        assertThrows(ValidationException.class,
                () -> ValidationUtil.requireNonBlank("a".repeat(101), "field", 100));
    }
}
