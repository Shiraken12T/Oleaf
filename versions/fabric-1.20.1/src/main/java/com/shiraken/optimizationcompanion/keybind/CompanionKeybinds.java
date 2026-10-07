package com.shiraken.optimizationcompanion.keybind;

import com.shiraken.optimizationcompanion.config.ConfigManager;
import com.shiraken.optimizationcompanion.hud.PerformanceOverlay;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class CompanionKeybinds {
    private static final String CATEGORY = "key.category.oleaf";

    private static KeyBinding toggleOverlay;
    private static KeyBinding editOverlay;
    private static KeyBinding resetOverlay;

    private CompanionKeybinds() {
    }

    public static void register() {
        toggleOverlay = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.oleaf.toggle_overlay",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                CATEGORY
        ));
        editOverlay = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.oleaf.edit_overlay",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                CATEGORY
        ));
        resetOverlay = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.oleaf.reset_overlay",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleOverlay.wasPressed()) {
                var overlay = ConfigManager.getConfig().getOverlay();
                overlay.setEnabled(!overlay.isEnabled());
                ConfigManager.save();
            }

            while (editOverlay.wasPressed()) {
                PerformanceOverlay.toggleEditMode();
            }

            while (resetOverlay.wasPressed()) {
                ConfigManager.getConfig().getOverlay().resetPosition();
                ConfigManager.save();
            }
        });
    }
}
