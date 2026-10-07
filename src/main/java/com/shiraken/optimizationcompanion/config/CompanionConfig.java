package com.shiraken.optimizationcompanion.config;

import com.shiraken.optimizationcompanion.upscale.UpscaleQuality;
import com.shiraken.optimizationcompanion.upscale.UpscaleSettings;

public class CompanionConfig {
    private Preset selectedPreset = Preset.BALANCED_SURVIVAL;
    private boolean advancedMode = false;
    private OverlaySettings overlay = new OverlaySettings();
    private GraphicsSettings graphics = new GraphicsSettings();
    private UpscaleSettings upscale = new UpscaleSettings();
    private FrameGenSettings frameGen = new FrameGenSettings();

    public Preset getSelectedPreset() {
        return selectedPreset;
    }

    public void setSelectedPreset(Preset selectedPreset) {
        this.selectedPreset = selectedPreset == null ? Preset.BALANCED_SURVIVAL : selectedPreset;
        if (graphics != null) {
            graphics.setPreset(this.selectedPreset);
        }
    }

    public boolean isAdvancedMode() {
        return advancedMode;
    }

    public void setAdvancedMode(boolean advancedMode) {
        this.advancedMode = advancedMode;
    }

    public OverlaySettings getOverlay() {
        return overlay;
    }

    public GraphicsSettings getGraphics() {
        return graphics;
    }

    public UpscaleSettings getUpscale() {
        return upscale;
    }

    public FrameGenSettings getFrameGen() {
        return frameGen;
    }

    public void sanitize() {
        if (selectedPreset == null) {
            selectedPreset = Preset.BALANCED_SURVIVAL;
        }
        if (overlay == null) {
            overlay = new OverlaySettings();
        }
        if (graphics == null) {
            graphics = new GraphicsSettings();
        }
        if (upscale == null) {
            upscale = new UpscaleSettings();
        }
        if (frameGen == null) {
            frameGen = new FrameGenSettings();
        }
        overlay.sanitize();
        graphics.sanitize();
        upscale.sanitize();
        frameGen.sanitize();
        if (!graphics.hasAppliedOnce()) {
            // First launch: seed graphics knobs from the selected preset.
            GraphicsProfile seed = new GraphicsProfile();
            seed.applyPresetDefaults(selectedPreset);
            seed.writeTo(graphics);
            applyUpscaleForPreset(selectedPreset);
        } else {
            selectedPreset = graphics.getPreset();
        }
    }

    public void applyUpscaleForPreset(Preset preset) {
        if (upscale == null) {
            upscale = new UpscaleSettings();
        }
        switch (preset) {
            case LOW_END_LAPTOP -> {
                upscale.setQuality(UpscaleQuality.ULTRA_PERFORMANCE);
                upscale.setSharpen(0.30F);
            }
            case BALANCED_SURVIVAL -> {
                upscale.setQuality(UpscaleQuality.OFF);
                upscale.setSharpen(0.2F);
            }
            case PVP_COMPETITIVE, BUILDER_SHADERS -> {
                upscale.setQuality(UpscaleQuality.OFF);
                upscale.setSharpen(0.2F);
            }
        }
    }

    public void applyPreset(Preset preset) {
        selectedPreset = preset;
        applyUpscaleForPreset(preset);

        switch (preset) {
            case LOW_END_LAPTOP -> {
                advancedMode = false;
                overlay.enabled = true;
                overlay.anchor = OverlayAnchor.TOP_LEFT;
                overlay.scale = 0.85F;
                overlay.opacity = 0.8F;
                overlay.showFps = true;
                overlay.showFrameTime = true;
                overlay.showMemory = true;
                overlay.showPing = false;
                overlay.showEntities = true;
                overlay.showFsr = true;
                overlay.showFrameGen = true;
                overlay.showOptimizedCore = true;
            }
            case BALANCED_SURVIVAL -> {
                overlay.enabled = true;
                overlay.anchor = OverlayAnchor.TOP_LEFT;
                overlay.scale = 1.0F;
                overlay.opacity = 0.75F;
                overlay.showFps = true;
                overlay.showFrameTime = true;
                overlay.showMemory = true;
                overlay.showPing = true;
                overlay.showEntities = true;
                overlay.showFsr = true;
                overlay.showFrameGen = true;
                overlay.showOptimizedCore = true;
            }
            case PVP_COMPETITIVE -> {
                overlay.enabled = true;
                overlay.anchor = OverlayAnchor.TOP_RIGHT;
                overlay.scale = 0.9F;
                overlay.opacity = 0.7F;
                overlay.showFps = true;
                overlay.showFrameTime = true;
                overlay.showMemory = false;
                overlay.showPing = true;
                overlay.showEntities = false;
                overlay.showFsr = false;
                overlay.showFrameGen = false;
                overlay.showOptimizedCore = true;
            }
            case BUILDER_SHADERS -> {
                overlay.enabled = true;
                overlay.anchor = OverlayAnchor.BOTTOM_LEFT;
                overlay.scale = 1.0F;
                overlay.opacity = 0.75F;
                overlay.showFps = true;
                overlay.showFrameTime = true;
                overlay.showMemory = true;
                overlay.showPing = false;
                overlay.showEntities = true;
                overlay.showFsr = true;
                overlay.showFrameGen = true;
                overlay.showOptimizedCore = true;
            }
        }
    }

