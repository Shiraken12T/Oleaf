package com.shiraken.optimizationcompanion.gui.graphics;

import com.shiraken.optimizationcompanion.config.CompanionConfig;
import com.shiraken.optimizationcompanion.config.ConfigManager;
import com.shiraken.optimizationcompanion.config.OverlayAnchor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/**
 * Vanilla-styled performance overlay settings (enable, anchor, line toggles).
 */
public final class OverlaySettingsScreen extends Screen {
    private final Screen parent;

    public OverlaySettingsScreen(Screen parent) {
        super(Text.translatable("text.oleaf.overlay_settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearChildren();

        CompanionConfig.OverlaySettings overlay = ConfigManager.getConfig().getOverlay();

        int left = this.width / 2 - 155;
        int right = this.width / 2 + 5;
        int y = this.height / 6;
        int row = 24;

        addDrawableChild(CyclingButtonWidget
                .builder((Boolean value) -> Text.translatable(value ? "options.on" : "options.off"))
                .values(true, false)
                .initially(overlay.isEnabled())
                .build(left, y, 150, 20, Text.translatable("text.oleaf.modmenu.overlay_enabled"),
                        (button, value) -> overlay.setEnabled(value)));

        addDrawableChild(CyclingButtonWidget
                .builder((OverlayAnchor value) -> Text.literal(value.getDisplayName()))
                .values(OverlayAnchor.values())
                .initially(overlay.getAnchor())
                .build(right, y, 150, 20, Text.translatable("text.oleaf.modmenu.overlay_anchor"),
                        (button, value) -> overlay.setAnchor(value)));

        y += row;
        addDrawableChild(new OffsetSlider(
                left, y, 150, 20,
                Text.translatable("text.oleaf.modmenu.overlay_offset_x"),
                overlay.getOffsetX(),
                overlay::setOffsetX
        ));
        addDrawableChild(new OffsetSlider(
                right, y, 150, 20,
                Text.translatable("text.oleaf.modmenu.overlay_offset_y"),
                overlay.getOffsetY(),
                overlay::setOffsetY
        ));

        y += row;
        addDrawableChild(CyclingButtonWidget
                .builder((Boolean value) -> Text.translatable(value ? "options.on" : "options.off"))
                .values(true, false)
                .initially(overlay.isShowFps())
                .build(left, y, 150, 20, Text.translatable("text.oleaf.modmenu.show_fps"),
                        (button, value) -> overlay.setShowFps(value)));
        addDrawableChild(CyclingButtonWidget
                .builder((Boolean value) -> Text.translatable(value ? "options.on" : "options.off"))
                .values(true, false)
                .initially(overlay.isShowFrameTime())
                .build(right, y, 150, 20, Text.translatable("text.oleaf.modmenu.show_frame_time"),
                        (button, value) -> overlay.setShowFrameTime(value)));

        y += row;
        addDrawableChild(CyclingButtonWidget
                .builder((Boolean value) -> Text.translatable(value ? "options.on" : "options.off"))
                .values(true, false)
                .initially(overlay.isShowMemory())
                .build(left, y, 150, 20, Text.translatable("text.oleaf.modmenu.show_memory"),
                        (button, value) -> overlay.setShowMemory(value)));
        addDrawableChild(CyclingButtonWidget
                .builder((Boolean value) -> Text.translatable(value ? "options.on" : "options.off"))
                .values(true, false)
                .initially(overlay.isShowPing())
                .build(right, y, 150, 20, Text.translatable("text.oleaf.modmenu.show_ping"),
                        (button, value) -> overlay.setShowPing(value)));

        y += row;
        addDrawableChild(CyclingButtonWidget
                .builder((Boolean value) -> Text.translatable(value ? "options.on" : "options.off"))
                .values(true, false)
                .initially(overlay.isShowEntities())
                .build(left, y, 150, 20, Text.translatable("text.oleaf.modmenu.show_entities"),
                        (button, value) -> overlay.setShowEntities(value)));
        addDrawableChild(CyclingButtonWidget
                .builder((Boolean value) -> Text.translatable(value ? "options.on" : "options.off"))
                .values(true, false)
                .initially(overlay.isShowFsr())
                .build(right, y, 150, 20, Text.translatable("text.oleaf.modmenu.show_fsr"),
                        (button, value) -> overlay.setShowFsr(value)));

        y += row;
        addDrawableChild(CyclingButtonWidget
                .builder((Boolean value) -> Text.translatable(value ? "options.on" : "options.off"))
                .values(true, false)
                .initially(overlay.isShowFrameGen())
                .build(left, y, 150, 20, Text.translatable("text.oleaf.modmenu.show_framegen"),
                        (button, value) -> overlay.setShowFrameGen(value)));

        int footerY = this.height - 27;
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
                .dimensions(this.width / 2 - 100, footerY, 200, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.translatable("text.oleaf.overlay_settings.subtitle"),
                this.width / 2,
                34,
                0xFFA0A0A0
        );
    }

    @Override
    public void close() {
        ConfigManager.save();
        this.client.setScreen(this.parent);
    }

    @FunctionalInterface
    private interface IntConsumer {
        void accept(int value);
    }

    private static final class OffsetSlider extends SliderWidget {
        private final Text label;
        private final IntConsumer consumer;
        private int intValue;

        private OffsetSlider(int x, int y, int width, int height, Text label, int value, IntConsumer consumer) {
            super(x, y, width, height, Text.empty(), (value + 300) / 600.0D);
            this.label = label;
            this.consumer = consumer;
            this.intValue = Math.max(-300, Math.min(300, value));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.translatable("text.oleaf.overlay_settings.offset", label.getString(), intValue));
        }

        @Override
        protected void applyValue() {
            intValue = -300 + (int) Math.round(this.value * 600.0D);
            intValue = Math.max(-300, Math.min(300, intValue));
            consumer.accept(intValue);
            updateMessage();
        }
    }
}
