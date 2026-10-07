package com.shiraken.optimizationcompanion.upscale;

/**
 * Upscale filter used to bring the low-res world image back to native size.
 * <p>
 * FSR uses a real AMD FidelityFX FSR1 two-pass (EASU then RCAS).
 * SGSR1 is Qualcomm Snapdragon Game Super Resolution 1 (single pass).
 * Other modes are simple spatial fallbacks.
 */
public enum UpscaleAlgorithm {
    FSR("FSR 1 (EASU+RCAS)", 0),
    SGSR1("SGSR 1 (Snapdragon)", 1),
    BICUBIC("Bicubic (Smooth)", 2),
    BILINEAR("Bilinear (Soft)", 3),
    NEAREST("Nearest (Pixelated)", 4);

    private final String displayName;
    private final int shaderIndex;

    UpscaleAlgorithm(String displayName, int shaderIndex) {
        this.displayName = displayName;
        this.shaderIndex = shaderIndex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getShaderIndex() {
        return shaderIndex;
    }

    public boolean isRealFsr1() {
        return this == FSR;
    }

    public boolean isSgsr1() {
        return this == SGSR1;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
