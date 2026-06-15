package com.revaro.enums;

public enum ReportType {
    EVENT("Event"),
    COMMENT("Comment"),
    USER("User Profile");

    private final String displayName;
    ReportType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
