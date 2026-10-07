package com.shiraken.optimizationcompanion.gui.graphics;

import com.shiraken.optimizationcompanion.config.CloudDetail;
import com.shiraken.optimizationcompanion.config.ConfigManager;
import com.shiraken.optimizationcompanion.config.FrameGenSettings;
import com.shiraken.optimizationcompanion.config.GraphicsProfile;
import com.shiraken.optimizationcompanion.config.GraphicsQuality;
import com.shiraken.optimizationcompanion.config.ParticleDetail;
import com.shiraken.optimizationcompanion.config.Preset;
import com.shiraken.optimizationcompanion.config.StabilityBias;
import com.shiraken.optimizationcompanion.config.VisualEffectsLevel;
import com.shiraken.optimizationcompanion.settings.IrisSettingsBridge;
import com.shiraken.optimizationcompanion.settings.SettingsApplier;
import com.shiraken.optimizationcompanion.settings.SodiumSettingsBridge;
import com.shiraken.optimizationcompanion.upscale.UpscalePipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.Options;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Vanilla-styled graphics presets hub (Options → Video Settings).
 */
public final class GraphicsHubScreen extends Screen {
    private final Screen parent;
    private final GraphicsProfile profile;

    private CycleButton<Preset> presetButton;
    private IntSlider renderDistanceSlider;
    private IntSlider simulationDistanceSlider;
    private IntSlider entityDistanceSlider;
    private IntSlider maxFpsSlider;
    private IntSlider fovSlider;
    private BrightnessSlider brightnessSlider;
    private Component statusMessage = Component.empty();

    public GraphicsHubScreen(Screen parent) {
        super(Component.translatable("text.oleaf.graphics_hub.title"));
        this.parent = parent;
        this.profile = GraphicsProfile.fromConfig(ConfigManager.getConfig());
    }

