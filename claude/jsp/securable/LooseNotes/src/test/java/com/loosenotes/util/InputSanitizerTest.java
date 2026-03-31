package com.loosenotes.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InputSanitizer – control character removal and filename safety.
 *
 * SSEM: Integrity – trust boundary input canonicalization.
 */
class InputSanitizerTest {

    @Test
    void sanitize_removesNullBytes() {
        String result = InputSanitizer.sanitize("hello\u0000world");
        assertFalse(result.contains("\u0000"));
        assertEquals("helloworld", result);
    }

    @Test
    void sanitize_removesControlCharsButPreservesNewlineAndTab() {
        String input = "line1\nline2\ttabbed\u0007bell";
        String result = InputSanitizer.sanitize(input);
        assertTrue(result.contains("\n"));
        assertTrue(result.contains("\t"));
        assertFalse(result.contains("\u0007"));
    }

    @Test
    void sanitize_returnsNullForNull() {
        assertNull(InputSanitizer.sanitize(null));
    }

    @Test
    void sanitizeLine_stripsNewlines() {
        String result = InputSanitizer.sanitizeLine("title\ninjected");
        assertFalse(result.contains("\n"));
    }

    @Test
    void sanitizeFilename_stripsPatchTraversal() {
        String result = InputSanitizer.sanitizeFilename("../../etc/passwd");
        assertFalse(result.contains("/"));
        assertFalse(result.contains(".."));
    }

    @Test
    void sanitizeFilename_handlesNull() {
        assertEquals("unnamed", InputSanitizer.sanitizeFilename(null));
    }

    @Test
    void sanitizeFilename_truncatesLongNames() {
        String longName = "a".repeat(300) + ".txt";
        String result = InputSanitizer.sanitizeFilename(longName);
        assertTrue(result.length() <= 255);
    }
}
