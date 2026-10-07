package com.shiraken.optimizationcompanion.gui.graphics;

import com.shiraken.optimizationcompanion.config.GraphicsProfile;
import com.shiraken.optimizationcompanion.settings.SettingsApplier;
import com.shiraken.optimizationcompanion.upscale.UpscaleAlgorithm;
import com.shiraken.optimizationcompanion.upscale.UpscalePipeline;
import com.shiraken.optimizationcompanion.upscale.UpscaleQuality;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Dedicated built-in upscaler (FSR-lite) tuning screen with algorithm choice,
 * custom render scale and sharpening.
 */
public final class UpscaleSettingsScreen extends Screen {
    private final Screen parent;
    private final GraphicsProfile profile;

    private PercentSlider customScaleSlider;
    private SharpenSlider sharpenSlider;
    private Component statusMessage = Component.empty();

    public UpscaleSettingsScreen(Screen parent, GraphicsProfile profile) {
        super(Component.translatable("text.oleaf.upscale.title"));
        this.parent = parent;
        this.profile = profile;
    }

    @Override
    protected void init() {
        clearWidgets();

        int left = this.width / 2 - 155;
        int right = this.width / 2 + 5;
        int y = this.height / 6;
        int row = 24;

        addRenderableWidget(CycleButton
                .builder((UpscaleQuality value) -> Component.literal(value.getDisplayName()), profile.getUpscaleQuality())
                .withValues(UpscaleQuality.values())
                .create(left, y, 150, 20, Component.translatable("text.oleaf.graphics_hub.upscale"),
                        (button, value) -> {
                            profile.setUpscaleQuality(value);
                            refreshEnabled();
                        }));

        addRenderableWidget(CycleButton
                .builder((UpscaleAlgorithm value) -> Component.literal(value.getDisplayName()), profile.getUpscaleAlgorithm())
                .withValues(UpscaleAlgorithm.values())
                .create(right, y, 150, 20, Component.translatable("text.oleaf.upscale.algorithm"),
                        (button, value) -> profile.setUpscaleAlgorithm(value)));

        y += row;
        customScaleSlider = addRenderableWidget(new PercentSlider(
                left, y, 150, 20, 30, 100, profile.getUpscaleCustomScalePercent(),
                value -> {
                    profile.setUpscaleCustomScalePercent(value);
                    return Component.translatable("text.oleaf.upscale.custom_scale", value);
                }));

        sharpenSlider = addRenderableWidget(new SharpenSlider(right, y, 150, 20, profile.getUpscaleSharpen()));

        refreshEnabled();

        int footerY = this.height - 27;
        addRenderableWidget(Button.builder(Component.translatable("text.oleaf.graphics_hub.apply"), button -> applyNow())
                .bounds(this.width / 2 - 155, footerY, 150, 20)
                .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(this.width / 2 + 5, footerY, 150, 20)
                .build());
    }

    private void refreshEnabled() {
        boolean enabled = profile.getUpscaleQuality().isEnabled();
        if (sharpenSlider != null) {
            sharpenSlider.active = enabled;
        }
        if (customScaleSlider != null) {
            customScaleSlider.active = enabled && profile.getUpscaleQuality() == UpscaleQuality.CUSTOM;
        }
    }

    private void applyNow() {
        SettingsApplier.apply(profile);
        statusMessage = Component.translatable(
                profile.getUpscaleQuality().isEnabled()
                        ? (UpscalePipeline.get().isBlockedByIrisShaders()
                                ? "text.oleaf.graphics_hub.upscale_applied_iris"
                                : "text.oleaf.graphics_hub.upscale_applied")
                        : "text.oleaf.upscale.disabled"
        );
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);
        context.centeredText(this.font, this.title, this.width / 2, 15, 0xFFFFFFFF);
        context.centeredText(
                this.font,
                Component.translatable("text.oleaf.upscale.subtitle"),
                this.width / 2, 28, 0xFFA0A0A0);

        int infoY = this.height / 6 + 60;
        for (String line : describeInfo()) {
            context.centeredText(this.font, Component.literal(line), this.width / 2, infoY, 0xFFC0C0C0);
            infoY += 12;
        }

        if (!statusMessage.getString().isEmpty()) {
            context.centeredText(this.font, statusMessage, this.width / 2, this.height - 42, 0xFFFFFF55);
        }
    }

    private java.util.List<String> describeInfo() {
        java.util.List<String> lines = new java.util.ArrayList<>();
        int scalePercent = Math.round(profile.getUpscaleQuality() == UpscaleQuality.CUSTOM
                ? profile.getUpscaleCustomScalePercent()
                : profile.getUpscaleQuality().getScale() * 100.0F);
        if (profile.getUpscaleQuality().isEnabled()) {
            lines.add("Rendering at " + scalePercent + "% then upscaling with " + profile.getUpscaleAlgorithm().getDisplayName());
        } else {
            lines.add("Upscaler is off — the game renders at native resolution.");
        }
        lines.add("FSR 1 = real AMD EASU then RCAS. SGSR1 = Snapdragon spatial. Not temporal FSR2/4.");
        if (UpscalePipeline.get().isIrisLoaded() && UpscalePipeline.get().isBlockedByIrisShaders()) {
            lines.add(Component.translatable("text.oleaf.graphics_hub.upscale_iris_pack_hint").getString());
        }
        return lines;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @FunctionalInterface
    private interface IntMessageFactory {
        Component create(int value);
    }

    private static final class PercentSlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final IntMessageFactory messageFactory;
        private int intValue;

        private PercentSlider(int x, int y, int width, int height, int min, int max, int value, IntMessageFactory messageFactory) {
            super(x, y, width, height, Component.empty(), (clamp(min, max, value) - min) / (double) (max - min));
            this.min = min;
            this.max = max;
            this.intValue = clamp(min, max, value);
            this.messageFactory = messageFactory;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(messageFactory.create(intValue));
        }

        @Override
        protected void applyValue() {
            intValue = min + (int) Math.round(this.value * (max - min));
            messageFactory.create(intValue);
        }

        private static int clamp(int min, int max, int value) {
            return Math.max(min, Math.min(max, value));
        }
    }

    private final class SharpenSlider extends AbstractSliderButton {
        private SharpenSlider(int x, int y, int width, int height, float sharpen) {
            super(x, y, width, height, Component.empty(), sharpen);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int percent = (int) Math.round(this.value * 100.0D);
            setMessage(Component.translatable("text.oleaf.graphics_hub.upscale_sharpen", percent));
        }

        @Override
        protected void applyValue() {
            profile.setUpscaleSharpen((float) this.value);
            updateMessage();
        }
    }
}
