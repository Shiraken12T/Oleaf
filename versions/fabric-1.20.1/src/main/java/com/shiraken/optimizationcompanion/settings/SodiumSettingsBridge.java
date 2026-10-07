package com.shiraken.optimizationcompanion.settings;

import com.shiraken.optimizationcompanion.OleafClient;
import com.shiraken.optimizationcompanion.config.Preset;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Soft Sodium / Reese's Sodium Options integration via reflection.
 * Never hard-crashes when Sodium is absent.
 */
public final class SodiumSettingsBridge {
    private static final String SODIUM_MOD_ID = "sodium";
    private static final String REESES_MOD_ID = "reeses-sodium-options";

    /** Current Sodium (1.21.11+) video screen. */
    private static final String SODIUM_VIDEO_SETTINGS = "net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen";
    /** Legacy Sodium options GUI (pre-rename). */
    private static final String SODIUM_OPTIONS_GUI = "net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI";
    private static final String SODIUM_OPTIONS_GUI_LEGACY = "me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI";
    /** Reese's Sodium Options replacement UI used by Fabulously Optimized. */
    private static final String REESES_VIDEO_OPTIONS =
            "me.flashyreese.mods.reeses_sodium_options.client.gui.SodiumVideoOptionsScreen";

    private static final String CLIENT_MOD = "net.caffeinemc.mods.sodium.client.SodiumClientMod";
    private static final String[] OPTIONS_CLASSES = {
            "net.caffeinemc.mods.sodium.client.gui.SodiumOptions",
            "net.caffeinemc.mods.sodium.client.gui.SodiumGameOptions"
    };

    private SodiumSettingsBridge() {
    }

