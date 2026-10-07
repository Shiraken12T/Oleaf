package com.shiraken.optimizationcompanion.keybind;

import com.shiraken.optimizationcompanion.config.ConfigManager;
import com.shiraken.optimizationcompanion.hud.PerformanceOverlay;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class CompanionKeybinds {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("oleaf", "category")
    );

    private static KeyMapping toggleOverlay;
    private static KeyMapping editOverlay;
    private static KeyMapping resetOverlay;

    private CompanionKeybinds() {
    }

    public static void register() {
        toggleOverlay = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.oleaf.toggle_overlay",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                CATEGORY
        ));
        editOverlay = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.oleaf.edit_overlay",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                CATEGORY
        ));
        resetOverlay = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.oleaf.reset_overlay",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleOverlay.consumeClick()) {
                var overlay = ConfigManager.getConfig().getOverlay();
                overlay.setEnabled(!overlay.isEnabled());
                ConfigManager.save();
            }

            while (editOverlay.consumeClick()) {
                PerformanceOverlay.toggleEditMode();
            }

            while (resetOverlay.consumeClick()) {
                ConfigManager.getConfig().getOverlay().resetPosition();
                ConfigManager.save();
            }
        });
    }
}