    @Override
    protected void init() {
        clearWidgets();

        // Classic OptionsScreen two-column layout (150px buttons).
        int left = this.width / 2 - 155;
        int right = this.width / 2 + 5;
        int y = this.height / 6 - 12;
        int row = 24;

        presetButton = addRenderableWidget(CycleButton
                .builder((Preset value) -> Component.literal(value.getDisplayName()), profile.getPreset())
                .withValues(Preset.values())
                .create(left, y, 310, 20, Component.translatable("text.oleaf.graphics_hub.preset"),
                        (button, value) -> {
                            profile.applyPresetDefaults(value);
                            rebuildAfterPreset();
                        }));

        y += row;
        addRenderableWidget(CycleButton
                .builder((GraphicsQuality value) -> Component.literal(value.getDisplayName()), profile.getGraphicsQuality())
                .withValues(GraphicsQuality.values())
                .create(left, y, 150, 20, Component.translatable("text.oleaf.graphics_hub.quality"),
                        (button, value) -> profile.setGraphicsQuality(value)));

        addRenderableWidget(CycleButton
                .builder((StabilityBias value) -> Component.literal(value.getDisplayName()), profile.getStabilityBias())
                .withValues(StabilityBias.values())
                .create(right, y, 150, 20, Component.translatable("text.oleaf.graphics_hub.stability"),
                        (button, value) -> {
                            profile.setStabilityBias(value);
                            applyStabilityToKnobs(value);
                            maxFpsSlider.setIntValue(profile.getMaxFps());
                        }));

        y += row;
        renderDistanceSlider = addRenderableWidget(new IntSlider(
                left, y, 150, 20,
                Component.translatable("text.oleaf.graphics_hub.render_distance_label"),
                2, 32, profile.getRenderDistance(),
                value -> {
                    profile.setRenderDistance(value);
                    return Component.translatable("text.oleaf.graphics_hub.render_distance", value);
                }));

        simulationDistanceSlider = addRenderableWidget(new IntSlider(
                right, y, 150, 20,
                Component.translatable("text.oleaf.graphics_hub.simulation_distance_label"),
                5, 32, profile.getSimulationDistance(),
                value -> {
                    profile.setSimulationDistance(value);
                    return Component.translatable("text.oleaf.graphics_hub.simulation_distance", value);
                }));

        y += row;
        entityDistanceSlider = addRenderableWidget(new IntSlider(
                left, y, 150, 20,
                Component.translatable("text.oleaf.graphics_hub.entity_distance_label"),
                50, 500, profile.getEntityDistancePercent(),
                value -> {
                    profile.setEntityDistancePercent(value);
                    return Component.translatable("text.oleaf.graphics_hub.entity_distance", value);
                }));

        maxFpsSlider = addRenderableWidget(new IntSlider(
                right, y, 150, 20,
                Component.translatable("text.oleaf.graphics_hub.max_fps_label"),
                10, Options.UNLIMITED_FRAMERATE_CUTOFF, profile.getMaxFps(),
                value -> {
                    // Match vanilla FPS slider stepping (10 FPS increments).
                    int stepped = Math.round(value / 10.0F) * 10;
                    stepped = Math.max(10, Math.min(Options.UNLIMITED_FRAMERATE_CUTOFF, stepped));
                    profile.setMaxFps(stepped);
                    if (stepped >= Options.UNLIMITED_FRAMERATE_CUTOFF) {
                        return Component.translatable(
                                "options.generic_value",
                                Component.translatable("options.framerateLimit"),
                                Component.translatable("options.framerateLimit.max")
                        );
                    }
                    return Component.translatable("text.oleaf.graphics_hub.max_fps", stepped);
                }));

        y += row;
        addRenderableWidget(CycleButton
                .builder(value -> Component.translatable(value ? "options.on" : "options.off"), profile.isVsync())
                .withValues(true, false).create(left, y, 150, 20, Component.translatable("text.oleaf.graphics_hub.vsync"),
                        (button, value) -> profile.setVsync(value)));

        brightnessSlider = addRenderableWidget(new BrightnessSlider(right, y, 150, 20, profile.getBrightness()));

        y += row;
        addRenderableWidget(CycleButton
                .builder((VisualEffectsLevel value) -> Component.literal(value.getDisplayName()), profile.getVisualEffects())
                .withValues(VisualEffectsLevel.values())
                .create(left, y, 150, 20, Component.translatable("text.oleaf.graphics_hub.effects"),
                        (button, value) -> profile.setVisualEffects(value)));

        addRenderableWidget(CycleButton
                .builder((ParticleDetail value) -> Component.literal(value.getDisplayName()), profile.getParticleDetail())
                .withValues(ParticleDetail.values())
                .create(right, y, 150, 20, Component.translatable("text.oleaf.graphics_hub.particles"),
                        (button, value) -> profile.setParticleDetail(value)));

        y += row;
        addRenderableWidget(CycleButton
                .builder((CloudDetail value) -> Component.literal(value.getDisplayName()), profile.getCloudDetail())
                .withValues(CloudDetail.values())
                .create(left, y, 150, 20, Component.translatable("text.oleaf.graphics_hub.clouds"),
                        (button, value) -> profile.setCloudDetail(value)));

        addRenderableWidget(CycleButton
                .builder(value -> Component.translatable(value ? "options.on" : "options.off"), profile.isEntityShadows())
                .withValues(true, false).create(right, y, 150, 20, Component.translatable("text.oleaf.graphics_hub.entity_shadows"),
                        (button, value) -> profile.setEntityShadows(value)));

        y += row;
        addRenderableWidget(CycleButton
                .builder(value -> Component.translatable(value ? "options.on" : "options.off"), profile.isViewBobbing())
                .withValues(true, false).create(left, y, 150, 20, Component.translatable("text.oleaf.graphics_hub.view_bobbing"),
                        (button, value) -> profile.setViewBobbing(value)));

        fovSlider = addRenderableWidget(new IntSlider(
                right, y, 150, 20, Component.empty(), 30, 110, profile.getFov(),
                value -> {
                    profile.setFov(value);
                    return Component.translatable("text.oleaf.graphics_hub.fov", value);
                }));

        y += row;
        addRenderableWidget(CycleButton
                .builder(GraphicsHubScreen::guiScaleLabel, profile.getGuiScale())
                .withValues(0, 1, 2, 3, 4, 5, 6).create(left, y, 150, 20, Component.translatable("text.oleaf.graphics_hub.gui_scale"),
                        (button, value) -> profile.setGuiScale(value)));

        addRenderableWidget(CycleButton
                .builder(value -> Component.translatable(value ? "options.on" : "options.off"), profile.isFullscreen())
                .withValues(true, false).create(right, y, 150, 20, Component.translatable("text.oleaf.graphics_hub.fullscreen"),
                        (button, value) -> profile.setFullscreen(value)));

        y += row;
        addRenderableWidget(Button.builder(
                        upscaleButtonLabel(),
                        button -> this.minecraft.gui.setScreen(new UpscaleSettingsScreen(this, profile)))
                .bounds(left, y, 150, 20)
                .build());
        addRenderableWidget(Button.builder(
                        frameGenButtonLabel(),
                        button -> this.minecraft.gui.setScreen(new FrameGenScreen(this, profile)))
                .bounds(right, y, 150, 20)
                .build());

        y += row;
        addRenderableWidget(Button.builder(
                        overlayButtonLabel(),
                        button -> this.minecraft.gui.setScreen(new OverlaySettingsScreen(this)))
                .bounds(left, y, 150, 20)
                .build());
        Button shadersButton = addRenderableWidget(Button.builder(
                        shadersButtonLabel(),
                        button -> openShaders())
                .bounds(right, y, 150, 20)
                .build());
        shadersButton.active = IrisSettingsBridge.isAvailable();

        if (UpscalePipeline.get().isIrisLoaded() && UpscalePipeline.get().isBlockedByIrisShaders()) {
            statusMessage = Component.translatable("text.oleaf.graphics_hub.upscale_iris_pack_hint");
        }

        // Leave room under the footer for the author watermark.
        int footerY = this.height - 40;
        addRenderableWidget(Button.builder(Component.translatable("text.oleaf.graphics_hub.apply"), button -> applyProfile())
                .bounds(this.width / 2 - 155, footerY, 100, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("text.oleaf.graphics_hub.advanced"), button ->
                        this.minecraft.gui.setScreen(new AdvancedGraphicsScreen(this)))
                .bounds(this.width / 2 - 50, footerY, 100, 20)
                .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(this.width / 2 + 55, footerY, 100, 20)
                .build());
    }

