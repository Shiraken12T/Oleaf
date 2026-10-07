package com.shiraken.optimizationcompanion.config;

import com.shiraken.optimizationcompanion.upscale.UpscaleAlgorithm;
import com.shiraken.optimizationcompanion.upscale.UpscaleQuality;
import com.shiraken.optimizationcompanion.upscale.UpscaleSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.particle.ParticlesMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable graphics hub state: a preset plus simplified quality knobs.
 */
public final class GraphicsProfile {
    private Preset preset = Preset.BALANCED_SURVIVAL;
    private GraphicsQuality graphicsQuality = GraphicsQuality.MEDIUM;
    private int renderDistance = 12;
    private int simulationDistance = 8;
    private int entityDistancePercent = 100;
    private int maxFps = GameOptions.MAX_FPS_LIMIT;
    private boolean vsync = true;
    private boolean entityShadows = true;
    private boolean viewBobbing = true;
    private double brightness = 0.5D;
    private StabilityBias stabilityBias = StabilityBias.BALANCED;
    private VisualEffectsLevel visualEffects = VisualEffectsLevel.REDUCED;
    private ParticleDetail particleDetail = ParticleDetail.DECREASED;
    private CloudDetail cloudDetail = CloudDetail.FAST;
    private UpscaleQuality upscaleQuality = UpscaleQuality.OFF;
    private float upscaleSharpen = 0.2F;
    private UpscaleAlgorithm upscaleAlgorithm = UpscaleAlgorithm.FSR;
    private int upscaleCustomScalePercent = 67;
    private boolean upscaleForceWithIris = false;
    private int fov = 70;
    private int guiScale = 0;
    private boolean fullscreen = false;

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
        this.maxFps = Math.max(10, Math.min(GameOptions.MAX_FPS_LIMIT, maxFps));
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

    public UpscaleQuality getUpscaleQuality() {
        return upscaleQuality;
    }

    public void setUpscaleQuality(UpscaleQuality upscaleQuality) {
        this.upscaleQuality = upscaleQuality == null ? UpscaleQuality.OFF : upscaleQuality;
    }

    public float getUpscaleSharpen() {
        return upscaleSharpen;
    }

    public void setUpscaleSharpen(float upscaleSharpen) {
        this.upscaleSharpen = Math.max(0.0F, Math.min(1.0F, upscaleSharpen));
    }

    public UpscaleAlgorithm getUpscaleAlgorithm() {
        return upscaleAlgorithm;
    }

    public void setUpscaleAlgorithm(UpscaleAlgorithm upscaleAlgorithm) {
        this.upscaleAlgorithm = upscaleAlgorithm == null ? UpscaleAlgorithm.FSR : upscaleAlgorithm;
    }

    public int getUpscaleCustomScalePercent() {
        return upscaleCustomScalePercent;
    }

    public void setUpscaleCustomScalePercent(int upscaleCustomScalePercent) {
        this.upscaleCustomScalePercent = Math.max(30, Math.min(100, upscaleCustomScalePercent));
    }

    public int getFov() {
        return fov;
    }

    public void setFov(int fov) {
        this.fov = Math.max(30, Math.min(110, fov));
    }

    public int getGuiScale() {
        return guiScale;
    }

