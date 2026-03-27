package com.loosenotes.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RateLimiter.
 * SSEM: Availability - validates that rate limiting functions correctly.
 */
class RateLimiterTest {

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter(3, 60); // 3 attempts per 60 seconds
    }

    @Test
    void tryAcquire_withinLimit_returnsTrue() {
        assertTrue(rateLimiter.tryAcquire("192.168.1.1:user"));
        assertTrue(rateLimiter.tryAcquire("192.168.1.1:user"));
        assertTrue(rateLimiter.tryAcquire("192.168.1.1:user"));
    }

    @Test
    void tryAcquire_exceedsLimit_returnsFalse() {
        String key = "192.168.1.2:user";
        rateLimiter.tryAcquire(key);
        rateLimiter.tryAcquire(key);
        rateLimiter.tryAcquire(key);
        assertFalse(rateLimiter.tryAcquire(key)); // 4th attempt should fail
    }

    @Test
    void reset_afterReset_allowsNewAttempts() {
        String key = "192.168.1.3:user";
        rateLimiter.tryAcquire(key);
        rateLimiter.tryAcquire(key);
        rateLimiter.tryAcquire(key);
        assertFalse(rateLimiter.tryAcquire(key));

        rateLimiter.reset(key);
        assertTrue(rateLimiter.tryAcquire(key));
    }

    @Test
    void differentKeys_areIndependent() {
        String key1 = "192.168.1.4:user1";
        String key2 = "192.168.1.4:user2";

        rateLimiter.tryAcquire(key1);
        rateLimiter.tryAcquire(key1);
        rateLimiter.tryAcquire(key1);
        assertFalse(rateLimiter.tryAcquire(key1));

        // Different key should still be allowed
        assertTrue(rateLimiter.tryAcquire(key2));
    }

    @Test
    void remainingAttempts_decreasesWithUse() {
        String key = "192.168.1.5:user";
        assertEquals(3, rateLimiter.remainingAttempts(key));
        rateLimiter.tryAcquire(key);
        assertEquals(2, rateLimiter.remainingAttempts(key));
    }
}