    private void rebuildAfterPreset() {
        // Rebuild widgets so sliders/toggles match preset defaults.
        this.rebuildWidgets();
        statusMessage = Component.translatable(
                "text.oleaf.graphics_hub.preset_selected",
                profile.getPreset().getDisplayName()
        );
    }

    private void applyStabilityToKnobs(StabilityBias bias) {
        switch (bias) {
            case STABILITY -> {
                profile.setVsync(true);
                profile.setMaxFps(60);
            }
            case BALANCED -> {
                profile.setVsync(true);
                profile.setMaxFps(120);
            }
            case PERFORMANCE -> {
                profile.setVsync(false);
                profile.setMaxFps(Options.UNLIMITED_FRAMERATE_CUTOFF);
            }
        }
    }

    private void applyProfile() {
        SettingsApplier.apply(profile);
        String sodiumNote = SodiumSettingsBridge.isAvailable()
                ? Component.translatable("text.oleaf.graphics_hub.sodium_updated").getString()
                : Component.translatable("text.oleaf.graphics_hub.sodium_absent").getString();
        String upscaleNote = profile.getUpscaleQuality().isEnabled()
                ? Component.translatable(
                        UpscalePipeline.get().isBlockedByIrisShaders()
                                ? "text.oleaf.graphics_hub.upscale_applied_iris"
                                : "text.oleaf.graphics_hub.upscale_applied"
                ).getString()
                : "";
        statusMessage = Component.translatable(
                "text.oleaf.graphics_hub.applied",
                profile.getPreset().getDisplayName(),
                (sodiumNote + " " + upscaleNote).trim()
        );
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);
        context.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
        context.centeredText(
                this.font,
                Component.translatable("text.oleaf.graphics_hub.subtitle"),
                this.width / 2,
                28,
                0xFFA0A0A0
        );
        context.centeredText(
                this.font,
                hubStatusLine(),
                this.width / 2,
                40,
                0xFFC0C0C0
        );

        int summaryY = this.height - 66;
        for (String line : profile.describeChanges()) {
            context.centeredText(this.font, Component.literal(line), this.width / 2, summaryY, 0xFFE0E0E0);
            summaryY += 10;
            break; // one summary line under the options, like vanilla tooltips area
        }

        if (!statusMessage.getString().isEmpty()) {
            context.centeredText(this.font, statusMessage, this.width / 2, this.height - 54, 0xFFFFFF55);
        }

