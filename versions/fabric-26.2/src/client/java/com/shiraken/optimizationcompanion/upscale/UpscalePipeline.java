package com.shiraken.optimizationcompanion.upscale;

import com.shiraken.optimizationcompanion.OleafClient;
import com.shiraken.optimizationcompanion.config.ConfigManager;

/**
 * Spatial upscaler facade for Minecraft 26.2.
 * <p>
 * Full FSR1/SGSR GPU passes from 1.21.11 are stubbed here because the 26.2
 * Blaze3D RenderPipeline / RenderTarget APIs changed substantially. Settings,
 * scale-factor reporting, and Iris detection still work so the Graphics Hub
 * remains usable; the actual low-res world pass will be reintroduced once the
 * new pipeline hooks are wired.
 */
public final class UpscalePipeline {
    private static final UpscalePipeline INSTANCE = new UpscalePipeline();

    private boolean shouldScale;
    private boolean loggedStub;

    private UpscalePipeline() {
    }

    public static UpscalePipeline get() {
        return INSTANCE;
    }

    public UpscaleSettings settings() {
        return ConfigManager.getConfig().getUpscale();
    }

    public boolean isIrisLoaded() {
        return IrisUpscaleCompat.isIrisLoaded();
    }

    public boolean isBlockedByIrisShaders() {
        return IrisUpscaleCompat.isShaderPackInUse();
    }

    public float getActiveScale() {
        UpscaleSettings settings = settings();
        if (!settings.isEnabled()) {
            return 1.0F;
        }
        return Math.max(0.25F, Math.min(1.0F, settings.getScale()));
    }

    public double getCurrentScaleFactor() {
        // Stub: do not report a reduced framebuffer size until GPU path is ported.
        return 1.0D;
    }

    public void onResolutionChanged() {
        // no-op stub
    }

    public void notifySettingsChanged() {
        if (!loggedStub && settings().isEnabled()) {
            loggedStub = true;
            OleafClient.LOGGER.info(
                    "Upscale settings saved (algo={} scale={}) — GPU upscale pass is stubbed on 26.2 for now",
                    settings().getAlgorithm().getDisplayName(),
                    getActiveScale()
            );
        }
        if (IrisUpscaleCompat.isShaderPackInUse()) {
            IrisUpscaleCompat.requestPipelineRebuild("upscale settings changed");
        }
    }

    public void setShouldScale(boolean enable) {
        shouldScale = enable && getActiveScale() < 0.999F;
    }

    public boolean isShouldScale() {
        return shouldScale;
    }
}
