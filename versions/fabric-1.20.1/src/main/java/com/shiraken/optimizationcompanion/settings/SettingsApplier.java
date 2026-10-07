package com.shiraken.optimizationcompanion.settings;

import com.shiraken.optimizationcompanion.OleafClient;
import com.shiraken.optimizationcompanion.config.CloudDetail;
import com.shiraken.optimizationcompanion.config.ConfigManager;
import com.shiraken.optimizationcompanion.config.GraphicsProfile;
import com.shiraken.optimizationcompanion.config.GraphicsQuality;
import com.shiraken.optimizationcompanion.config.ParticleDetail;
import com.shiraken.optimizationcompanion.config.StabilityBias;
import com.shiraken.optimizationcompanion.config.VisualEffectsLevel;
import com.shiraken.optimizationcompanion.upscale.UpscalePipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;

/**
 * Applies Graphics Hub profiles to Minecraft GameOptions and optional Sodium settings.
 */
public final class SettingsApplier {
    private static OptionsBackup sessionBackup;

    private SettingsApplier() {
    }

    public static boolean hasSessionBackup() {
        return sessionBackup != null;
    }

    public static void restoreSessionBackup() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (sessionBackup == null || client == null) {
            return;
        }
        sessionBackup.restore(client.options);
        OleafClient.LOGGER.info("Restored previous Minecraft/Sodium video settings from session backup");
    }

    public static void apply(GraphicsProfile profile) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || profile == null) {
            return;
        }

        GameOptions options = client.options;
        if (sessionBackup == null) {
            sessionBackup = OptionsBackup.capture(options);
            OleafClient.LOGGER.info("Captured one-shot video settings backup for this session");
        }

        applyMinecraftOptions(client, options, profile);
        SodiumSettingsBridge.applyPreset(profile.getPreset());
        options.write();

        ConfigManager.getConfig().setSelectedPreset(profile.getPreset());
        // Do NOT call applyPreset() here — it resets OverlaySettings (anchor, toggles, etc.)
        // to preset defaults and wiped user overlay position on every Graphics Hub Apply.
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

    private static void applyMinecraftOptions(MinecraftClient client, GameOptions options, GraphicsProfile profile) {
        GraphicsMode mode = switch (profile.getGraphicsQuality()) {
            case LOW -> GraphicsMode.FAST;
            case MEDIUM -> GraphicsMode.FANCY;
            case HIGH, ULTRA -> GraphicsMode.FABULOUS;
        };

        options.getGraphicsMode().setValue(mode);

        options.getViewDistance().setValue(profile.getRenderDistance());
        options.getSimulationDistance().setValue(profile.getSimulationDistance());
        options.getEntityDistanceScaling().setValue(profile.getEntityDistancePercent() / 100.0D);
        options.getMaxFps().setValue(profile.getMaxFps());
        options.getEnableVsync().setValue(profile.isVsync());
        options.getEntityShadows().setValue(profile.isEntityShadows());
        options.getBobView().setValue(profile.isViewBobbing());
        options.getGamma().setValue(profile.getBrightness());
        options.getFov().setValue(profile.getFov());
        options.getGuiScale().setValue(profile.getGuiScale());

        boolean wantFullscreen = profile.isFullscreen();
        options.getFullscreen().setValue(wantFullscreen);
        if (client.getWindow() != null && client.getWindow().isFullscreen() != wantFullscreen) {
            client.getWindow().toggleFullscreen();
            options.getFullscreen().setValue(client.getWindow().isFullscreen());
        }

        applyParticles(options, profile.getParticleDetail());
        applyClouds(options, profile.getCloudDetail());
        applyVisualEffectsExtras(options, profile.getVisualEffects(), profile.getGraphicsQuality());
        applyStabilityBiasExtras(options, profile.getStabilityBias(), profile);

        if (profile.getGraphicsQuality() == GraphicsQuality.ULTRA) {
            options.getBiomeBlendRadius().setValue(Math.max(options.getBiomeBlendRadius().getValue(), 3));
            options.getMipmapLevels().setValue(4);
        } else if (profile.getGraphicsQuality() == GraphicsQuality.LOW) {
            options.getBiomeBlendRadius().setValue(Math.min(options.getBiomeBlendRadius().getValue(), 1));
            options.getMipmapLevels().setValue(2);
        }

        if (client.worldRenderer != null) {
            client.worldRenderer.reload();
        }
        // Reapply GUI scale / viewport so FOV and GUI scale changes take effect immediately.
        client.onResolutionChanged();
    }

    private static void applyParticles(GameOptions options, ParticleDetail detail) {
        ParticlesMode mode = switch (detail) {
            case MINIMAL -> ParticlesMode.MINIMAL;
            case DECREASED -> ParticlesMode.DECREASED;
            case ALL -> ParticlesMode.ALL;
        };
        options.getParticles().setValue(mode);
    }

    private static void applyClouds(GameOptions options, CloudDetail detail) {
        CloudRenderMode mode = switch (detail) {
            case OFF -> CloudRenderMode.OFF;
            case FAST -> CloudRenderMode.FAST;
            case FANCY -> CloudRenderMode.FANCY;
        };
        options.getCloudRenderMode().setValue(mode);
    }

    private static void applyVisualEffectsExtras(GameOptions options, VisualEffectsLevel effects, GraphicsQuality quality) {
        // 1.20.1 has AO / smooth lighting but not cutoutLeaves / vignette GameOptions.
        switch (effects) {
            case MINIMAL -> options.getAo().setValue(false);
            case REDUCED -> options.getAo().setValue(quality != GraphicsQuality.LOW);
            case FULL -> options.getAo().setValue(true);
        }
    }

    private static void applyStabilityBiasExtras(GameOptions options, StabilityBias bias, GraphicsProfile profile) {
        // Hub already sets max FPS / VSync from explicit knobs; bias only nudges if user left defaults linked via preset.
        if (bias == StabilityBias.PERFORMANCE && profile.getPreset() == com.shiraken.optimizationcompanion.config.Preset.PVP_COMPETITIVE) {
            options.getBobView().setValue(false);
            options.getEntityShadows().setValue(false);
        }
    }
}
