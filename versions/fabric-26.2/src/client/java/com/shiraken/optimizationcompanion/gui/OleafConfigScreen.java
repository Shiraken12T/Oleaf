package com.shiraken.optimizationcompanion.gui;

import com.shiraken.optimizationcompanion.config.CompanionConfig;
import com.shiraken.optimizationcompanion.config.ConfigManager;
import com.shiraken.optimizationcompanion.config.GraphicsProfile;
import com.shiraken.optimizationcompanion.config.OverlayAnchor;
import com.shiraken.optimizationcompanion.config.Preset;
import com.shiraken.optimizationcompanion.mods.DetectedMod;
import com.shiraken.optimizationcompanion.mods.OptimizationModDetector;
import com.shiraken.optimizationcompanion.settings.SettingsApplier;
import com.shiraken.optimizationcompanion.upscale.UpscalePipeline;
import com.shiraken.optimizationcompanion.upscale.UpscaleQuality;
import com.shiraken.optimizationcompanion.upscale.UpscaleSettings;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class OleafConfigScreen {
    private OleafConfigScreen() {
    }

    public static Screen create(Screen parent) {
        CompanionConfig config = ConfigManager.getConfig();
        CompanionConfig.OverlaySettings overlay = config.getOverlay();
        UpscaleSettings upscale = config.getUpscale();
        Preset originalPreset = config.getSelectedPreset();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("text.oleaf.title"))
                .setDoesConfirmSave(true)
                .setSavingRunnable(() -> {
                    if (config.getSelectedPreset() != originalPreset) {
                        // Upscale defaults only — applyPreset() would also stomp overlay settings.
                        config.applyUpscaleForPreset(config.getSelectedPreset());
                    }
                    ConfigManager.save();
                    UpscalePipeline.get().notifySettingsChanged();
                });
        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory main = builder.getOrCreateCategory(Component.translatable("text.oleaf.modmenu.main"));
        main.addEntry(entries.startTextDescription(Component.translatable("text.oleaf.modmenu.main_help")).build());
        main.addEntry(entries.startEnumSelector(
                        Component.translatable("text.oleaf.modmenu.preset"),
                        Preset.class,
                        config.getSelectedPreset())
                .setDefaultValue(Preset.BALANCED_SURVIVAL)
                .setTooltip(Component.translatable("text.oleaf.modmenu.preset_tooltip"))
                .setSaveConsumer(config::setSelectedPreset)
                .build());
        for (String summaryLine : config.getSelectedPreset().getSummary()) {
            main.addEntry(entries.startTextDescription(Component.literal("• " + summaryLine)).build());
        }
        main.addEntry(entries.startBooleanToggle(
                        Component.translatable("text.oleaf.modmenu.advanced_mode"),
                        config.isAdvancedMode())
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.oleaf.modmenu.advanced_mode_tooltip"))
                .setSaveConsumer(config::setAdvancedMode)
                .build());
        main.addEntry(entries.startTextDescription(Component.translatable("text.oleaf.modmenu.open_hub_help")).build());

        ConfigCategory graphics = builder.getOrCreateCategory(Component.translatable("text.oleaf.modmenu.graphics"));
        graphics.addEntry(entries.startTextDescription(Component.translatable("text.oleaf.modmenu.graphics_help")).build());
        graphics.addEntry(entries.startBooleanToggle(
                        Component.translatable("text.oleaf.modmenu.apply_preset_now"),
                        false)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text.oleaf.modmenu.apply_preset_now_tooltip"))
                .setSaveConsumer(apply -> {
                    if (apply) {
                        GraphicsProfile profile = GraphicsProfile.fromConfig(config);
                        SettingsApplier.apply(profile);
                    }
                })
                .build());
        graphics.addEntry(entries.startTextDescription(Component.translatable("text.oleaf.modmenu.graphics_controls")).build());
        graphics.addEntry(entries.startTextDescription(Component.translatable("text.oleaf.modmenu.graphics_hint_quality")).build());
        graphics.addEntry(entries.startTextDescription(Component.translatable("text.oleaf.modmenu.graphics_hint_distance")).build());
        graphics.addEntry(entries.startTextDescription(Component.translatable("text.oleaf.modmenu.graphics_hint_fps")).build());
        graphics.addEntry(entries.startTextDescription(Component.translatable("text.oleaf.modmenu.graphics_hint_effects")).build());

        ConfigCategory upscaleCategory = builder.getOrCreateCategory(Component.translatable("text.oleaf.modmenu.upscale"));
        upscaleCategory.addEntry(entries.startTextDescription(Component.translatable("text.oleaf.modmenu.upscale_help")).build());
        if (UpscalePipeline.get().isIrisLoaded()) {
            upscaleCategory.addEntry(entries.startTextDescription(Component.translatable("text.oleaf.modmenu.upscale_iris_warning")).build());
        }
        upscaleCategory.addEntry(entries.startEnumSelector(
                        Component.translatable("text.oleaf.modmenu.upscale_quality"),
                        UpscaleQuality.class,
                        upscale.getQuality())
                .setDefaultValue(UpscaleQuality.OFF)
                .setEnumNameProvider(value -> Component.literal(((UpscaleQuality) value).getDisplayName()))
                .setTooltip(Component.translatable("text.oleaf.modmenu.upscale_quality_tooltip"))
                .setSaveConsumer(upscale::setQuality)
                .build());
        upscaleCategory.addEntry(entries.startIntSlider(
                        Component.translatable("text.oleaf.modmenu.upscale_sharpen"),
                        Math.round(upscale.getSharpen() * 100.0F),
                        0,
                        100)
                .setDefaultValue(20)
                .setTextGetter(value -> Component.literal(value + "%"))
                .setTooltip(Component.translatable("text.oleaf.modmenu.upscale_sharpen_tooltip"))
                .setSaveConsumer(value -> upscale.setSharpen(value / 100.0F))
                .build());

        ConfigCategory overlayCategory = builder.getOrCreateCategory(Component.translatable("text.oleaf.modmenu.overlay"));
        overlayCategory.addEntry(entries.startTextDescription(Component.translatable("text.oleaf.modmenu.overlay_help")).build());
        overlayCategory.addEntry(entries.startBooleanToggle(
                        Component.translatable("text.oleaf.modmenu.overlay_enabled"),
                        overlay.isEnabled())
                .setDefaultValue(true)
                .setSaveConsumer(overlay::setEnabled)
                .build());
        overlayCategory.addEntry(entries.startEnumSelector(
                        Component.translatable("text.oleaf.modmenu.overlay_anchor"),
                        OverlayAnchor.class,
                        overlay.getAnchor())
                .setDefaultValue(OverlayAnchor.TOP_LEFT)
                .setSaveConsumer(overlay::setAnchor)
                .build());
        overlayCategory.addEntry(entries.startIntSlider(
                        Component.translatable("text.oleaf.modmenu.overlay_offset_x"),
                        overlay.getOffsetX(),
                        -300,
                        300)
                .setDefaultValue(8)
                .setTextGetter(value -> Component.literal(value + " px"))
                .setSaveConsumer(overlay::setOffsetX)
                .build());
        overlayCategory.addEntry(entries.startIntSlider(
                        Component.translatable("text.oleaf.modmenu.overlay_offset_y"),
                        overlay.getOffsetY(),
                        -300,
                        300)
                .setDefaultValue(8)
                .setTextGetter(value -> Component.literal(value + " px"))
                .setSaveConsumer(overlay::setOffsetY)
                .build());
        overlayCategory.addEntry(entries.startIntSlider(
                        Component.translatable("text.oleaf.modmenu.overlay_scale"),
                        Math.round(overlay.getScale() * 100.0F),
                        50,
                        200)
                .setDefaultValue(100)
                .setTextGetter(value -> Component.literal(value + "%"))
                .setSaveConsumer(value -> overlay.setScale(value / 100.0F))
                .build());
        overlayCategory.addEntry(entries.startIntSlider(
                        Component.translatable("text.oleaf.modmenu.overlay_opacity"),
                        Math.round(overlay.getOpacity() * 100.0F),
                        20,
                        100)
                .setDefaultValue(75)
                .setTextGetter(value -> Component.literal(value + "%"))
                .setSaveConsumer(value -> overlay.setOpacity(value / 100.0F))
                .build());
        overlayCategory.addEntry(entries.startBooleanToggle(
                        Component.translatable("text.oleaf.modmenu.show_fps"),
                        overlay.isShowFps())
                .setDefaultValue(true)
                .setSaveConsumer(overlay::setShowFps)
                .build());
        overlayCategory.addEntry(entries.startBooleanToggle(
                        Component.translatable("text.oleaf.modmenu.show_frame_time"),
                        overlay.isShowFrameTime())
                .setDefaultValue(true)
                .setSaveConsumer(overlay::setShowFrameTime)
                .build());
        overlayCategory.addEntry(entries.startBooleanToggle(
                        Component.translatable("text.oleaf.modmenu.show_memory"),
                        overlay.isShowMemory())
                .setDefaultValue(true)
                .setSaveConsumer(overlay::setShowMemory)
                .build());
        overlayCategory.addEntry(entries.startBooleanToggle(
                        Component.translatable("text.oleaf.modmenu.show_ping"),
                        overlay.isShowPing())
                .setDefaultValue(true)
                .setSaveConsumer(overlay::setShowPing)
                .build());
        overlayCategory.addEntry(entries.startBooleanToggle(
                        Component.translatable("text.oleaf.modmenu.show_entities"),
                        overlay.isShowEntities())
                .setDefaultValue(true)
                .setSaveConsumer(overlay::setShowEntities)
                .build());
        overlayCategory.addEntry(entries.startBooleanToggle(
                        Component.translatable("text.oleaf.modmenu.show_fsr"),
                        overlay.isShowFsr())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.oleaf.modmenu.show_fsr_tooltip"))
                .setSaveConsumer(overlay::setShowFsr)
                .build());
        overlayCategory.addEntry(entries.startBooleanToggle(
                        Component.translatable("text.oleaf.modmenu.show_framegen"),
                        overlay.isShowFrameGen())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text.oleaf.modmenu.show_framegen_tooltip"))
                .setSaveConsumer(overlay::setShowFrameGen)
                .build());
        overlayCategory.addEntry(entries.startTextDescription(Component.translatable("text.oleaf.modmenu.keybinds_help")).build());

        ConfigCategory detected = builder.getOrCreateCategory(Component.translatable("text.oleaf.modmenu.detected"));
        detected.addEntry(entries.startTextDescription(Component.translatable("text.oleaf.modmenu.detected_help")).build());
        for (DetectedMod mod : OptimizationModDetector.detect()) {
            detected.addEntry(entries.startTextDescription(Component.literal(
                    mod.displayName() + " (" + mod.modId() + "): " + mod.statusLabel() + " — " + mod.purpose()
            )).build());
        }

        return builder.build();
    }
}
