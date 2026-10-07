package com.shiraken.optimizationcompanion.gui.graphics;

import com.shiraken.optimizationcompanion.mods.DetectedMod;
import com.shiraken.optimizationcompanion.mods.OptimizationModDetector;
import com.shiraken.optimizationcompanion.settings.IrisSettingsBridge;
import com.shiraken.optimizationcompanion.settings.SettingsApplier;
import com.shiraken.optimizationcompanion.settings.SodiumSettingsBridge;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Vanilla-styled launcher for native Sodium / Minecraft / Iris video menus.
 */
public final class AdvancedGraphicsScreen extends Screen {
    private final Screen parent;
    private final boolean autoOpenNativeMenu;
    private boolean didAutoOpen;
    private Component statusMessage = Component.empty();

    public AdvancedGraphicsScreen(Screen parent) {
        this(parent, true);
    }

    public AdvancedGraphicsScreen(Screen parent, boolean autoOpenNativeMenu) {
        super(Component.translatable("text.oleaf.advanced.title"));
        this.parent = parent;
        this.autoOpenNativeMenu = autoOpenNativeMenu;
    }

    @Override
    protected void init() {
        clearWidgets();

        int left = this.width / 2 - 155;
        int right = this.width / 2 + 5;
        int y = this.height / 6 + 24;
        int row = 24;

        boolean sodium = SodiumSettingsBridge.isAvailable();
        boolean iris = IrisSettingsBridge.isAvailable();

        Button sodiumButton = Button.builder(
                Component.translatable("text.oleaf.advanced.sodium"),
                button -> openSodium()
        ).bounds(left, y, 150, 20).build();
        sodiumButton.active = sodium;
        addRenderableWidget(sodiumButton);

        addRenderableWidget(Button.builder(
                Component.translatable("text.oleaf.advanced.vanilla"),
                button -> this.minecraft.gui.setScreen(new VideoSettingsScreen(this, this.minecraft, this.minecraft.options))
        ).bounds(right, y, 150, 20).build());

        y += row;
        Button irisButton = Button.builder(
                Component.translatable("text.oleaf.advanced.iris"),
                button -> openIris()
        ).bounds(left, y, 150, 20).build();
        irisButton.active = iris;
        addRenderableWidget(irisButton);

        Button restoreButton = Button.builder(
                Component.translatable("text.oleaf.advanced.restore"),
                button -> {
                    if (SettingsApplier.hasSessionBackup()) {
                        SettingsApplier.restoreSessionBackup();
                        statusMessage = Component.translatable("text.oleaf.advanced.restore_done");
                    } else {
                        statusMessage = Component.translatable("text.oleaf.advanced.restore_missing");
                    }
                }
        ).bounds(right, y, 150, 20).build();
        restoreButton.active = SettingsApplier.hasSessionBackup();
        addRenderableWidget(restoreButton);

        y += row + 8;
        addRenderableWidget(Button.builder(
                Component.translatable("text.oleaf.advanced.open_default"),
                button -> openDefaultVideoMenu()
        ).bounds(this.width / 2 - 155, y, 310, 20).build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(this.width / 2 - 100, this.height - 27, 200, 20)
                .build());

        // First open: jump into Sodium (or vanilla) so Advanced matches the pack's normal video UI.
        if (autoOpenNativeMenu && !didAutoOpen) {
            didAutoOpen = true;
            this.minecraft.execute(this::openDefaultVideoMenu);
        }
    }

    private void openDefaultVideoMenu() {
        if (SodiumSettingsBridge.isAvailable()) {
            openSodium();
        } else {
            this.minecraft.gui.setScreen(new VideoSettingsScreen(this, this.minecraft, this.minecraft.options));
        }
    }

    private void openSodium() {
        Screen sodiumScreen = SodiumSettingsBridge.createOptionsScreen(this);
        if (sodiumScreen != null) {
            this.minecraft.gui.setScreen(sodiumScreen);
            return;
        }

        // Sodium is loaded but no GUI could be constructed — fall back to vanilla video options.
        if (SodiumSettingsBridge.isAvailable()) {
            statusMessage = Component.translatable("text.oleaf.advanced.sodium_fallback_vanilla");
            this.minecraft.gui.setScreen(new VideoSettingsScreen(this, this.minecraft, this.minecraft.options));
        } else {
            statusMessage = Component.translatable("text.oleaf.advanced.sodium_unavailable");
        }
    }

    private void openIris() {
        Screen irisScreen = IrisSettingsBridge.createOptionsScreen(this);
        if (irisScreen != null) {
            this.minecraft.gui.setScreen(irisScreen);
        } else {
            statusMessage = Component.translatable("text.oleaf.advanced.iris_unavailable");
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);
        context.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        context.centeredText(
                this.font,
                Component.translatable("text.oleaf.advanced.subtitle"),
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
                ? Component.translatable("text.oleaf.advanced.detected_none").getString()
                : Component.translatable("text.oleaf.advanced.detected_list", String.join(", ", installed)).getString();
        context.centeredText(this.font, Component.literal(detected), this.width / 2, this.height / 6 + 100, 0xFFE0E0E0);

        if (!statusMessage.getString().isEmpty()) {
            context.centeredText(this.font, statusMessage, this.width / 2, this.height - 50, 0xFFFFFF55);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}
