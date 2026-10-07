package com.shiraken.optimizationcompanion.upscale;

import com.shiraken.optimizationcompanion.OleafClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.lang.reflect.Method;

/**
 * Soft Iris detection + pipeline rebuild so shader packs reallocate at the
 * scaled framebuffer size (needed for real GPU savings with packs on).
 */
public final class IrisUpscaleCompat {
    private static final String[] API_CLASSES = {
            "net.irisshaders.iris.api.v0.IrisApi",
            "net.coderbot.iris.api.v0.IrisApi"
    };

    private static Boolean irisLoaded;
    private static Object apiInstance;
    private static Method isShaderPackInUse;
    private static Method getConfig;
    private static Method areShadersEnabled;
    private static Method setShadersEnabled;
    private static boolean lookupFailed;
    private static long lastRebuildMs;
    private static String lastRebuildReason = "";

    private IrisUpscaleCompat() {
    }

    public static boolean isIrisLoaded() {
        if (irisLoaded == null) {
            irisLoaded = FabricLoader.getInstance().isModLoaded("iris");
        }
        return irisLoaded;
    }

    public static boolean isShaderPackInUse() {
        if (!isIrisLoaded() || lookupFailed) {
            return false;
        }

        ensureApi();
        if (apiInstance == null || isShaderPackInUse == null) {
            return false;
        }

        try {
            Object result = isShaderPackInUse.invoke(apiInstance);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    /**
     * Toggle Iris shaders off/on (or reload world renderer) so gbuffers resize
     * to the current client framebuffer — the FSR1-style integration path.
     */
    public static void requestPipelineRebuild(String reason) {
        if (!isIrisLoaded() || !isShaderPackInUse()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (reason.equals(lastRebuildReason) && now - lastRebuildMs < 1500L) {
            return;
        }
        lastRebuildMs = now;
        lastRebuildReason = reason == null ? "" : reason;

        ensureApi();
        MinecraftClient client = MinecraftClient.getInstance();

        try {
            if (apiInstance != null && getConfig != null && areShadersEnabled != null && setShadersEnabled != null) {
                Object config = getConfig.invoke(apiInstance);
                Object enabled = areShadersEnabled.invoke(config);
                if (enabled instanceof Boolean && (Boolean) enabled) {
                    setShadersEnabled.invoke(config, false);
                    setShadersEnabled.invoke(config, true);
                    OleafClient.LOGGER.info(
                            "Rebuilt Iris shader pipeline for upscale ({})",
                            lastRebuildReason
                    );
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            OleafClient.LOGGER.warn("Iris shader toggle rebuild failed: {}", exception.toString());
        }

        if (client != null && client.worldRenderer != null) {
            client.worldRenderer.reload();
            OleafClient.LOGGER.info(
                    "Reloaded world renderer for upscale with Iris ({})",
                    lastRebuildReason
            );
        }
    }

    private static void ensureApi() {
        if (apiInstance != null || lookupFailed) {
            return;
        }

        for (String className : API_CLASSES) {
            try {
                Class<?> apiClass = Class.forName(className);
                Method getInstance = apiClass.getMethod("getInstance");
                Object instance = getInstance.invoke(null);
                Method packMethod = apiClass.getMethod("isShaderPackInUse");
                Method configMethod = apiClass.getMethod("getConfig");
                Object config = configMethod.invoke(instance);
                Class<?> configClass = config.getClass();
                Method areEnabled = findMethod(configClass, "areShadersEnabled", "getShadersEnabled", "isShadersEnabled");
                Method setEnabled = findMethod(configClass, "setShadersEnabled");

                apiInstance = instance;
                isShaderPackInUse = packMethod;
                getConfig = configMethod;
                areShadersEnabled = areEnabled;
                setShadersEnabled = setEnabled;
                return;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // try next candidate
            }
        }

        lookupFailed = true;
    }

    private static Method findMethod(Class<?> type, String... names) {
        for (String name : names) {
            try {
                if (name.startsWith("set")) {
                    return type.getMethod(name, boolean.class);
                }
                return type.getMethod(name);
            } catch (NoSuchMethodException ignored) {
                // try next
            }
        }
        return null;
    }
}
