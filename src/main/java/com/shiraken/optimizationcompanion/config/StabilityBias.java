package com.shiraken.optimizationcompanion.config;

public enum StabilityBias {
    STABILITY("Smoothness"),
    BALANCED("Balanced"),
    PERFORMANCE("Max FPS");

    private final String displayName;

    StabilityBias(String displayName) {
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
