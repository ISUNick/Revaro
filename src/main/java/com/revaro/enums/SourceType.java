package com.revaro.enums;

public enum SourceType {
    ORGANIZER_DIRECT("Organizer Direct"),
    FACEBOOK("Facebook"),
    INSTAGRAM("Instagram"),
    DISCORD("Discord"),
    FORUM("Forum"),
    WEBSITE("Website"),
    OTHER("Other");

    private final String displayName;

    SourceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
