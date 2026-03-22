package com.loosenotes.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppConfig {
    private final Path homeDirectory;
    private final Path databasePath;
    private final Path uploadDirectory;
    private final boolean demoMode;
    private final long maxUploadBytes;

    private AppConfig(Path homeDirectory, Path databasePath, Path uploadDirectory, boolean demoMode, long maxUploadBytes) {
        this.homeDirectory = homeDirectory;
        this.databasePath = databasePath;
        this.uploadDirectory = uploadDirectory;
        this.demoMode = demoMode;
        this.maxUploadBytes = maxUploadBytes;
    }

    public static AppConfig load() {
        String configuredHome = System.getProperty("loosenotes.home");
        if (configuredHome == null || configuredHome.isBlank()) {
            configuredHome = System.getenv("LOOSENOTES_HOME");
        }
        if (configuredHome == null || configuredHome.isBlank()) {
            configuredHome = Paths.get(System.getProperty("user.home"), ".loosenotes").toString();
        }

        Path homeDirectory = Paths.get(configuredHome).toAbsolutePath().normalize();
        Path dataDirectory = homeDirectory.resolve("data");
        Path uploadDirectory = homeDirectory.resolve("uploads");
        Path databasePath = dataDirectory.resolve("loosenotes.db");

        try {
            Files.createDirectories(dataDirectory);
            Files.createDirectories(uploadDirectory);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to initialize application storage directories.", ex);
        }

        return new AppConfig(
                homeDirectory,
                databasePath,
                uploadDirectory,
                readBooleanSetting("loosenotes.demoMode", "LOOSENOTES_DEMO_MODE", true),
                5L * 1024L * 1024L);
    }

    private static boolean readBooleanSetting(String propertyName, String envName, boolean defaultValue) {
        String configuredValue = System.getProperty(propertyName);
        if (configuredValue == null || configuredValue.isBlank()) {
            configuredValue = System.getenv(envName);
        }
        if (configuredValue == null || configuredValue.isBlank()) {
            return defaultValue;
        }
        String normalizedValue = configuredValue.trim();
        if ("true".equalsIgnoreCase(normalizedValue) || "1".equals(normalizedValue) || "yes".equalsIgnoreCase(normalizedValue)) {
            return true;
        }
        if ("false".equalsIgnoreCase(normalizedValue) || "0".equals(normalizedValue) || "no".equalsIgnoreCase(normalizedValue)) {
            return false;
        }
        throw new IllegalStateException("Invalid boolean value for " + propertyName + " or " + envName + ": '" + configuredValue + '\'');
    }

    public Path getHomeDirectory() {
        return homeDirectory;
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    public Path getUploadDirectory() {
        return uploadDirectory;
    }

    public boolean isDemoMode() {
        return demoMode;
    }

    public long getMaxUploadBytes() {
        return maxUploadBytes;
    }
}
