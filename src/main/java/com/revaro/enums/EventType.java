package com.revaro.enums;

import java.util.Arrays;
import java.util.List;

public enum EventType {
    CAR_MEET("Car Meet", "badge-car-meet"),
    CAR_SHOW("Car Show", "badge-car-show"),
    CRUISE("Cruise", "badge-cruise"),
    TRACK_DAY("Track Day", "badge-track-day"),
    DRAG_STRIP_EVENT("Drag Strip Event", "badge-drag-strip"),
    // Old name for drag strip events, some rows in the database still use it
    DRAG_RACING("Drag Strip Event", "badge-drag-strip"),
    OTHER("Other", "badge-other");

    private final String displayName;
    private final String badgeClass;

    EventType(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public static List<EventType> selectable() {
        return Arrays.stream(values())
                .filter(type -> type != DRAG_RACING)
                .toList();
    }
}
