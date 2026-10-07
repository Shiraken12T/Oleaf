package com.shiraken.optimizationcompanion.gui.graphics;

import com.shiraken.optimizationcompanion.config.GraphicsProfile;
import com.shiraken.optimizationcompanion.settings.SettingsApplier;
import com.shiraken.optimizationcompanion.upscale.UpscaleAlgorithm;
import com.shiraken.optimizationcompanion.upscale.UpscalePipeline;
import com.shiraken.optimizationcompanion.upscale.UpscaleQuality;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/**
 * Dedicated built-in upscaler (FSR-lite) tuning screen with algorithm choice,
 * custom render scale and sharpening.
 */
public final class UpscaleSettingsScreen extends Screen {
    private final Screen parent;
    private final GraphicsProfile profile;

    private PercentSlider customScaleSlider;
    private SharpenSlider sharpenSlider;
    private Text statusMessage = Text.empty();

    public UpscaleSettingsScreen(Screen parent, GraphicsProfile profile) {
        super(Text.translatable("text.oleaf.upscale.title"));
        this.parent = parent;
        this.profile = profile;
    }

    @Override
    protected void init() {
        clearChildren();

        int left = this.width / 2 - 155;
        int right = this.width / 2 + 5;
        int y = this.height / 6;
        int row = 24;

        addDrawableChild(CyclingButtonWidget
                .builder((UpscaleQuality value) -> Text.literal(value.getDisplayName()))
                .values(UpscaleQuality.values())
                .initially(profile.getUpscaleQuality())
                .build(left, y, 150, 20, Text.translatable("text.oleaf.graphics_hub.upscale"),
                        (button, value) -> {
                            profile.setUpscaleQuality(value);
                            refreshEnabled();
                        }));

        addDrawableChild(CyclingButtonWidget
                .builder((UpscaleAlgorithm value) -> Text.literal(value.getDisplayName()))
                .values(UpscaleAlgorithm.values())
                .initially(profile.getUpscaleAlgorithm())
                .build(right, y, 150, 20, Text.translatable("text.oleaf.upscale.algorithm"),
                        (button, value) -> profile.setUpscaleAlgorithm(value)));

        y += row;
        customScaleSlider = addDrawableChild(new PercentSlider(
                left, y, 150, 20, 30, 100, profile.getUpscaleCustomScalePercent(),
                value -> {
                    profile.setUpscaleCustomScalePercent(value);
                    return Text.translatable("text.oleaf.upscale.custom_scale", value);
                }));

        sharpenSlider = addDrawableChild(new SharpenSlider(right, y, 150, 20, profile.getUpscaleSharpen()));

        refreshEnabled();

        int footerY = this.height - 27;
        addDrawableChild(ButtonWidget.builder(Text.translatable("text.oleaf.graphics_hub.apply"), button -> applyNow())
                .dimensions(this.width / 2 - 155, footerY, 150, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
                .dimensions(this.width / 2 + 5, footerY, 150, 20)
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
        statusMessage = Text.translatable(
                profile.getUpscaleQuality().isEnabled()
                        ? (UpscalePipeline.get().isBlockedByIrisShaders()
                                ? "text.oleaf.graphics_hub.upscale_applied_iris"
                                : "text.oleaf.graphics_hub.upscale_applied")
                        : "text.oleaf.upscale.disabled"
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.translatable("text.oleaf.upscale.subtitle"),
                this.width / 2, 28, 0xFFA0A0A0);

        int infoY = this.height / 6 + 60;
        for (String line : describeInfo()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(line), this.width / 2, infoY, 0xFFC0C0C0);
            infoY += 12;
        }

        if (!statusMessage.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, statusMessage, this.width / 2, this.height - 42, 0xFFFFFF55);
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
            lines.add(Text.translatable("text.oleaf.graphics_hub.upscale_iris_pack_hint").getString());
        }
        return lines;
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    @FunctionalInterface
    private interface IntMessageFactory {
        Text create(int value);
    }

    private static final class PercentSlider extends SliderWidget {
        private final int min;
        private final int max;
        private final IntMessageFactory messageFactory;
        private int intValue;

        private PercentSlider(int x, int y, int width, int height, int min, int max, int value, IntMessageFactory messageFactory) {
            super(x, y, width, height, Text.empty(), (clamp(min, max, value) - min) / (double) (max - min));
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

    private final class SharpenSlider extends SliderWidget {
        private SharpenSlider(int x, int y, int width, int height, float sharpen) {
            super(x, y, width, height, Text.empty(), sharpen);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int percent = (int) Math.round(this.value * 100.0D);
            setMessage(Text.translatable("text.oleaf.graphics_hub.upscale_sharpen", percent));
        }

        @Override
        protected void applyValue() {
            profile.setUpscaleSharpen((float) this.value);
            updateMessage();
        }
    }
}
