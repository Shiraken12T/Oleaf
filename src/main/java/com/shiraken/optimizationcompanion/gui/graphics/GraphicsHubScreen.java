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
import com.shiraken.optimizationcompanion.settings.OptimizedCoreSettingsBridge;
import com.shiraken.optimizationcompanion.settings.SettingsApplier;
import com.shiraken.optimizationcompanion.settings.SodiumSettingsBridge;
import com.shiraken.optimizationcompanion.upscale.UpscalePipeline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/**
 * Vanilla-styled graphics presets hub (Options → Video Settings).
 */
public final class GraphicsHubScreen extends Screen {
    private final Screen parent;
    private final GraphicsProfile profile;

    private CyclingButtonWidget<Preset> presetButton;
    private IntSlider renderDistanceSlider;
    private IntSlider simulationDistanceSlider;
    private IntSlider entityDistanceSlider;
    private IntSlider maxFpsSlider;
    private IntSlider fovSlider;
    private BrightnessSlider brightnessSlider;
    private Text statusMessage = Text.empty();

    public GraphicsHubScreen(Screen parent) {
        super(Text.translatable("text.oleaf.graphics_hub.title"));
        this.parent = parent;
        this.profile = GraphicsProfile.fromConfig(ConfigManager.getConfig());
    }

