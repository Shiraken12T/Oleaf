package com.shiraken.optimizationcompanion.gui.graphics;

import com.shiraken.optimizationcompanion.mods.DetectedMod;
import com.shiraken.optimizationcompanion.mods.OptimizationModDetector;
import com.shiraken.optimizationcompanion.settings.IrisSettingsBridge;
import com.shiraken.optimizationcompanion.settings.SettingsApplier;
import com.shiraken.optimizationcompanion.settings.SodiumSettingsBridge;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Vanilla-styled launcher for native Sodium / Minecraft / Iris video menus.
 */
public final class AdvancedGraphicsScreen extends Screen {
    private final Screen parent;
    private final boolean autoOpenNativeMenu;
    private boolean didAutoOpen;
    private Text statusMessage = Text.empty();

    public AdvancedGraphicsScreen(Screen parent) {
        this(parent, true);
    }

    public AdvancedGraphicsScreen(Screen parent, boolean autoOpenNativeMenu) {
        super(Text.translatable("text.oleaf.advanced.title"));
        this.parent = parent;
        this.autoOpenNativeMenu = autoOpenNativeMenu;
    }

    @Override
    protected void init() {
        clearChildren();

        int left = this.width / 2 - 155;
        int right = this.width / 2 + 5;
        int y = this.height / 6 + 24;
        int row = 24;

        boolean sodium = SodiumSettingsBridge.isAvailable();
        boolean iris = IrisSettingsBridge.isAvailable();

        ButtonWidget sodiumButton = ButtonWidget.builder(
                Text.translatable("text.oleaf.advanced.sodium"),
                button -> openSodium()
        ).dimensions(left, y, 150, 20).build();
        sodiumButton.active = sodium;
        addDrawableChild(sodiumButton);

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("text.oleaf.advanced.vanilla"),
                button -> this.client.setScreen(new VideoOptionsScreen(this, this.client.options))
        ).dimensions(right, y, 150, 20).build());

        y += row;
        ButtonWidget irisButton = ButtonWidget.builder(
                Text.translatable("text.oleaf.advanced.iris"),
                button -> openIris()
        ).dimensions(left, y, 150, 20).build();
        irisButton.active = iris;
        addDrawableChild(irisButton);

        ButtonWidget restoreButton = ButtonWidget.builder(
                Text.translatable("text.oleaf.advanced.restore"),
                button -> {
                    if (SettingsApplier.hasSessionBackup()) {
                        SettingsApplier.restoreSessionBackup();
                        statusMessage = Text.translatable("text.oleaf.advanced.restore_done");
                    } else {
                        statusMessage = Text.translatable("text.oleaf.advanced.restore_missing");
                    }
                }
        ).dimensions(right, y, 150, 20).build();
        restoreButton.active = SettingsApplier.hasSessionBackup();
        addDrawableChild(restoreButton);

        y += row + 8;
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("text.oleaf.advanced.open_default"),
                button -> openDefaultVideoMenu()
        ).dimensions(this.width / 2 - 155, y, 310, 20).build());

        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
                .dimensions(this.width / 2 - 100, this.height - 27, 200, 20)
                .build());

        // First open: jump into Sodium (or vanilla) so Advanced matches the pack's normal video UI.
        if (autoOpenNativeMenu && !didAutoOpen) {
            didAutoOpen = true;
            this.client.execute(this::openDefaultVideoMenu);
        }
    }

    private void openDefaultVideoMenu() {
        if (SodiumSettingsBridge.isAvailable()) {
            openSodium();
        } else {
            this.client.setScreen(new VideoOptionsScreen(this, this.client.options));
        }
    }

    private void openSodium() {
        Screen sodiumScreen = SodiumSettingsBridge.createOptionsScreen(this);
        if (sodiumScreen != null) {
            this.client.setScreen(sodiumScreen);
            return;
        }

        // Sodium is loaded but no GUI could be constructed — fall back to vanilla video options.
        if (SodiumSettingsBridge.isAvailable()) {
            statusMessage = Text.translatable("text.oleaf.advanced.sodium_fallback_vanilla");
            this.client.setScreen(new VideoOptionsScreen(this, this.client.options));
        } else {
            statusMessage = Text.translatable("text.oleaf.advanced.sodium_unavailable");
        }
    }

    private void openIris() {
        Screen irisScreen = IrisSettingsBridge.createOptionsScreen(this);
        if (irisScreen != null) {
            this.client.setScreen(irisScreen);
        } else {
            statusMessage = Text.translatable("text.oleaf.advanced.iris_unavailable");
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.translatable("text.oleaf.advanced.subtitle"),
                this.width / 2,
                36,
                0xFFA0A0A0
        );

        List<String> installed = new ArrayList<>();
        for (DetectedMod mod : OptimizationModDetector.detect()) {
            if (mod.installed()) {
                installed.add(mod.displayName());
            }
        }
        String detected = installed.isEmpty()
                ? Text.translatable("text.oleaf.advanced.detected_none").getString()
                : Text.translatable("text.oleaf.advanced.detected_list", String.join(", ", installed)).getString();
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(detected), this.width / 2, this.height / 6 + 100, 0xFFE0E0E0);

        if (!statusMessage.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, statusMessage, this.width / 2, this.height - 50, 0xFFFFFF55);
        }
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}
