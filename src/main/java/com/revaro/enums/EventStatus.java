package com.revaro.enums;

/**
 * Lifecycle status of an event.
 */
public enum EventStatus {

    ACTIVE("Active"),
    CANCELLED("Cancelled"),
    POSTPONED("Postponed");

    private final String displayName;

    EventStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
