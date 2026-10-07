package com.shiraken.optimizationcompanion.config;

public enum ParticleDetail {
    MINIMAL("Minimal"),
    DECREASED("Decreased"),
    ALL("All");

    private final String displayName;

    ParticleDetail(String displayName) {
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
