package com.loosenotes.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public final class SecurityUtil {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final int ITERATIONS = 65_536;
    private static final String CSRF_SESSION_KEY = "csrfToken";

    private SecurityUtil() {
    }

    public static PasswordHash hashPassword(String password) {
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt);
        return new PasswordHash(Base64.getEncoder().encodeToString(salt), Base64.getEncoder().encodeToString(hash));
    }

    public static boolean verifyPassword(String candidate, String saltBase64, String expectedHashBase64) {
        byte[] salt = Base64.getDecoder().decode(saltBase64);
        byte[] actual = pbkdf2(candidate.toCharArray(), salt);
        byte[] expected = Base64.getDecoder().decode(expectedHashBase64);
        return MessageDigest.isEqual(actual, expected);
    }

    public static String newToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String sha256Base64(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    public static String csrfToken(HttpSession session) {
        Object existing = session.getAttribute(CSRF_SESSION_KEY);
        if (existing instanceof String && !((String) existing).isBlank()) {
            return (String) existing;
        }
        String token = newToken();
        session.setAttribute(CSRF_SESSION_KEY, token);
        return token;
    }

    public static boolean isValidCsrf(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object expected = session.getAttribute(CSRF_SESSION_KEY);
        String provided = request.getParameter("csrfToken");
        if (!(expected instanceof String) || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(((String) expected).getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] pbkdf2(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, HASH_BYTES * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Unable to hash password", ex);
        }
    }

    public static final class PasswordHash {
        private final String saltBase64;
        private final String hashBase64;

        public PasswordHash(String saltBase64, String hashBase64) {
            this.saltBase64 = saltBase64;
            this.hashBase64 = hashBase64;
        }

        public String getSaltBase64() {
            return saltBase64;
        }

        public String getHashBase64() {
            return hashBase64;
        }
    }
}
