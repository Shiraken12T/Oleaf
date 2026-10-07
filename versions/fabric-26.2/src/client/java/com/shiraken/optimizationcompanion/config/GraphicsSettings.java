package com.shiraken.optimizationcompanion.config;

import net.minecraft.client.Options;

/**
 * Persisted Graphics Hub knobs so the UI restores the last applied values.
 */
public class GraphicsSettings {
    private Preset preset = Preset.BALANCED_SURVIVAL;
    private GraphicsQuality graphicsQuality = GraphicsQuality.MEDIUM;
    private int renderDistance = 12;
    private int simulationDistance = 8;
    private int entityDistancePercent = 100;
    private int maxFps = 120;
    private boolean vsync = true;
    private boolean entityShadows = true;
    private boolean viewBobbing = true;
    private double brightness = 0.5D;
    private StabilityBias stabilityBias = StabilityBias.BALANCED;
    private VisualEffectsLevel visualEffects = VisualEffectsLevel.REDUCED;
    private ParticleDetail particleDetail = ParticleDetail.DECREASED;
    private CloudDetail cloudDetail = CloudDetail.FAST;
    private boolean hasAppliedOnce = false;

    public Preset getPreset() {
        return preset;
    }

    public void setPreset(Preset preset) {
        this.preset = preset == null ? Preset.BALANCED_SURVIVAL : preset;
    }

    public GraphicsQuality getGraphicsQuality() {
        return graphicsQuality;
    }

    public void setGraphicsQuality(GraphicsQuality graphicsQuality) {
        this.graphicsQuality = graphicsQuality == null ? GraphicsQuality.MEDIUM : graphicsQuality;
    }

    public int getRenderDistance() {
        return renderDistance;
    }

    public void setRenderDistance(int renderDistance) {
        this.renderDistance = Math.max(2, Math.min(32, renderDistance));
    }

    public int getSimulationDistance() {
        return simulationDistance;
    }

    public void setSimulationDistance(int simulationDistance) {
        this.simulationDistance = Math.max(5, Math.min(32, simulationDistance));
    }

    public int getEntityDistancePercent() {
        return entityDistancePercent;
    }

    public void setEntityDistancePercent(int entityDistancePercent) {
        this.entityDistancePercent = Math.max(50, Math.min(500, entityDistancePercent));
    }

    public int getMaxFps() {
        return maxFps;
    }

    public void setMaxFps(int maxFps) {
        this.maxFps = Math.max(10, Math.min(Options.UNLIMITED_FRAMERATE_CUTOFF, maxFps));
    }

    public boolean isVsync() {
        return vsync;
    }

    public void setVsync(boolean vsync) {
        this.vsync = vsync;
    }

    public boolean isEntityShadows() {
        return entityShadows;
    }

    public void setEntityShadows(boolean entityShadows) {
        this.entityShadows = entityShadows;
    }

    public boolean isViewBobbing() {
        return viewBobbing;
    }

    public void setViewBobbing(boolean viewBobbing) {
        this.viewBobbing = viewBobbing;
    }

    public double getBrightness() {
        return brightness;
    }

    public void setBrightness(double brightness) {
        this.brightness = Math.max(0.0D, Math.min(1.0D, brightness));
    }

    public StabilityBias getStabilityBias() {
        return stabilityBias;
    }

    public void setStabilityBias(StabilityBias stabilityBias) {
        this.stabilityBias = stabilityBias == null ? StabilityBias.BALANCED : stabilityBias;
    }

    public VisualEffectsLevel getVisualEffects() {
        return visualEffects;
    }

    public void setVisualEffects(VisualEffectsLevel visualEffects) {
        this.visualEffects = visualEffects == null ? VisualEffectsLevel.REDUCED : visualEffects;
    }

    public ParticleDetail getParticleDetail() {
        return particleDetail;
    }

    public void setParticleDetail(ParticleDetail particleDetail) {
        this.particleDetail = particleDetail == null ? ParticleDetail.DECREASED : particleDetail;
    }

    public CloudDetail getCloudDetail() {
        return cloudDetail;
    }

    public void setCloudDetail(CloudDetail cloudDetail) {
        this.cloudDetail = cloudDetail == null ? CloudDetail.FAST : cloudDetail;
    }

    public boolean hasAppliedOnce() {
        return hasAppliedOnce;
    }

    public void setHasAppliedOnce(boolean hasAppliedOnce) {
        this.hasAppliedOnce = hasAppliedOnce;
    }

    public void sanitize() {
        if (preset == null) {
            preset = Preset.BALANCED_SURVIVAL;
        }
        if (graphicsQuality == null) {
            graphicsQuality = GraphicsQuality.MEDIUM;
        }
        if (stabilityBias == null) {
            stabilityBias = StabilityBias.BALANCED;
        }
        if (visualEffects == null) {
            visualEffects = VisualEffectsLevel.REDUCED;
        }
        if (particleDetail == null) {
            particleDetail = ParticleDetail.DECREASED;
        }
        if (cloudDetail == null) {
            cloudDetail = CloudDetail.FAST;
        }
        setRenderDistance(renderDistance);
        setSimulationDistance(simulationDistance);
        setEntityDistancePercent(entityDistancePercent);
        setMaxFps(maxFps);
        setBrightness(brightness);
    }
}
