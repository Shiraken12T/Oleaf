package com.shiraken.optimizationcompanion.mods;

public record DetectedMod(String modId, String displayName, String purpose, boolean installed) {
    public String statusLabel() {
        return installed ? "Installed" : "Not installed";
    }
}