    @Override
    protected void init() {
        clearChildren();

        // Classic OptionsScreen two-column layout (150px buttons).
        int left = this.width / 2 - 155;
        int right = this.width / 2 + 5;
        int y = this.height / 6 - 12;
        int row = 24;

        presetButton = addDrawableChild(CyclingButtonWidget
                .builder((Preset value) -> Text.literal(value.getDisplayName()), profile.getPreset())
                .values(Preset.values())
                .build(left, y, 310, 20, Text.translatable("text.oleaf.graphics_hub.preset"),
                        (button, value) -> {
                            profile.applyPresetDefaults(value);
                            rebuildAfterPreset();
                        }));

        y += row;
        addDrawableChild(CyclingButtonWidget
                .builder((GraphicsQuality value) -> Text.literal(value.getDisplayName()), profile.getGraphicsQuality())
                .values(GraphicsQuality.values())
                .build(left, y, 150, 20, Text.translatable("text.oleaf.graphics_hub.quality"),
                        (button, value) -> profile.setGraphicsQuality(value)));

        addDrawableChild(CyclingButtonWidget
                .builder((StabilityBias value) -> Text.literal(value.getDisplayName()), profile.getStabilityBias())
                .values(StabilityBias.values())
                .build(right, y, 150, 20, Text.translatable("text.oleaf.graphics_hub.stability"),
                        (button, value) -> {
                            profile.setStabilityBias(value);
                            applyStabilityToKnobs(value);
                            maxFpsSlider.setIntValue(profile.getMaxFps());
                        }));

        y += row;
        renderDistanceSlider = addDrawableChild(new IntSlider(
                left, y, 150, 20,
                Text.translatable("text.oleaf.graphics_hub.render_distance_label"),
                2, 32, profile.getRenderDistance(),
                value -> {
                    profile.setRenderDistance(value);
                    return Text.translatable("text.oleaf.graphics_hub.render_distance", value);
                }));

        simulationDistanceSlider = addDrawableChild(new IntSlider(
                right, y, 150, 20,
                Text.translatable("text.oleaf.graphics_hub.simulation_distance_label"),
                5, 32, profile.getSimulationDistance(),
                value -> {
                    profile.setSimulationDistance(value);
                    return Text.translatable("text.oleaf.graphics_hub.simulation_distance", value);
                }));

        y += row;
        entityDistanceSlider = addDrawableChild(new IntSlider(
                left, y, 150, 20,
                Text.translatable("text.oleaf.graphics_hub.entity_distance_label"),
                50, 500, profile.getEntityDistancePercent(),
                value -> {
                    profile.setEntityDistancePercent(value);
                    return Text.translatable("text.oleaf.graphics_hub.entity_distance", value);
                }));

        maxFpsSlider = addDrawableChild(new IntSlider(
                right, y, 150, 20,
                Text.translatable("text.oleaf.graphics_hub.max_fps_label"),
                10, GameOptions.MAX_FPS_LIMIT, profile.getMaxFps(),
                value -> {
                    // Match vanilla FPS slider stepping (10 FPS increments).
                    int stepped = Math.round(value / 10.0F) * 10;
                    stepped = Math.max(10, Math.min(GameOptions.MAX_FPS_LIMIT, stepped));
                    profile.setMaxFps(stepped);
                    if (stepped >= GameOptions.MAX_FPS_LIMIT) {
                        return Text.translatable(
                                "options.generic_value",
                                Text.translatable("options.framerateLimit"),
                                Text.translatable("options.framerateLimit.max")
                        );
                    }
                    return Text.translatable("text.oleaf.graphics_hub.max_fps", stepped);
                }));

        y += row;
        addDrawableChild(CyclingButtonWidget
                .builder(value -> Text.translatable(value ? "options.on" : "options.off"), profile.isVsync())
                .values(true, false)
                .build(left, y, 150, 20, Text.translatable("text.oleaf.graphics_hub.vsync"),
                        (button, value) -> profile.setVsync(value)));

        brightnessSlider = addDrawableChild(new BrightnessSlider(right, y, 150, 20, profile.getBrightness()));

        y += row;
        addDrawableChild(CyclingButtonWidget
                .builder((VisualEffectsLevel value) -> Text.literal(value.getDisplayName()), profile.getVisualEffects())
                .values(VisualEffectsLevel.values())
                .build(left, y, 150, 20, Text.translatable("text.oleaf.graphics_hub.effects"),
                        (button, value) -> profile.setVisualEffects(value)));

        addDrawableChild(CyclingButtonWidget
                .builder((ParticleDetail value) -> Text.literal(value.getDisplayName()), profile.getParticleDetail())
                .values(ParticleDetail.values())
                .build(right, y, 150, 20, Text.translatable("text.oleaf.graphics_hub.particles"),
                        (button, value) -> profile.setParticleDetail(value)));

        y += row;
        addDrawableChild(CyclingButtonWidget
                .builder((CloudDetail value) -> Text.literal(value.getDisplayName()), profile.getCloudDetail())
                .values(CloudDetail.values())
                .build(left, y, 150, 20, Text.translatable("text.oleaf.graphics_hub.clouds"),
                        (button, value) -> profile.setCloudDetail(value)));

        addDrawableChild(CyclingButtonWidget
                .builder(value -> Text.translatable(value ? "options.on" : "options.off"), profile.isEntityShadows())
                .values(true, false)
                .build(right, y, 150, 20, Text.translatable("text.oleaf.graphics_hub.entity_shadows"),
                        (button, value) -> profile.setEntityShadows(value)));

        y += row;
        addDrawableChild(CyclingButtonWidget
                .builder(value -> Text.translatable(value ? "options.on" : "options.off"), profile.isViewBobbing())
                .values(true, false)
                .build(left, y, 150, 20, Text.translatable("text.oleaf.graphics_hub.view_bobbing"),
                        (button, value) -> profile.setViewBobbing(value)));

        fovSlider = addDrawableChild(new IntSlider(
                right, y, 150, 20, Text.empty(), 30, 110, profile.getFov(),
                value -> {
                    profile.setFov(value);
                    return Text.translatable("text.oleaf.graphics_hub.fov", value);
                }));

        y += row;
        addDrawableChild(CyclingButtonWidget
                .builder(GraphicsHubScreen::guiScaleLabel, profile.getGuiScale())
                .values(0, 1, 2, 3, 4, 5, 6)
                .build(left, y, 150, 20, Text.translatable("text.oleaf.graphics_hub.gui_scale"),
                        (button, value) -> profile.setGuiScale(value)));

        addDrawableChild(CyclingButtonWidget
                .builder(value -> Text.translatable(value ? "options.on" : "options.off"), profile.isFullscreen())
                .values(true, false)
                .build(right, y, 150, 20, Text.translatable("text.oleaf.graphics_hub.fullscreen"),
                        (button, value) -> profile.setFullscreen(value)));

        y += row;
        addDrawableChild(ButtonWidget.builder(
                        upscaleButtonLabel(),
                        button -> this.client.setScreen(new UpscaleSettingsScreen(this, profile)))
                .dimensions(left, y, 150, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(
                        frameGenButtonLabel(),
                        button -> this.client.setScreen(new FrameGenScreen(this, profile)))
                .dimensions(right, y, 150, 20)
                .build());

        y += row;
        addDrawableChild(ButtonWidget.builder(
                        overlayButtonLabel(),
                        button -> this.client.setScreen(new OverlaySettingsScreen(this)))
                .dimensions(left, y, 150, 20)
                .build());
        ButtonWidget shadersButton = addDrawableChild(ButtonWidget.builder(
                        shadersButtonLabel(),
                        button -> openShaders())
                .dimensions(right, y, 150, 20)
                .build());
        shadersButton.active = IrisSettingsBridge.isAvailable();

        if (UpscalePipeline.get().isIrisLoaded() && UpscalePipeline.get().isBlockedByIrisShaders()) {
            statusMessage = Text.translatable("text.oleaf.graphics_hub.upscale_iris_pack_hint");
        }

        // Leave room under the footer for the author watermark.
        int footerY = this.height - 40;
        addDrawableChild(ButtonWidget.builder(Text.translatable("text.oleaf.graphics_hub.apply"), button -> applyProfile())
                .dimensions(this.width / 2 - 155, footerY, 100, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("text.oleaf.graphics_hub.advanced"), button ->
                        this.client.setScreen(new AdvancedGraphicsScreen(this)))
                .dimensions(this.width / 2 - 50, footerY, 100, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
                .dimensions(this.width / 2 + 55, footerY, 100, 20)
                .build());
    }

    private void rebuildAfterPreset() {
        // Rebuild widgets so sliders/toggles match preset defaults.
        this.clearAndInit();
        statusMessage = Text.translatable(
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
            case PERFORMANCE -> {
                profile.setVsync(false);
                profile.setMaxFps(GameOptions.MAX_FPS_LIMIT);
            }
            case BALANCED -> {
                // Leave max FPS and VSync under user control; Balanced does not force 120/60 caps.
            }
        }
    }

    private void applyProfile() {
        SettingsApplier.apply(profile);
        String sodiumNote = SodiumSettingsBridge.isAvailable()
                ? Text.translatable("text.oleaf.graphics_hub.sodium_updated").getString()
                : Text.translatable("text.oleaf.graphics_hub.sodium_absent").getString();
        String ocNote = OptimizedCoreSettingsBridge.isAvailable()
                ? Text.translatable("text.oleaf.graphics_hub.optimizedcore_updated").getString()
                : "";
        String upscaleNote = profile.getUpscaleQuality().isEnabled()
                ? Text.translatable(
                        UpscalePipeline.get().isBlockedByIrisShaders()
                                ? "text.oleaf.graphics_hub.upscale_applied_iris"
                                : "text.oleaf.graphics_hub.upscale_applied"
                ).getString()
                : "";
        statusMessage = Text.translatable(
                "text.oleaf.graphics_hub.applied",
                profile.getPreset().getDisplayName(),
                (sodiumNote + " " + ocNote + " " + upscaleNote).replaceAll(" +", " ").trim()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.translatable("text.oleaf.graphics_hub.subtitle"),
                this.width / 2,
                28,
                0xFFA0A0A0
        );
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                hubStatusLine(),
                this.width / 2,
                40,
                0xFFC0C0C0
        );

        int summaryY = this.height - 66;
        for (String line : profile.describeChanges()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(line), this.width / 2, summaryY, 0xFFE0E0E0);
            summaryY += 10;
            break; // one summary line under the options, like vanilla tooltips area
        }

        if (!statusMessage.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, statusMessage, this.width / 2, this.height - 54, 0xFFFFFF55);
        }

