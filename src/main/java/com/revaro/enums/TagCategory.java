package com.revaro.enums;

public enum TagCategory {
    STYLE("Style & Culture", "rgba(99,102,241,0.15)", "#a5b4fc"),
    VIBE("Vibe & Format", "rgba(16,185,129,0.15)", "#6ee7b7"),
    PERFORMANCE("Performance", "rgba(239,68,68,0.15)", "#fca5a5"),
    BRANDS("Brands", "rgba(245,158,11,0.15)", "#fde68a"),
    OTHER("Other", "rgba(107,114,128,0.15)", "#d1d5db");

    private final String displayName;
    private final String bgColor;
    private final String textColor;

    TagCategory(String displayName, String bgColor, String textColor) {
        this.displayName = displayName;
        this.bgColor = bgColor;
        this.textColor = textColor;
    }

    public String getDisplayName() { return displayName; }
    public String getBgColor() { return bgColor; }
    public String getTextColor() { return textColor; }
}
