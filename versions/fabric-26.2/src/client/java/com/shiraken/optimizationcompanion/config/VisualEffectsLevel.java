package com.shiraken.optimizationcompanion.config;

public enum VisualEffectsLevel {
    MINIMAL("Minimal"),
    REDUCED("Reduced"),
    FULL("Full");

    private final String displayName;

    VisualEffectsLevel(String displayName) {
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
