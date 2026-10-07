package com.shiraken.optimizationcompanion.settings;

import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.particle.ParticlesMode;

/**
 * One-shot snapshot of Minecraft video-related options for session restore.
 */
public final class OptionsBackup {
    private final int viewDistance;
    private final int simulationDistance;
    private final GraphicsMode graphicsMode;
    private final ParticlesMode particles;
    private final CloudRenderMode clouds;
    private final boolean ao;
    private final boolean entityShadows;
    private final boolean vsync;
    private final int maxFps;
    private final int biomeBlendRadius;
    private final int mipmapLevels;
    private final double entityDistanceScaling;
    private final boolean bobView;
    private final boolean cutoutLeaves;
    private final Object sodiumBackup;

    private OptionsBackup(
            int viewDistance,
            int simulationDistance,
            GraphicsMode graphicsMode,
            ParticlesMode particles,
            CloudRenderMode clouds,
            boolean ao,
            boolean entityShadows,
            boolean vsync,
            int maxFps,
            int biomeBlendRadius,
            int mipmapLevels,
            double entityDistanceScaling,
            boolean bobView,
            boolean cutoutLeaves,
            Object sodiumBackup
    ) {
        this.viewDistance = viewDistance;
        this.simulationDistance = simulationDistance;
        this.graphicsMode = graphicsMode;
        this.particles = particles;
        this.clouds = clouds;
        this.ao = ao;
        this.entityShadows = entityShadows;
        this.vsync = vsync;
        this.maxFps = maxFps;
        this.biomeBlendRadius = biomeBlendRadius;
        this.mipmapLevels = mipmapLevels;
        this.entityDistanceScaling = entityDistanceScaling;
        this.bobView = bobView;
        this.cutoutLeaves = cutoutLeaves;
        this.sodiumBackup = sodiumBackup;
    }

    public static OptionsBackup capture(GameOptions options) {
        return new OptionsBackup(
                options.getViewDistance().getValue(),
                options.getSimulationDistance().getValue(),
                options.getPreset().getValue(),
                options.getParticles().getValue(),
                options.getCloudRenderMode().getValue(),
                options.getAo().getValue(),
                options.getEntityShadows().getValue(),
                options.getEnableVsync().getValue(),
                options.getMaxFps().getValue(),
                options.getBiomeBlendRadius().getValue(),
                options.getMipmapLevels().getValue(),
                options.getEntityDistanceScaling().getValue(),
                options.getBobView().getValue(),
                options.getCutoutLeaves().getValue(),
                SodiumSettingsBridge.captureBackup()
        );
    }

    public void restore(GameOptions options) {
        options.getViewDistance().setValue(viewDistance);
        options.getSimulationDistance().setValue(simulationDistance);
        options.getPreset().setValue(graphicsMode);
        options.getParticles().setValue(particles);
        options.getCloudRenderMode().setValue(clouds);
        options.getAo().setValue(ao);
        options.getEntityShadows().setValue(entityShadows);
        options.getEnableVsync().setValue(vsync);
        options.getMaxFps().setValue(maxFps);
        options.getBiomeBlendRadius().setValue(biomeBlendRadius);
        options.getMipmapLevels().setValue(mipmapLevels);
        options.getEntityDistanceScaling().setValue(entityDistanceScaling);
        options.getBobView().setValue(bobView);
        options.getCutoutLeaves().setValue(cutoutLeaves);
        SodiumSettingsBridge.restoreBackup(sodiumBackup);
        options.write();
    }

    public boolean hasSodiumBackup() {
        return sodiumBackup != null;
    }
}