    public static boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded(SODIUM_MOD_ID);
    }

    public static boolean isReesesAvailable() {
        return FabricLoader.getInstance().isModLoaded(REESES_MOD_ID);
    }

    /**
     * Opens the same Sodium video UI the pack would normally show.
     * Prefers Reese's screen on FO, then Sodium's VideoSettingsScreen / legacy GUI.
     */
    public static Screen createOptionsScreen(Screen parent) {
        if (!isAvailable()) {
            return null;
        }

        // Prefer RSO when present — FO replaces Sodium's default options UI with it.
        if (isReesesAvailable()) {
            Screen reeses = tryConstructScreen(REESES_VIDEO_OPTIONS, parent);
            if (reeses != null) {
                return reeses;
            }
            OleafClient.LOGGER.warn(
                    "Reese's Sodium Options is loaded but {} could not be opened; trying Sodium screens",
                    REESES_VIDEO_OPTIONS
            );
        }

        String[] candidates = {
                SODIUM_VIDEO_SETTINGS,
                SODIUM_OPTIONS_GUI,
                SODIUM_OPTIONS_GUI_LEGACY,
                REESES_VIDEO_OPTIONS
        };

        for (String className : candidates) {
            Screen screen = tryCreateScreen(className, parent);
            if (screen != null) {
                return screen;
            }
        }

        OleafClient.LOGGER.warn(
                "Unable to open Sodium settings screen (sodium={}, reeses={})",
                isAvailable(),
                isReesesAvailable()
        );
        return null;
    }

    private static Screen tryCreateScreen(String className, Screen parent) {
        try {
            Class<?> guiClass = Class.forName(className);
            try {
                Method createScreen = guiClass.getMethod("createScreen", Screen.class);
                Object screen = createScreen.invoke(null, parent);
                if (screen instanceof Screen result) {
                    return result;
                }
            } catch (NoSuchMethodException ignored) {
                // fall through to constructor
            }

            return tryConstructScreen(className, parent);
        } catch (ClassNotFoundException exception) {
            OleafClient.LOGGER.debug("Sodium screen class missing: {}", className);
            return null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            OleafClient.LOGGER.warn("Sodium screen candidate failed: {}", className, exception);
            return null;
        }
    }

    private static Screen tryConstructScreen(String className, Screen parent) {
        try {
            Class<?> guiClass = Class.forName(className);
            Constructor<?> constructor = guiClass.getConstructor(Screen.class);
            Object screen = constructor.newInstance(parent);
            if (screen instanceof Screen result) {
                return result;
            }
        } catch (ClassNotFoundException exception) {
            OleafClient.LOGGER.debug("Sodium screen class missing: {}", className);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            OleafClient.LOGGER.warn("Sodium screen constructor failed: {}", className, exception);
        }
        return null;
    }

    public static void applyPreset(Preset preset) {
        if (!isAvailable()) {
            return;
        }

        try {
            Object options = getSodiumOptions();
            if (options == null) {
                return;
            }

            Object quality = getFieldValue(options, "quality");
            Object performance = getFieldValue(options, "performance");
            if (performance == null) {
                return;
            }

            // Sodium 0.8+ quality fields differ from older weather/leaves enums; apply what still exists.
            switch (preset) {
                case LOW_END_LAPTOP -> {
                    setFieldIfPresent(quality, "enableVignette", false);
                    setFieldIfPresent(performance, "useBlockFaceCulling", true);
                    setFieldIfPresent(performance, "useEntityCulling", true);
                    setFieldIfPresent(performance, "useFogOcclusion", true);
                    setFieldIfPresent(performance, "animateOnlyVisibleTextures", true);
                    setEnumFieldIfPresent(performance, "chunkBuildDeferMode", "ALWAYS");
                    setEnumFieldIfPresent(quality, "weatherQuality", "FAST");
                    setEnumFieldIfPresent(quality, "leavesQuality", "FAST");
                }
                case BALANCED_SURVIVAL -> {
                    setFieldIfPresent(quality, "enableVignette", true);
                    setFieldIfPresent(performance, "useBlockFaceCulling", true);
                    setFieldIfPresent(performance, "useEntityCulling", true);
                    setFieldIfPresent(performance, "useFogOcclusion", true);
                    setFieldIfPresent(performance, "animateOnlyVisibleTextures", true);
                    setEnumFieldIfPresent(performance, "chunkBuildDeferMode", "ALWAYS");
                    setEnumFieldIfPresent(quality, "weatherQuality", "DEFAULT");
                    setEnumFieldIfPresent(quality, "leavesQuality", "DEFAULT");
                }
                case PVP_COMPETITIVE -> {
                    setFieldIfPresent(quality, "enableVignette", false);
                    setFieldIfPresent(performance, "useBlockFaceCulling", true);
                    setFieldIfPresent(performance, "useEntityCulling", true);
                    setFieldIfPresent(performance, "useFogOcclusion", true);
                    setFieldIfPresent(performance, "animateOnlyVisibleTextures", true);
                    setEnumFieldIfPresent(performance, "chunkBuildDeferMode", "ALWAYS");
                    setEnumFieldIfPresent(quality, "weatherQuality", "FAST");
                    setEnumFieldIfPresent(quality, "leavesQuality", "FAST");
                }
                case BUILDER_SHADERS -> {
                    setFieldIfPresent(quality, "enableVignette", true);
                    setFieldIfPresent(performance, "useBlockFaceCulling", true);
                    setFieldIfPresent(performance, "useEntityCulling", false);
                    setFieldIfPresent(performance, "useFogOcclusion", true);
                    setFieldIfPresent(performance, "animateOnlyVisibleTextures", false);
                    setEnumFieldIfPresent(performance, "chunkBuildDeferMode", "ALWAYS");
                    setEnumFieldIfPresent(quality, "weatherQuality", "FANCY");
                    setEnumFieldIfPresent(quality, "leavesQuality", "FANCY");
                }
            }

            writeSodiumOptions(options);
            OleafClient.LOGGER.info("Applied Sodium preset leanings for {}", preset.getDisplayName());
        } catch (ReflectiveOperationException | RuntimeException exception) {
            OleafClient.LOGGER.warn("Failed to apply Sodium settings for {}", preset.getDisplayName(), exception);
        }
    }

    public static Map<String, Object> captureBackup() {
        if (!isAvailable()) {
            return null;
        }

        try {
            Object options = getSodiumOptions();
            if (options == null) {
                return null;
            }

            Object quality = getFieldValue(options, "quality");
            Object performance = getFieldValue(options, "performance");
            Map<String, Object> backup = new LinkedHashMap<>();
            if (quality != null) {
                putIfReadable(backup, quality, "weatherQuality");
                putIfReadable(backup, quality, "leavesQuality");
                putIfReadable(backup, quality, "enableVignette");
                putIfReadable(backup, quality, "hiddenFluidCulling");
                putIfReadable(backup, quality, "improvedFluidShaping");
            }
            if (performance != null) {
                putIfReadable(backup, performance, "useBlockFaceCulling");
                putIfReadable(backup, performance, "useEntityCulling");
                putIfReadable(backup, performance, "useFogOcclusion");
                putIfReadable(backup, performance, "animateOnlyVisibleTextures");
                putIfReadable(backup, performance, "chunkBuildDeferMode");
            }
            return backup;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            OleafClient.LOGGER.warn("Unable to capture Sodium settings backup", exception);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static void restoreBackup(Object backupObject) {
        if (!isAvailable() || !(backupObject instanceof Map<?, ?> raw)) {
            return;
        }

        try {
            Map<String, Object> backup = (Map<String, Object>) raw;
            Object options = getSodiumOptions();
            if (options == null) {
                return;
            }

            Object quality = getFieldValue(options, "quality");
            Object performance = getFieldValue(options, "performance");
            if (quality != null) {
                restoreField(quality, "weatherQuality", backup.get("weatherQuality"));
                restoreField(quality, "leavesQuality", backup.get("leavesQuality"));
                restoreField(quality, "enableVignette", backup.get("enableVignette"));
                restoreField(quality, "hiddenFluidCulling", backup.get("hiddenFluidCulling"));
                restoreField(quality, "improvedFluidShaping", backup.get("improvedFluidShaping"));
            }
            if (performance != null) {
                restoreField(performance, "useBlockFaceCulling", backup.get("useBlockFaceCulling"));
                restoreField(performance, "useEntityCulling", backup.get("useEntityCulling"));
                restoreField(performance, "useFogOcclusion", backup.get("useFogOcclusion"));
                restoreField(performance, "animateOnlyVisibleTextures", backup.get("animateOnlyVisibleTextures"));
                restoreField(performance, "chunkBuildDeferMode", backup.get("chunkBuildDeferMode"));
            }
            writeSodiumOptions(options);
            OleafClient.LOGGER.info("Restored Sodium settings from session backup");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            OleafClient.LOGGER.warn("Unable to restore Sodium settings backup", exception);
        }
    }

    private static Object getSodiumOptions() throws ReflectiveOperationException {
        Class<?> clientMod = Class.forName(CLIENT_MOD);
        Method optionsMethod = clientMod.getMethod("options");
        return optionsMethod.invoke(null);
    }

    private static void writeSodiumOptions(Object options) throws ReflectiveOperationException {
        ReflectiveOperationException last = null;
        for (String className : OPTIONS_CLASSES) {
            try {
                Class<?> gameOptionsClass = Class.forName(className);
                Method write = gameOptionsClass.getMethod("writeToDisk", gameOptionsClass);
                write.invoke(null, options);
                return;
            } catch (ReflectiveOperationException exception) {
                last = exception;
            }
        }
        if (last != null) {
            throw last;
        }
    }

    private static void putIfReadable(Map<String, Object> backup, Object target, String name) {
        try {
            Object value = getFieldValue(target, name);
            if (value != null) {
                backup.put(name, value);
            }
        } catch (ReflectiveOperationException ignored) {
            // field absent on this Sodium version
        }
    }

    private static Object getFieldValue(Object target, String name) throws ReflectiveOperationException {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setFieldIfPresent(Object target, String name, Object value) {
        if (target == null) {
            return;
        }
        try {
            setField(target, name, value);
        } catch (ReflectiveOperationException ignored) {
            // optional field on this Sodium version
        }
    }

    private static void setEnumFieldIfPresent(Object target, String name, String enumConstant) {
        if (target == null) {
            return;
        }
        try {
            setEnumField(target, name, enumConstant);
        } catch (ReflectiveOperationException ignored) {
            // optional enum on this Sodium version
        }
    }

    private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void restoreField(Object target, String name, Object value) throws ReflectiveOperationException {
        if (value != null) {
            try {
                setField(target, name, value);
            } catch (NoSuchFieldException ignored) {
                // field absent on this Sodium version
            }
        }
    }

    private static void setEnumField(Object target, String name, String enumConstant) throws ReflectiveOperationException {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        Class<?> type = field.getType();
        if (!type.isEnum()) {
            return;
        }
        Object constant = Enum.valueOf(type.asSubclass(Enum.class), enumConstant);
        field.set(target, constant);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
