package com.shiraken.optimizationcompanion.config;

public enum GraphicsQuality {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    ULTRA("Ultra");

    private final String displayName;

    GraphicsQuality(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
