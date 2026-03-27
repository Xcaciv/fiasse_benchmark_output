package com.loosenotes.util;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public class TokenUtil {

    private static final SecureRandom random = new SecureRandom();

    private TokenUtil() {}

    public static String generateShareToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String generateResetToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String generateCsrfToken() {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
