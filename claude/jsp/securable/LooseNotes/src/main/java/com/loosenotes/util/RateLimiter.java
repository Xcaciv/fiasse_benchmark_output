package com.loosenotes.util;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window rate limiter.
 *
 * SSEM / ASVS alignment:
 * - ASVS V2.3 (Brute Force): limits login and registration attempts per IP.
 * - Availability: prevents a single IP from exhausting authentication resources.
 * - Resilience: ConcurrentHashMap ensures thread-safe operation.
 * - Modifiability: maxAttempts and windowSeconds are injected, not hard-coded.
 *
 * Trade-off: in-memory state is lost on restart and is per-JVM instance.
 * For multi-node deployments, replace with a Redis-backed implementation
 * behind the same interface.
 */
public class RateLimiter {

    private final int maxAttempts;
    private final long windowMillis;

    /** Key → list of attempt timestamps within the current window. */
    private final Map<String, WindowEntry> entries = new ConcurrentHashMap<>();

    public RateLimiter(int maxAttempts, int windowSeconds) {
        this.maxAttempts  = maxAttempts;
        this.windowMillis = (long) windowSeconds * 1000L;
    }

    /**
     * Records an attempt and returns true if the key is within the allowed limit.
     *
     * @param key a bucketing key, e.g. IP address
     * @return true if the attempt is allowed; false if rate-limited
     */
    public boolean tryAcquire(String key) {
        long now = Instant.now().toEpochMilli();
        WindowEntry entry = entries.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart > windowMillis) {
                return new WindowEntry(now, 1);
            }
            existing.count++;
            return existing;
        });
        return entry.count <= maxAttempts;
    }

    /**
     * Resets the counter for a key (e.g., after a successful login).
     *
     * @param key the key to reset
     */
    public void reset(String key) {
        entries.remove(key);
    }

    /** Returns the number of attempts recorded in the current window for a key. */
    public int getAttemptCount(String key) {
        WindowEntry entry = entries.get(key);
        if (entry == null) return 0;
        long now = Instant.now().toEpochMilli();
        if (now - entry.windowStart > windowMillis) return 0;
        return entry.count;
    }

    /** Sliding-window state per key. */
    private static final class WindowEntry {
        long windowStart;
        int  count;

        WindowEntry(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count       = count;
        }
    }
}