    public void setGuiScale(int guiScale) {
        this.guiScale = Math.max(0, Math.min(6, guiScale));
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    public boolean isUpscaleForceWithIris() {
        return upscaleForceWithIris;
    }

    public void setUpscaleForceWithIris(boolean upscaleForceWithIris) {
        this.upscaleForceWithIris = upscaleForceWithIris;
    }

    public void applyPresetDefaults(Preset preset) {
        this.preset = preset == null ? Preset.BALANCED_SURVIVAL : preset;

        switch (this.preset) {
            case LOW_END_LAPTOP -> {
                graphicsQuality = GraphicsQuality.LOW;
                renderDistance = 8;
                simulationDistance = 6;
                entityDistancePercent = 75;
                maxFps = 60;
                vsync = true;
                entityShadows = false;
                viewBobbing = false;
                brightness = 0.5D;
                stabilityBias = StabilityBias.STABILITY;
                visualEffects = VisualEffectsLevel.MINIMAL;
                particleDetail = ParticleDetail.MINIMAL;
                cloudDetail = CloudDetail.OFF;
                upscaleQuality = UpscaleQuality.ULTRA_PERFORMANCE;
                upscaleSharpen = 0.30F;
            }
            case BALANCED_SURVIVAL -> {
                graphicsQuality = GraphicsQuality.MEDIUM;
                renderDistance = 12;
                simulationDistance = 8;
                entityDistancePercent = 100;
                maxFps = GameOptions.MAX_FPS_LIMIT;
                vsync = true;
                entityShadows = true;
                viewBobbing = true;
                brightness = 0.5D;
                stabilityBias = StabilityBias.BALANCED;
                visualEffects = VisualEffectsLevel.REDUCED;
                particleDetail = ParticleDetail.DECREASED;
                cloudDetail = CloudDetail.FAST;
                upscaleQuality = UpscaleQuality.OFF;
                upscaleSharpen = 0.2F;
            }
            case PVP_COMPETITIVE -> {
                graphicsQuality = GraphicsQuality.MEDIUM;
                renderDistance = 10;
                simulationDistance = 8;
                entityDistancePercent = 100;
                maxFps = GameOptions.MAX_FPS_LIMIT;
                vsync = false;
                entityShadows = false;
                viewBobbing = false;
                brightness = 0.55D;
                stabilityBias = StabilityBias.PERFORMANCE;
                visualEffects = VisualEffectsLevel.MINIMAL;
                particleDetail = ParticleDetail.MINIMAL;
                cloudDetail = CloudDetail.OFF;
                upscaleQuality = UpscaleQuality.OFF;
                upscaleSharpen = 0.2F;
            }
            case BUILDER_SHADERS -> {
                graphicsQuality = GraphicsQuality.HIGH;
                renderDistance = 16;
                simulationDistance = 12;
                entityDistancePercent = 125;
                maxFps = GameOptions.MAX_FPS_LIMIT;
                vsync = false;
                entityShadows = true;
                viewBobbing = true;
                brightness = 0.5D;
                stabilityBias = StabilityBias.BALANCED;
                visualEffects = VisualEffectsLevel.FULL;
                particleDetail = ParticleDetail.ALL;
                cloudDetail = CloudDetail.FANCY;
                upscaleQuality = UpscaleQuality.OFF;
                upscaleSharpen = 0.2F;
            }
        }
    }

    public void writeTo(GraphicsSettings settings) {
        settings.setPreset(preset);
        settings.setGraphicsQuality(graphicsQuality);
        settings.setRenderDistance(renderDistance);
        settings.setSimulationDistance(simulationDistance);
        settings.setEntityDistancePercent(entityDistancePercent);
        settings.setMaxFps(maxFps);
        settings.setVsync(vsync);
        settings.setEntityShadows(entityShadows);
        settings.setViewBobbing(viewBobbing);
        settings.setBrightness(brightness);
        settings.setStabilityBias(stabilityBias);
        settings.setVisualEffects(visualEffects);
        settings.setParticleDetail(particleDetail);
        settings.setCloudDetail(cloudDetail);
        settings.setHasAppliedOnce(true);
    }

    public void writeUpscaleTo(UpscaleSettings settings) {
        if (settings == null) {
            return;
        }
        settings.setQuality(upscaleQuality);
        settings.setSharpen(upscaleSharpen);
        settings.setAlgorithm(upscaleAlgorithm);
        settings.setCustomScalePercent(upscaleCustomScalePercent);
        settings.setForceWithIris(upscaleForceWithIris);
    }

    public void readFrom(GraphicsSettings settings) {
        if (settings == null) {
            return;
        }
        settings.sanitize();
        preset = settings.getPreset();
        graphicsQuality = settings.getGraphicsQuality();
        renderDistance = settings.getRenderDistance();
        simulationDistance = settings.getSimulationDistance();
        entityDistancePercent = settings.getEntityDistancePercent();
        maxFps = settings.getMaxFps();
        vsync = settings.isVsync();
        entityShadows = settings.isEntityShadows();
        viewBobbing = settings.isViewBobbing();
        brightness = settings.getBrightness();
        stabilityBias = settings.getStabilityBias();
        visualEffects = settings.getVisualEffects();
        particleDetail = settings.getParticleDetail();
        cloudDetail = settings.getCloudDetail();
    }

    public void readUpscaleFrom(UpscaleSettings settings) {
        if (settings == null) {
            return;
        }
        settings.sanitize();
        upscaleQuality = settings.getQuality();
        upscaleSharpen = settings.getSharpen();
        upscaleAlgorithm = settings.getAlgorithm();
        upscaleCustomScalePercent = settings.getCustomScalePercent();
        upscaleForceWithIris = settings.isForceWithIris();
    }

    /**
     * Overlay live Minecraft numeric/toggle options so the hub matches what is active.
     * Qualitative knobs (preset / quality / priority / extra details) stay from saved config
     * because vanilla GraphicsMode cannot express Ultra vs High uniquely.
     */
    public void syncFromGameOptions() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return;
        }