        context.text(
                this.font,
                Component.translatable("text.oleaf.graphics_hub.watermark"),
                6,
                this.height - 12,
                0xFF888888,
                true
        );
    }

    private Component upscaleButtonLabel() {
        return Component.translatable(
                "text.oleaf.graphics_hub.upscale_button",
                profile.getUpscaleQuality().getDisplayName()
        );
    }

    private Component frameGenButtonLabel() {
        FrameGenSettings fg = ConfigManager.getConfig().getFrameGen();
        if (!fg.isEnabled()) {
            return Component.translatable(
                    "text.oleaf.graphics_hub.framegen_button",
                    Component.translatable("text.oleaf.graphics_hub.framegen_off")
            );
        }
        return Component.translatable(
                "text.oleaf.graphics_hub.framegen_button",
                fg.getMultiplier() + "x"
        );
    }

    private Component overlayButtonLabel() {
        var overlay = ConfigManager.getConfig().getOverlay();
        Component state = Component.translatable(overlay.isEnabled() ? "options.on" : "options.off");
        return Component.translatable(
                "text.oleaf.graphics_hub.overlay_button",
                state,
                overlay.getAnchor().getDisplayName()
        );
    }

    private Component shadersButtonLabel() {
        if (!IrisSettingsBridge.isAvailable()) {
            return Component.translatable("text.oleaf.graphics_hub.shaders_unavailable");
        }
        return Component.translatable("text.oleaf.graphics_hub.shaders_button");
    }

    private void openShaders() {
        Screen irisScreen = IrisSettingsBridge.createOptionsScreen(this);
        if (irisScreen != null) {
            this.minecraft.gui.setScreen(irisScreen);
        } else {
            statusMessage = Component.translatable("text.oleaf.advanced.iris_unavailable");
        }
    }

    private Component hubStatusLine() {
        FrameGenSettings fg = ConfigManager.getConfig().getFrameGen();
        String fgState = fg.isEnabled()
                ? fg.getMultiplier() + "x"
                : Component.translatable("text.oleaf.graphics_hub.framegen_off").getString();
        Component fpsText = profile.getMaxFps() >= Options.UNLIMITED_FRAMERATE_CUTOFF
                ? Component.translatable("text.oleaf.graphics_hub.status_unlimited")
                : Component.literal(String.valueOf(profile.getMaxFps()));
        Component vsyncText = Component.translatable(profile.isVsync() ? "options.on" : "options.off");
        return Component.translatable(
                "text.oleaf.graphics_hub.status_line",
                profile.getUpscaleQuality().getDisplayName(),
                fgState,
                fpsText,
                vsyncText
        );
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @FunctionalInterface
    private interface IntMessageFactory {
        Component create(int value);
    }

    private static final class IntSlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final IntMessageFactory messageFactory;
        private int intValue;

        private IntSlider(int x, int y, int width, int height, Component unused, int min, int max, int value, IntMessageFactory messageFactory) {
            super(x, y, width, height, Component.empty(), normalize(min, max, value));
            this.min = min;
            this.max = max;
            this.intValue = clamp(min, max, value);
            this.messageFactory = messageFactory;
            updateMessage();
        }

        private void setIntValue(int value) {
            this.intValue = clamp(min, max, value);
            this.value = normalize(min, max, this.intValue);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(messageFactory.create(intValue));
        }

        @Override
        protected void applyValue() {
            intValue = min + (int) Math.round(this.value * (max - min));
            updateMessage();
            messageFactory.create(intValue);
        }

        private static double normalize(int min, int max, int value) {
            return (clamp(min, max, value) - min) / (double) (max - min);
        }

        private static int clamp(int min, int max, int value) {
            return Math.max(min, Math.min(max, value));
        }
    }

    private final class BrightnessSlider extends AbstractSliderButton {
        private BrightnessSlider(int x, int y, int width, int height, double brightness) {
            super(x, y, width, height, Component.empty(), brightness);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int percent = (int) Math.round(this.value * 100.0D);
            setMessage(Component.translatable("text.oleaf.graphics_hub.brightness", percent));
        }

        @Override
        protected void applyValue() {
            profile.setBrightness(this.value);
            updateMessage();
        }
    }

    private static Component guiScaleLabel(int value) {
        if (value == 0) {
            return Component.translatable("options.guiScale.auto");
        }
        return Component.literal(value + "x");
    }
}
