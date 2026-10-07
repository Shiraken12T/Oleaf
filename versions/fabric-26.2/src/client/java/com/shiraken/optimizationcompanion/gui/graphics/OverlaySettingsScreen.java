package com.shiraken.optimizationcompanion.gui.graphics;

import com.shiraken.optimizationcompanion.config.CompanionConfig;
import com.shiraken.optimizationcompanion.config.ConfigManager;
import com.shiraken.optimizationcompanion.config.OverlayAnchor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Vanilla-styled performance overlay settings (enable, anchor, line toggles).
 */
public final class OverlaySettingsScreen extends Screen {
    private final Screen parent;

    public OverlaySettingsScreen(Screen parent) {
        super(Component.translatable("text.oleaf.overlay_settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearWidgets();

        CompanionConfig.OverlaySettings overlay = ConfigManager.getConfig().getOverlay();

        int left = this.width / 2 - 155;
        int right = this.width / 2 + 5;
        int y = this.height / 6;
        int row = 24;

        addRenderableWidget(CycleButton
                .builder(value -> Component.translatable(value ? "options.on" : "options.off"), overlay.isEnabled())
                .withValues(true, false).create(left, y, 150, 20, Component.translatable("text.oleaf.modmenu.overlay_enabled"),
                        (button, value) -> overlay.setEnabled(value)));

        addRenderableWidget(CycleButton
                .builder((OverlayAnchor value) -> Component.literal(value.getDisplayName()), overlay.getAnchor())
                .withValues(OverlayAnchor.values())
                .create(right, y, 150, 20, Component.translatable("text.oleaf.modmenu.overlay_anchor"),
                        (button, value) -> overlay.setAnchor(value)));

        y += row;
        addRenderableWidget(new OffsetSlider(
                left, y, 150, 20,
                Component.translatable("text.oleaf.modmenu.overlay_offset_x"),
                overlay.getOffsetX(),
                overlay::setOffsetX
        ));
        addRenderableWidget(new OffsetSlider(
                right, y, 150, 20,
                Component.translatable("text.oleaf.modmenu.overlay_offset_y"),
                overlay.getOffsetY(),
                overlay::setOffsetY
        ));

        y += row;
        addRenderableWidget(CycleButton
                .builder(value -> Component.translatable(value ? "options.on" : "options.off"), overlay.isShowFps())
                .withValues(true, false).create(left, y, 150, 20, Component.translatable("text.oleaf.modmenu.show_fps"),
                        (button, value) -> overlay.setShowFps(value)));
        addRenderableWidget(CycleButton
                .builder(value -> Component.translatable(value ? "options.on" : "options.off"), overlay.isShowFrameTime())
                .withValues(true, false).create(right, y, 150, 20, Component.translatable("text.oleaf.modmenu.show_frame_time"),
                        (button, value) -> overlay.setShowFrameTime(value)));

        y += row;
        addRenderableWidget(CycleButton
                .builder(value -> Component.translatable(value ? "options.on" : "options.off"), overlay.isShowMemory())
                .withValues(true, false).create(left, y, 150, 20, Component.translatable("text.oleaf.modmenu.show_memory"),
                        (button, value) -> overlay.setShowMemory(value)));
        addRenderableWidget(CycleButton
                .builder(value -> Component.translatable(value ? "options.on" : "options.off"), overlay.isShowPing())
                .withValues(true, false).create(right, y, 150, 20, Component.translatable("text.oleaf.modmenu.show_ping"),
                        (button, value) -> overlay.setShowPing(value)));

        y += row;
        addRenderableWidget(CycleButton
                .builder(value -> Component.translatable(value ? "options.on" : "options.off"), overlay.isShowEntities())
                .withValues(true, false).create(left, y, 150, 20, Component.translatable("text.oleaf.modmenu.show_entities"),
                        (button, value) -> overlay.setShowEntities(value)));
        addRenderableWidget(CycleButton
                .builder(value -> Component.translatable(value ? "options.on" : "options.off"), overlay.isShowFsr())
                .withValues(true, false).create(right, y, 150, 20, Component.translatable("text.oleaf.modmenu.show_fsr"),
                        (button, value) -> overlay.setShowFsr(value)));

        y += row;
        addRenderableWidget(CycleButton
                .builder(value -> Component.translatable(value ? "options.on" : "options.off"), overlay.isShowFrameGen())
                .withValues(true, false).create(left, y, 150, 20, Component.translatable("text.oleaf.modmenu.show_framegen"),
                        (button, value) -> overlay.setShowFrameGen(value)));

        int footerY = this.height - 27;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(this.width / 2 - 100, footerY, 200, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);
        context.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        context.centeredText(
                this.font,
                Component.translatable("text.oleaf.overlay_settings.subtitle"),
                this.width / 2,
                34,
                0xFFA0A0A0
        );
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        this.minecraft.gui.setScreen(this.parent);
    }

    @FunctionalInterface
    private interface IntConsumer {
        void accept(int value);
    }

    private static final class OffsetSlider extends AbstractSliderButton {
        private final Component label;
        private final IntConsumer consumer;
        private int intValue;

        private OffsetSlider(int x, int y, int width, int height, Component label, int value, IntConsumer consumer) {
            super(x, y, width, height, Component.empty(), (value + 300) / 600.0D);
            this.label = label;
            this.consumer = consumer;
            this.intValue = Math.max(-300, Math.min(300, value));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("text.oleaf.overlay_settings.offset", label.getString(), intValue));
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
