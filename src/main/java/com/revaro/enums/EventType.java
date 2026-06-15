package com.revaro.enums;

public enum EventType {
    CAR_MEET("Car Meet"),
    CAR_SHOW("Car Show"),
    CRUISE("Cruise"),
    TRACK_DAY("Track Day"),
    DRAG_STRIP_EVENT("Drag Strip Event"),
    DRAG_RACING("Drag Strip Event"),   // legacy — maps to same display/badge
    OTHER("Other");

    private final String displayName;

    EventType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** CSS class suffix for badge coloring */
    public String getBadgeClass() {
        return switch (this) {
            case CAR_MEET        -> "badge-car-meet";
            case CAR_SHOW        -> "badge-car-show";
            case CRUISE          -> "badge-cruise";
            case TRACK_DAY       -> "badge-track-day";
            case DRAG_STRIP_EVENT, DRAG_RACING -> "badge-drag-strip";
            case OTHER           -> "badge-other";
        };
    }
}
