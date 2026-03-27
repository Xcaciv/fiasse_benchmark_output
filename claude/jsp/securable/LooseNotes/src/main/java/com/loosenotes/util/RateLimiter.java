package com.loosenotes.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe in-memory rate limiter using a fixed-window counter.
 * SSEM: Availability - prevents brute-force and abuse.
 * SSEM: Resilience - concurrent-safe with atomic counters.
 *
 * <p>For production, replace with a distributed store (Redis) if multiple nodes.
 */
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private final int maxAttempts;
    private final long windowSeconds;

    /** Key → [attemptCount, windowStartEpochSecond] */
    private final Map<String, long[]> counters = new ConcurrentHashMap<>();

    /**
     * @param maxAttempts   max requests allowed per window
     * @param windowSeconds length of the time window in seconds
     */
    public RateLimiter(int maxAttempts, long windowSeconds) {
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
    }

    /**
     * Records an attempt and returns whether the key is within limits.
     *
     * @param key       rate limit key (e.g., IP address or "ip:username")
     * @return true if the attempt is allowed; false if limit exceeded
     */
    public boolean tryAcquire(String key) {
        long now = Instant.now().getEpochSecond();
        long[] window = counters.compute(key, (k, existing) ->
            resetOrIncrement(existing, now));
        boolean allowed = window[0] <= maxAttempts;
        if (!allowed) {
            log.warn("Rate limit exceeded for key: {}", key);
        }
        return allowed;
    }

    /**
     * Resets the counter for a given key (e.g., on successful login).
     *
     * @param key the rate limit key to reset
     */
    public void reset(String key) {
        counters.remove(key);
    }

    /**
     * Returns remaining attempts for the given key within the current window.
     */
    public int remainingAttempts(String key) {
        long now = Instant.now().getEpochSecond();
        long[] window = counters.get(key);
        if (window == null || isWindowExpired(window[1], now)) {
            return maxAttempts;
        }
        return Math.max(0, maxAttempts - (int) window[0]);
    }

    private long[] resetOrIncrement(long[] existing, long now) {
        if (existing == null || isWindowExpired(existing[1], now)) {
            return new long[]{1L, now};
        }
        return new long[]{existing[0] + 1L, existing[1]};
    }

    private boolean isWindowExpired(long windowStart, long now) {
        return now - windowStart >= windowSeconds;
    }

    /**
     * Periodically evicts stale entries to prevent unbounded memory growth.
     * Called by the cleanup scheduler.
     */
    public void evictExpired() {
        long now = Instant.now().getEpochSecond();
        int removed = 0;
        for (Map.Entry<String, long[]> entry : counters.entrySet()) {
            if (isWindowExpired(entry.getValue()[1], now)) {
                counters.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Evicted {} expired rate limit entries", removed);
        }
    }
}
