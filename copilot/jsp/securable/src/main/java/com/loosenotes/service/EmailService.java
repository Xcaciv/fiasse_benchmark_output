package com.loosenotes.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

public final class EmailService {
    private static Path outboxDirectory;

    private EmailService() {
    }

    public static synchronized void initialize(Path directory) throws IOException {
        if (outboxDirectory != null) {
            return;
        }
        Files.createDirectories(directory);
        outboxDirectory = directory.toAbsolutePath().normalize();
    }

    public static void writePasswordResetEmail(String emailAddress, String username, String resetLink) throws IOException {
        if (outboxDirectory == null) {
            throw new IllegalStateException("Outbox directory has not been initialized.");
        }
        String safeEmail = emailAddress.replaceAll("[^A-Za-z0-9@._-]", "_");
        Path file = outboxDirectory.resolve(Instant.now().toEpochMilli() + "-" + safeEmail + "-" + UUID.randomUUID() + ".txt");
        String body = String.join(System.lineSeparator(),
            "To: " + emailAddress,
            "Subject: Loose Notes password reset",
            "",
            "Hello " + username + ",",
            "",
            "Use the following one-time link within 1 hour to reset your password:",
            resetLink,
            "",
            "If you did not request this reset, you can ignore this email."
        );
        Files.writeString(file, body, StandardCharsets.UTF_8);
    }
}
