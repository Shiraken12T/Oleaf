package com.shiraken.optimizationcompanion.settings;

import com.shiraken.optimizationcompanion.OleafClient;
import com.shiraken.optimizationcompanion.config.CloudDetail;
import com.shiraken.optimizationcompanion.config.ConfigManager;
import com.shiraken.optimizationcompanion.config.GraphicsProfile;
import com.shiraken.optimizationcompanion.config.GraphicsQuality;
import com.shiraken.optimizationcompanion.config.ParticleDetail;
import com.shiraken.optimizationcompanion.config.Preset;
import com.shiraken.optimizationcompanion.config.StabilityBias;
import com.shiraken.optimizationcompanion.config.VisualEffectsLevel;
import com.shiraken.optimizationcompanion.upscale.UpscalePipeline;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;

/**
 * Applies Graphics Hub profiles to Minecraft Options and optional Sodium settings.
 */
public final class SettingsApplier {
    private static OptionsBackup sessionBackup;

    private SettingsApplier() {
    }

    public static boolean hasSessionBackup() {
        return sessionBackup != null;
    }

    public static void restoreSessionBackup() {
        Minecraft client = Minecraft.getInstance();
        if (sessionBackup == null || client == null) {
            return;
        }
        sessionBackup.restore(client.options);
        OleafClient.LOGGER.info("Restored previous Minecraft/Sodium video settings from session backup");
    }

    public static void apply(GraphicsProfile profile) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || profile == null) {
            return;
        }

        Options options = client.options;
        if (sessionBackup == null) {
            sessionBackup = OptionsBackup.capture(options);
            OleafClient.LOGGER.info("Captured one-shot video settings backup for this session");
        }

        applyMinecraftOptions(client, options, profile);
        SodiumSettingsBridge.applyPreset(profile.getPreset());
        options.save();

        ConfigManager.getConfig().setSelectedPreset(profile.getPreset());
        profile.writeTo(ConfigManager.getConfig().getGraphics());
        profile.writeUpscaleTo(ConfigManager.getConfig().getUpscale());
        ConfigManager.save();
        UpscalePipeline.get().notifySettingsChanged();

        OleafClient.LOGGER.info(
                "Applied graphics profile '{}' (quality={}, renderDistance={}, sim={}, entities={}%, maxFps={}, vsync={}, sodium={})",
                profile.getPreset().getDisplayName(),
                profile.getGraphicsQuality(),
                profile.getRenderDistance(),
                profile.getSimulationDistance(),
                profile.getEntityDistancePercent(),
                profile.getMaxFps(),
                profile.isVsync(),
                SodiumSettingsBridge.isAvailable()
        );
    }

    private static void applyMinecraftOptions(Minecraft client, Options options, GraphicsProfile profile) {
        GraphicsPreset mode = switch (profile.getGraphicsQuality()) {
            case LOW -> GraphicsPreset.FAST;
            case MEDIUM -> GraphicsPreset.FANCY;
            case HIGH, ULTRA -> GraphicsPreset.FABULOUS;
        };

        options.applyGraphicsPreset(mode);
        options.graphicsPreset().set(mode);

        options.renderDistance().set(profile.getRenderDistance());
        options.simulationDistance().set(profile.getSimulationDistance());
        options.entityDistanceScaling().set(profile.getEntityDistancePercent() / 100.0D);
        options.framerateLimit().set(profile.getMaxFps());
        options.enableVsync().set(profile.isVsync());
        options.entityShadows().set(profile.isEntityShadows());
        options.bobView().set(profile.isViewBobbing());
        options.gamma().set(profile.getBrightness());
        options.fov().set(profile.getFov());
        options.guiScale().set(profile.getGuiScale());

        boolean wantFullscreen = profile.isFullscreen();
        options.fullscreen().set(wantFullscreen);
        if (client.getWindow() != null && client.getWindow().isFullscreen() != wantFullscreen) {
            client.getWindow().toggleFullScreen();
            options.fullscreen().set(client.getWindow().isFullscreen());
        }

        applyParticles(options, profile.getParticleDetail());
        applyClouds(options, profile.getCloudDetail());
        applyVisualEffectsExtras(options, profile.getVisualEffects(), profile.getGraphicsQuality());
        applyStabilityBiasExtras(options, profile.getStabilityBias(), profile);

        if (profile.getGraphicsQuality() == GraphicsQuality.ULTRA) {
            options.biomeBlendRadius().set(Math.max(options.biomeBlendRadius().get(), 3));
            options.mipmapLevels().set(4);
        } else if (profile.getGraphicsQuality() == GraphicsQuality.LOW) {
            options.biomeBlendRadius().set(Math.min(options.biomeBlendRadius().get(), 1));
            options.mipmapLevels().set(2);
        }

        client.resizeGui();
    }

    private static void applyParticles(Options options, ParticleDetail detail) {
        ParticleStatus mode = switch (detail) {
            case MINIMAL -> ParticleStatus.MINIMAL;
            case DECREASED -> ParticleStatus.DECREASED;
            case ALL -> ParticleStatus.ALL;
        };
        options.particles().set(mode);
    }

    private static void applyClouds(Options options, CloudDetail detail) {
        CloudStatus mode = switch (detail) {
            case OFF -> CloudStatus.OFF;
            case FAST -> CloudStatus.FAST;
            case FANCY -> CloudStatus.FANCY;
        };
        options.cloudStatus().set(mode);
    }

    private static void applyVisualEffectsExtras(Options options, VisualEffectsLevel effects, GraphicsQuality quality) {
        switch (effects) {
            case MINIMAL -> {
                options.cutoutLeaves().set(false);
                options.ambientOcclusion().set(false);
                options.vignette().set(false);
            }
            case REDUCED -> {
                options.cutoutLeaves().set(quality.ordinal() >= GraphicsQuality.MEDIUM.ordinal());
                options.ambientOcclusion().set(quality != GraphicsQuality.LOW);
                options.vignette().set(true);
            }
            case FULL -> {
                options.cutoutLeaves().set(true);
                options.ambientOcclusion().set(true);
                options.vignette().set(true);
            }
        }
    }

    private static void applyStabilityBiasExtras(Options options, StabilityBias bias, GraphicsProfile profile) {
        if (bias == StabilityBias.PERFORMANCE && profile.getPreset() == Preset.PVP_COMPETITIVE) {
            options.bobView().set(false);
            options.entityShadows().set(false);
        }
    }
}
