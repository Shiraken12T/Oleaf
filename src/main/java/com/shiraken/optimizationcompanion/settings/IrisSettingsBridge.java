package com.shiraken.optimizationcompanion.settings;

import com.shiraken.optimizationcompanion.OleafClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;

import java.lang.reflect.Constructor;

/**
 * Soft Iris integration for opening the shader pack screen.
 */
public final class IrisSettingsBridge {
    private static final String IRIS_MOD_ID = "iris";
    private static final String[] SCREEN_CLASSES = {
            "net.irisshaders.iris.gui.screen.ShaderPackScreen",
            "net.coderbot.iris.gui.screen.ShaderPackScreen"
    };

    private IrisSettingsBridge() {
    }

    public static boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded(IRIS_MOD_ID);
    }

    public static Screen createOptionsScreen(Screen parent) {
        if (!isAvailable()) {
            return null;
        }

        for (String className : SCREEN_CLASSES) {
            try {
                Class<?> screenClass = Class.forName(className);
                Constructor<?> constructor = screenClass.getConstructor(Screen.class);
                Object screen = constructor.newInstance(parent);
                if (screen instanceof Screen) {
                    return (Screen) screen;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // try next candidate
            }
        }

        OleafClient.LOGGER.warn("Unable to open Iris shader pack screen");
        return null;
    }
}
