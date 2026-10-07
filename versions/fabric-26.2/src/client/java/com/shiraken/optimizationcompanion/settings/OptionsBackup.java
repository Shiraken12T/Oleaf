package com.shiraken.optimizationcompanion.settings;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;

/**
 * One-shot snapshot of Minecraft video-related options for session restore.
 */
public final class OptionsBackup {
    private final int viewDistance;
    private final int simulationDistance;
    private final GraphicsPreset graphicsMode;
    private final ParticleStatus particles;
    private final CloudStatus clouds;
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
            GraphicsPreset graphicsMode,
            ParticleStatus particles,
            CloudStatus clouds,
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

    public static OptionsBackup capture(Options options) {
        return new OptionsBackup(
                options.renderDistance().get(),
                options.simulationDistance().get(),
                options.graphicsPreset().get(),
                options.particles().get(),
                options.cloudStatus().get(),
                options.ambientOcclusion().get(),
                options.entityShadows().get(),
                options.enableVsync().get(),
                options.framerateLimit().get(),
                options.biomeBlendRadius().get(),
                options.mipmapLevels().get(),
                options.entityDistanceScaling().get(),
                options.bobView().get(),
                options.cutoutLeaves().get(),
                SodiumSettingsBridge.captureBackup()
        );
    }

    public void restore(Options options) {
        options.renderDistance().set(viewDistance);
        options.simulationDistance().set(simulationDistance);
        options.graphicsPreset().set(graphicsMode);
        options.particles().set(particles);
        options.cloudStatus().set(clouds);
        options.ambientOcclusion().set(ao);
        options.entityShadows().set(entityShadows);
        options.enableVsync().set(vsync);
        options.framerateLimit().set(maxFps);
        options.biomeBlendRadius().set(biomeBlendRadius);
        options.mipmapLevels().set(mipmapLevels);
        options.entityDistanceScaling().set(entityDistanceScaling);
        options.bobView().set(bobView);
        options.cutoutLeaves().set(cutoutLeaves);
        SodiumSettingsBridge.restoreBackup(sodiumBackup);
        options.save();
    }

    public boolean hasSodiumBackup() {
        return sodiumBackup != null;
    }
}
