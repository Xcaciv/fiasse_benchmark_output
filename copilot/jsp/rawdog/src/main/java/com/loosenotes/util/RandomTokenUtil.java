package com.loosenotes.util;

import java.util.UUID;

public final class RandomTokenUtil {
    private RandomTokenUtil() {
    }

    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }
}
