package com.shiraken.optimizationcompanion.gui.graphics;

import com.shiraken.optimizationcompanion.config.ConfigManager;
import com.shiraken.optimizationcompanion.config.FrameGenSettings;
import com.shiraken.optimizationcompanion.config.GraphicsProfile;
import com.shiraken.optimizationcompanion.settings.SettingsApplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/**
 * In-mod frame generation panel: enable interpolation, pick a multiplier, and
 * optionally apply FG-friendly video settings (VSync off). Max FPS stays under
 * Graphics Hub / Video Settings control.
 */
public final class FrameGenScreen extends Screen {
    private final Screen parent;
    private final GraphicsProfile profile;

    private Text statusMessage = Text.empty();

    public FrameGenScreen(Screen parent, GraphicsProfile profile) {
        super(Text.translatable("text.oleaf.framegen.title"));
        this.parent = parent;
        this.profile = profile;
    }

    @Override
    protected void init() {
        clearChildren();

        FrameGenSettings fg = ConfigManager.getConfig().getFrameGen();

        int cx = this.width / 2;
        int y = this.height / 2 - 20;
        int row = 24;

        addDrawableChild(CyclingButtonWidget
                .builder((Boolean value) -> Text.translatable(value ? "options.on" : "options.off"), fg.isEnabled())
                .values(true, false)
                .build(cx - 155, y, 150, 20, Text.translatable("text.oleaf.framegen.in_mod"),
                        (button, value) -> {
                            fg.setEnabled(value);
                            ConfigManager.save();
                        }));

        addDrawableChild(CyclingButtonWidget
                .builder(FrameGenScreen::multiplierLabel, fg.getMultiplier())
                .values(2, 3, 4, 5, 6)
                .build(cx + 5, y, 150, 20, Text.translatable("text.oleaf.framegen.multiplier"),
                        (button, value) -> {
                            fg.setMultiplier(value);
                            ConfigManager.save();
                        }));

        y += row;
        addDrawableChild(CyclingButtonWidget
                .builder((Boolean value) -> Text.translatable(value ? "options.on" : "options.off"), fg.isPaceFrames())
                .values(true, false)
                .build(cx - 155, y, 150, 20, Text.translatable("text.oleaf.framegen.pace"),
                        (button, value) -> {
                            fg.setPaceFrames(value);
                            ConfigManager.save();
                        }));

        addDrawableChild(CyclingButtonWidget
                .builder((Boolean value) -> Text.translatable(value ? "options.on" : "options.off"), fg.isAdaptiveFg())
                .values(true, false)
                .build(cx + 5, y, 150, 20, Text.translatable("text.oleaf.framegen.adaptive"),
                        (button, value) -> {
                            fg.setAdaptiveFg(value);
                            ConfigManager.save();
                        }));

        y += row;
        addDrawableChild(ButtonWidget.builder(
                        Text.translatable("text.oleaf.framegen.apply_friendly"), button -> applyFriendly())
                .dimensions(cx - 155, y, 310, 20)
                .build());

        int footerY = this.height - 27;
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
                .dimensions(cx - 100, footerY, 200, 20)
                .build());
    }

    /** Turns VSync off for smoother FG cadence; does not change Max FPS. */
    private void applyFriendly() {
        profile.setVsync(false);
        SettingsApplier.apply(profile);
        statusMessage = Text.translatable("text.oleaf.framegen.applied");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("text.oleaf.framegen.subtitle"),
                this.width / 2, 34, 0xFFA0A0A0);

        Text tip = Text.translatable("text.oleaf.framegen.tip");
        context.drawCenteredTextWithShadow(this.textRenderer, tip, this.width / 2, this.height / 2 - 46, 0xFFB0B0B0);

        if (!statusMessage.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, statusMessage, this.width / 2, this.height - 46, 0xFFFFFF55);
        }
    }

    private static Text multiplierLabel(int value) {
        return Text.translatable("text.oleaf.framegen.multiplier_value", value);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}
