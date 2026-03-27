package com.loosenotes.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InputSanitizer.
 * SSEM: Integrity - validates the canonicalize/sanitize pipeline.
 */
class InputSanitizerTest {

    @Test
    void sanitizeSingleLine_trimsWhitespace() {
        assertEquals("hello world", InputSanitizer.sanitizeSingleLine("  hello  world  "));
    }

    @Test
    void sanitizeSingleLine_removesControlCharacters() {
        String input = "hello\u0000world\u0001";
        String result = InputSanitizer.sanitizeSingleLine(input);
        assertFalse(result.contains("\u0000"));
        assertFalse(result.contains("\u0001"));
        assertTrue(result.contains("hello"));
    }

    @Test
    void sanitizeSingleLine_removesNewlines() {
        String result = InputSanitizer.sanitizeSingleLine("line1\nline2");
        assertFalse(result.contains("\n"));
    }

    @Test
    void sanitizeSingleLine_null_returnsNull() {
        assertNull(InputSanitizer.sanitizeSingleLine(null));
    }

    @Test
    void sanitizeMultiLine_preservesNewlines() {
        String result = InputSanitizer.sanitizeMultiLine("line1\nline2");
        assertTrue(result.contains("\n"));
    }

    @Test
    void sanitizeFilename_preventsPathTraversal() {
        assertEquals("..etc_passwd", InputSanitizer.sanitizeFilename("../../etc/passwd"));
        assertEquals("..etc_passwd", InputSanitizer.sanitizeFilename("..\\..\\etc\\passwd"));
    }

    @Test
    void sanitizeFilename_null_returnsDefault() {
        assertEquals("unnamed_file", InputSanitizer.sanitizeFilename(null));
        assertEquals("unnamed_file", InputSanitizer.sanitizeFilename(""));
    }

    @Test
    void sanitizeSearchQuery_escapesLikeSpecialChars() {
        String result = InputSanitizer.sanitizeSearchQuery("100% match_all", 100);
        assertTrue(result.contains("\\%"));
        assertTrue(result.contains("\\_"));
    }

    @Test
    void sanitizeSearchQuery_enforcesMaxLength() {
        String longQuery = "a".repeat(200);
        String result = InputSanitizer.sanitizeSearchQuery(longQuery, 50);
        assertTrue(result.length() <= 50);
    }

    @Test
    void canonicalizeEmail_lowercasesAndTrims() {
        assertEquals("user@example.com", InputSanitizer.canonicalizeEmail("  USER@EXAMPLE.COM  "));
    }
}