        GameOptions options = client.options;
        renderDistance = options.getViewDistance().getValue();
        simulationDistance = options.getSimulationDistance().getValue();
        entityDistancePercent = (int) Math.round(options.getEntityDistanceScaling().getValue() * 100.0D);
        maxFps = Math.min(GameOptions.MAX_FPS_LIMIT, Math.max(10, options.getMaxFps().getValue()));
        vsync = options.getEnableVsync().getValue();
        entityShadows = options.getEntityShadows().getValue();
        viewBobbing = options.getBobView().getValue();
        brightness = options.getGamma().getValue();
        particleDetail = fromParticles(options.getParticles().getValue());
        cloudDetail = fromClouds(options.getCloudRenderMode().getValue());
        fov = options.getFov().getValue();
        guiScale = options.getGuiScale().getValue();
        fullscreen = options.getFullscreen().getValue();

        // Only refresh quality from vanilla when we have no saved applied profile yet.
        if (graphicsQuality == null) {
            graphicsQuality = fromGraphicsMode(options.getPreset().getValue());
        }
    }

    private static GraphicsQuality fromGraphicsMode(GraphicsMode mode) {
        if (mode == null) {
            return GraphicsQuality.MEDIUM;
        }
        return switch (mode) {
            case FAST -> GraphicsQuality.LOW;
            case FANCY -> GraphicsQuality.MEDIUM;
            case FABULOUS -> GraphicsQuality.HIGH;
            default -> GraphicsQuality.MEDIUM;
        };
    }

    private static ParticleDetail fromParticles(ParticlesMode mode) {
        if (mode == null) {
            return ParticleDetail.DECREASED;
        }
        return switch (mode) {
            case MINIMAL -> ParticleDetail.MINIMAL;
            case DECREASED -> ParticleDetail.DECREASED;
            case ALL -> ParticleDetail.ALL;
            default -> ParticleDetail.DECREASED;
        };
    }

    private static CloudDetail fromClouds(CloudRenderMode mode) {
        if (mode == null) {
            return CloudDetail.FAST;
        }
        return switch (mode) {
            case OFF -> CloudDetail.OFF;
            case FAST -> CloudDetail.FAST;
            case FANCY -> CloudDetail.FANCY;
            default -> CloudDetail.FAST;
        };
    }

    public List<String> describeChanges() {
        List<String> lines = new ArrayList<>();
        String fpsLabel = maxFps >= GameOptions.MAX_FPS_LIMIT ? "Unlimited" : (maxFps + " FPS cap");
        lines.add(preset.getDisplayName()
                + "  |  "
                + graphicsQuality.getDisplayName()
                + "  |  "
                + renderDistance
                + " chunks  |  "
                + fpsLabel);
        for (String line : preset.getSummary()) {
            lines.add("• " + line);
        }
        return lines;
    }

    public static GraphicsProfile fromConfig(CompanionConfig config) {
        GraphicsProfile profile = new GraphicsProfile();
        if (config.getGraphics() != null && config.getGraphics().hasAppliedOnce()) {
            profile.readFrom(config.getGraphics());
            profile.readUpscaleFrom(config.getUpscale());
        } else {
            profile.applyPresetDefaults(config.getSelectedPreset());
        }
        // Always overlay live Minecraft values so returning to the hub shows reality.
        profile.syncFromGameOptions();
        return profile;
    }
}
