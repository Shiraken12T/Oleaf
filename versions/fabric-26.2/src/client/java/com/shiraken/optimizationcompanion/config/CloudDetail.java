package com.shiraken.optimizationcompanion.config;

public enum CloudDetail {
    OFF("Off"),
    FAST("Fast"),
    FANCY("Fancy");

    private final String displayName;

    CloudDetail(String displayName) {
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
