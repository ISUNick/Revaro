package com.revaro.enums;

public enum ReportStatus {
    PENDING("Pending"),
    REVIEWED("Reviewed"),
    DISMISSED("Dismissed");

    private final String displayName;
    ReportStatus(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