        context.drawTextWithShadow(
                this.textRenderer,
                Text.translatable("text.oleaf.graphics_hub.watermark"),
                6,
                this.height - 12,
                0xFF888888
        );
    }

    private Text upscaleButtonLabel() {
        return Text.translatable(
                "text.oleaf.graphics_hub.upscale_button",
                profile.getUpscaleQuality().getDisplayName()
        );
    }

    private Text frameGenButtonLabel() {
        FrameGenSettings fg = ConfigManager.getConfig().getFrameGen();
        if (!fg.isEnabled()) {
            return Text.translatable(
                    "text.oleaf.graphics_hub.framegen_button",
                    Text.translatable("text.oleaf.graphics_hub.framegen_off")
            );
        }
        return Text.translatable(
                "text.oleaf.graphics_hub.framegen_button",
                fg.getMultiplier() + "x"
        );
    }

    private Text overlayButtonLabel() {
        var overlay = ConfigManager.getConfig().getOverlay();
        Text state = Text.translatable(overlay.isEnabled() ? "options.on" : "options.off");
        return Text.translatable(
                "text.oleaf.graphics_hub.overlay_button",
                state,
                overlay.getAnchor().getDisplayName()
        );
    }

    private Text shadersButtonLabel() {
        if (!IrisSettingsBridge.isAvailable()) {
            return Text.translatable("text.oleaf.graphics_hub.shaders_unavailable");
        }
        return Text.translatable("text.oleaf.graphics_hub.shaders_button");
    }

    private void openShaders() {
        Screen irisScreen = IrisSettingsBridge.createOptionsScreen(this);
        if (irisScreen != null) {
            this.client.setScreen(irisScreen);
        } else {
            statusMessage = Text.translatable("text.oleaf.advanced.iris_unavailable");
        }
    }

    private Text hubStatusLine() {
        FrameGenSettings fg = ConfigManager.getConfig().getFrameGen();
        String fgState = fg.isEnabled()
                ? fg.getMultiplier() + "x"
                : Text.translatable("text.oleaf.graphics_hub.framegen_off").getString();
        Text fpsText = profile.getMaxFps() >= GameOptions.MAX_FPS_LIMIT
                ? Text.translatable("text.oleaf.graphics_hub.status_unlimited")
                : Text.literal(String.valueOf(profile.getMaxFps()));
        Text vsyncText = Text.translatable(profile.isVsync() ? "options.on" : "options.off");
        return Text.translatable(
                "text.oleaf.graphics_hub.status_line",
                profile.getUpscaleQuality().getDisplayName(),
                fgState,
                fpsText,
                vsyncText
        );
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    @FunctionalInterface
    private interface IntMessageFactory {
        Text create(int value);
    }

    private static final class IntSlider extends SliderWidget {
        private final int min;
        private final int max;
        private final IntMessageFactory messageFactory;
        private int intValue;

        private IntSlider(int x, int y, int width, int height, Text unused, int min, int max, int value, IntMessageFactory messageFactory) {
            super(x, y, width, height, Text.empty(), normalize(min, max, value));
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

    private final class BrightnessSlider extends SliderWidget {
        private BrightnessSlider(int x, int y, int width, int height, double brightness) {
            super(x, y, width, height, Text.empty(), brightness);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int percent = (int) Math.round(this.value * 100.0D);
            setMessage(Text.translatable("text.oleaf.graphics_hub.brightness", percent));
        }

        @Override
        protected void applyValue() {
            profile.setBrightness(this.value);
            updateMessage();
        }
    }

    private static Text guiScaleLabel(int value) {
        if (value == 0) {
            return Text.translatable("options.guiScale.auto");
        }
        return Text.literal(value + "x");
    }
}
