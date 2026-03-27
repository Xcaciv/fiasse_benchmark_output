package com.loosenotes.model;

/**
 * User role enumeration.
 * SSEM: Authenticity - role-based access control foundation.
 * SSEM: Modifiability - centralized role definition.
 */
public enum Role {
    USER,
    ADMIN;

    /**
     * Parse a role from a database string value.
     * Defaults to USER for unknown values (fail-safe).
     *
     * @param value raw string from database
     * @return corresponding Role, defaulting to USER
     */
    public static Role fromString(String value) {
        if (value == null) {
            return USER;
        }
        try {
            return Role.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return USER;
        }
    }
}
