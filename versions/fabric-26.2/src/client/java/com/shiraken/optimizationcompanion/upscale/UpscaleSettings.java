package com.shiraken.optimizationcompanion.upscale;

/**
 * Persisted spatial upscaler settings (world pass only; HUD stays native).
 */
public class UpscaleSettings {
    private UpscaleQuality quality = UpscaleQuality.OFF;
    /** 0.0–1.0 RCAS-like sharpen strength when upscaling is active. */
    private float sharpen = 0.2F;
    /** Upscale filter (FSR/bicubic/bilinear/nearest). */
    private UpscaleAlgorithm algorithm = UpscaleAlgorithm.FSR;
    /** Custom render scale (%), used when quality == CUSTOM. */
    private int customScalePercent = 67;
    /** User acknowledged Iris warning / chose to keep upscale on with Iris. */
    private boolean forceWithIris = false;

    public UpscaleQuality getQuality() {
        return quality;
    }

    public void setQuality(UpscaleQuality quality) {
        this.quality = quality == null ? UpscaleQuality.OFF : quality;
    }

    public float getSharpen() {
        return sharpen;
    }

    public void setSharpen(float sharpen) {
        this.sharpen = Math.max(0.0F, Math.min(1.0F, sharpen));
    }

    public UpscaleAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(UpscaleAlgorithm algorithm) {
        this.algorithm = algorithm == null ? UpscaleAlgorithm.FSR : algorithm;
    }

    public int getCustomScalePercent() {
        return customScalePercent;
    }

    public void setCustomScalePercent(int customScalePercent) {
        this.customScalePercent = Math.max(30, Math.min(100, customScalePercent));
    }

    public boolean isForceWithIris() {
        return forceWithIris;
    }

    public void setForceWithIris(boolean forceWithIris) {
        this.forceWithIris = forceWithIris;
    }

    public boolean isEnabled() {
        return quality != null && quality.isEnabled();
    }

    public float getScale() {
        if (!isEnabled()) {
            return 1.0F;
        }
        if (quality == UpscaleQuality.CUSTOM) {
            return Math.max(0.30F, Math.min(1.0F, customScalePercent / 100.0F));
        }
        return quality.getScale();
    }

    public void sanitize() {
        if (quality == null) {
            quality = UpscaleQuality.OFF;
        }
        if (algorithm == null) {
            algorithm = UpscaleAlgorithm.FSR;
        }
        setCustomScalePercent(customScalePercent);
        setSharpen(sharpen);
    }
}
