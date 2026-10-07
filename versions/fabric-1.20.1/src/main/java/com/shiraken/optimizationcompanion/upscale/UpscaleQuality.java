package com.shiraken.optimizationcompanion.upscale;

/**
 * AAA-style spatial upscale presets (internal render scale as fraction of native res).
 */
public enum UpscaleQuality {
    OFF("Off / Native", 1.0F),
    ULTRA_QUALITY("Ultra Quality", 0.77F),
    QUALITY("Quality", 0.67F),
    BALANCED("Balanced", 0.59F),
    PERFORMANCE("Performance", 0.50F),
    /** Stronger GPU cut for low-end / shader-bound scenes. */
    ULTRA_PERFORMANCE("Ultra Performance", 0.40F),
    /** User-defined render scale via the custom slider. */
    CUSTOM("Custom", 0.67F);

    private final String displayName;
    private final float scale;

    UpscaleQuality(String displayName, float scale) {
        this.displayName = displayName;
        this.scale = scale;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Internal world render scale (1.0 = native). */
    public float getScale() {
        return scale;
    }

    public boolean isEnabled() {
        return this != OFF;
    }
}
