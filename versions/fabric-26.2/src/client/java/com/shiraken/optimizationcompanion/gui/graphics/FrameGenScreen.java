package com.shiraken.optimizationcompanion.gui.graphics;

import com.shiraken.optimizationcompanion.config.ConfigManager;
import com.shiraken.optimizationcompanion.config.FrameGenSettings;
import com.shiraken.optimizationcompanion.config.GraphicsProfile;
import com.shiraken.optimizationcompanion.settings.SettingsApplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * In-mod frame generation panel: enable interpolation, pick a multiplier, and
 * optionally apply FG-friendly video settings (VSync off). Max FPS stays under
 * Graphics Hub / Video Settings control.
 */
public final class FrameGenScreen extends Screen {
    private final Screen parent;
    private final GraphicsProfile profile;

    private Component statusMessage = Component.empty();

    public FrameGenScreen(Screen parent, GraphicsProfile profile) {
        super(Component.translatable("text.oleaf.framegen.title"));
        this.parent = parent;
        this.profile = profile;
    }

    @Override
    protected void init() {
        clearWidgets();

        FrameGenSettings fg = ConfigManager.getConfig().getFrameGen();

        int cx = this.width / 2;
        int y = this.height / 2 - 20;
        int row = 24;

        addRenderableWidget(CycleButton
                .builder((Boolean value) -> Component.translatable(value ? "options.on" : "options.off"), fg.isEnabled())
                .withValues(true, false).create(cx - 155, y, 150, 20, Component.translatable("text.oleaf.framegen.in_mod"),
                        (button, value) -> {
                            fg.setEnabled(value);
                            ConfigManager.save();
                        }));

        addRenderableWidget(CycleButton
                .builder(FrameGenScreen::multiplierLabel, fg.getMultiplier())
                .withValues(2, 3, 4, 5, 6).create(cx + 5, y, 150, 20, Component.translatable("text.oleaf.framegen.multiplier"),
                        (button, value) -> {
                            fg.setMultiplier(value);
                            ConfigManager.save();
                        }));

        y += row;
        addRenderableWidget(CycleButton
                .builder((Boolean value) -> Component.translatable(value ? "options.on" : "options.off"), fg.isPaceFrames())
                .withValues(true, false).create(cx - 155, y, 150, 20, Component.translatable("text.oleaf.framegen.pace"),
                        (button, value) -> {
                            fg.setPaceFrames(value);
                            ConfigManager.save();
                        }));

        addRenderableWidget(CycleButton
                .builder((Boolean value) -> Component.translatable(value ? "options.on" : "options.off"), fg.isAdaptiveFg())
                .withValues(true, false).create(cx + 5, y, 150, 20, Component.translatable("text.oleaf.framegen.adaptive"),
                        (button, value) -> {
                            fg.setAdaptiveFg(value);
                            ConfigManager.save();
                        }));

        y += row;
        addRenderableWidget(Button.builder(
                        Component.translatable("text.oleaf.framegen.apply_friendly"), button -> applyFriendly())
                .bounds(cx - 155, y, 310, 20)
                .build());

        int footerY = this.height - 27;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(cx - 100, footerY, 200, 20)
                .build());
    }

    /** Turns VSync off for smoother FG cadence; does not change Max FPS. */
    private void applyFriendly() {
        profile.setVsync(false);
        SettingsApplier.apply(profile);
        statusMessage = Component.translatable("text.oleaf.framegen.applied");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);
        context.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        context.centeredText(this.font,
                Component.translatable("text.oleaf.framegen.subtitle"),
                this.width / 2, 34, 0xFFA0A0A0);

        Component tip = Component.translatable("text.oleaf.framegen.tip");
        context.centeredText(this.font, tip, this.width / 2, this.height / 2 - 46, 0xFFB0B0B0);

        if (!statusMessage.getString().isEmpty()) {
            context.centeredText(this.font, statusMessage, this.width / 2, this.height - 46, 0xFFFFFF55);
        }
    }

    private static Component multiplierLabel(int value) {
        return Component.translatable("text.oleaf.framegen.multiplier_value", value);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}
