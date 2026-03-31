package com.loosenotes.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RateLimiter – sliding window logic.
 *
 * SSEM: Availability – rate limiting prevents brute-force exhaustion.
 */
class RateLimiterTest {

    @Test
    void tryAcquire_allowsAttemptsWithinLimit() {
        RateLimiter limiter = new RateLimiter(3, 60);
        assertTrue(limiter.tryAcquire("1.2.3.4"));
        assertTrue(limiter.tryAcquire("1.2.3.4"));
        assertTrue(limiter.tryAcquire("1.2.3.4"));
    }

    @Test
    void tryAcquire_blocksWhenLimitExceeded() {
        RateLimiter limiter = new RateLimiter(2, 60);
        limiter.tryAcquire("5.6.7.8");
        limiter.tryAcquire("5.6.7.8");
        assertFalse(limiter.tryAcquire("5.6.7.8"), "Third attempt should be blocked");
    }

    @Test
    void reset_clearsCounterForKey() {
        RateLimiter limiter = new RateLimiter(2, 60);
        limiter.tryAcquire("9.9.9.9");
        limiter.tryAcquire("9.9.9.9");
        limiter.reset("9.9.9.9");
        assertTrue(limiter.tryAcquire("9.9.9.9"), "After reset, attempt should be allowed");
    }

    @Test
    void tryAcquire_tracksKeysSeparately() {
        RateLimiter limiter = new RateLimiter(1, 60);
        limiter.tryAcquire("10.0.0.1");
        assertFalse(limiter.tryAcquire("10.0.0.1"), "IP1 should be blocked");
        assertTrue(limiter.tryAcquire("10.0.0.2"),  "IP2 should still be allowed");
    }

    @Test
    void getAttemptCount_returnsCurrentCount() {
        RateLimiter limiter = new RateLimiter(5, 60);
        assertEquals(0, limiter.getAttemptCount("2.2.2.2"));
        limiter.tryAcquire("2.2.2.2");
        limiter.tryAcquire("2.2.2.2");
        assertEquals(2, limiter.getAttemptCount("2.2.2.2"));
    }
}
