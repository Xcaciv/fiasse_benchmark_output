package com.loosenotes.util;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class RateLimiter {
    private static final RateLimiter INSTANCE = new RateLimiter();
    private final Map<String, List<Long>> requestCounts = new ConcurrentHashMap<>();
    private static final long WINDOW_MS = 60_000L;

    private RateLimiter() {}

    public static RateLimiter getInstance() {
        return INSTANCE;
    }

    public boolean isAllowed(String key, int maxRequests) {
        long now = System.currentTimeMillis();
        requestCounts.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
        List<Long> timestamps = requestCounts.get(key);
        timestamps.removeIf(t -> now - t > WINDOW_MS);
        if (timestamps.size() >= maxRequests) {
            return false;
        }
        timestamps.add(now);
        return true;
    }

    public long getRetryAfterSeconds(String key) {
        List<Long> timestamps = requestCounts.get(key);
        if (timestamps == null || timestamps.isEmpty()) return 0;
        long oldest = timestamps.stream().mapToLong(Long::longValue).min().orElse(0);
        long resetTime = oldest + WINDOW_MS;
        long diff = (resetTime - System.currentTimeMillis()) / 1000;
        return Math.max(1, diff);
    }
}