    public static class OverlaySettings {
        private boolean enabled = true;
        private OverlayAnchor anchor = OverlayAnchor.TOP_LEFT;
        private int offsetX = 8;
        private int offsetY = 8;
        private float scale = 1.0F;
        private float opacity = 0.75F;
        private boolean showFps = true;
        private boolean showFrameTime = true;
        private boolean showMemory = true;
        private boolean showPing = true;
        private boolean showEntities = true;
        private boolean showFsr = true;
        private boolean showFrameGen = true;
        private Boolean showOptimizedCore = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public OverlayAnchor getAnchor() {
            return anchor;
        }

        public void setAnchor(OverlayAnchor anchor) {
            this.anchor = anchor == null ? OverlayAnchor.TOP_LEFT : anchor;
        }

        public int getOffsetX() {
            return offsetX;
        }

        public void setOffsetX(int offsetX) {
            this.offsetX = offsetX;
        }

        public int getOffsetY() {
            return offsetY;
        }

        public void setOffsetY(int offsetY) {
            this.offsetY = offsetY;
        }

        public float getScale() {
            return scale;
        }

        public void setScale(float scale) {
            this.scale = Math.max(0.5F, Math.min(2.0F, scale));
        }

        public float getOpacity() {
            return opacity;
        }

        public void setOpacity(float opacity) {
            this.opacity = Math.max(0.2F, Math.min(1.0F, opacity));
        }

        public boolean isShowFps() {
            return showFps;
        }

        public void setShowFps(boolean showFps) {
            this.showFps = showFps;
        }

        public boolean isShowFrameTime() {
            return showFrameTime;
        }

        public void setShowFrameTime(boolean showFrameTime) {
            this.showFrameTime = showFrameTime;
        }

        public boolean isShowMemory() {
            return showMemory;
        }

        public void setShowMemory(boolean showMemory) {
            this.showMemory = showMemory;
        }

        public boolean isShowPing() {
            return showPing;
        }

        public void setShowPing(boolean showPing) {
            this.showPing = showPing;
        }

        public boolean isShowEntities() {
            return showEntities;
        }

        public void setShowEntities(boolean showEntities) {
            this.showEntities = showEntities;
        }

        public boolean isShowFsr() {
            return showFsr;
        }

        public void setShowFsr(boolean showFsr) {
            this.showFsr = showFsr;
        }

        public boolean isShowFrameGen() {
            return showFrameGen;
        }

        public void setShowFrameGen(boolean showFrameGen) {
            this.showFrameGen = showFrameGen;
        }

        public boolean isShowOptimizedCore() {
            return showOptimizedCore == null || showOptimizedCore;
        }

        public void setShowOptimizedCore(boolean showOptimizedCore) {
            this.showOptimizedCore = showOptimizedCore;
        }

        public void resetPosition() {
            anchor = OverlayAnchor.TOP_LEFT;
            offsetX = 8;
            offsetY = 8;
        }

        private void sanitize() {
            if (anchor == null) {
                anchor = OverlayAnchor.TOP_LEFT;
            }
            setScale(scale);
            setOpacity(opacity);
            if (showOptimizedCore == null) {
                showOptimizedCore = true;
            }
        }
    }
}
