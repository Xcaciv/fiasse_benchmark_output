package com.loosenotes.model;

/**
 * Enumerated roles used for role-based access control.
 * Using an enum prevents invalid role strings from reaching the application.
 * SSEM: Integrity – constrained enum value, no free-text role strings.
 */
public enum Role {
    USER,
    ADMIN
}
