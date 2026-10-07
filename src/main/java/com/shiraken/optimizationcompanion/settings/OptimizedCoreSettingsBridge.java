package com.shiraken.optimizationcompanion.settings;

import com.shiraken.optimizationcompanion.OleafClient;
import com.shiraken.optimizationcompanion.config.GraphicsProfile;
import com.shiraken.optimizationcompanion.upscale.IrisUpscaleCompat;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;

/**
 * Soft OptimizedCore integration via reflection.
 * Never hard-crashes when OptimizedCore is absent.
 */
public final class OptimizedCoreSettingsBridge {
    private static final String MOD_ID = "optimizedcore";
    private static final String API_CLASS = "com.shiraken.optimizedcore.api.OptimizedCoreApi";

    private static volatile boolean resolved;
    private static Method applyOleafPresetMethod;
    private static Method applyOleafPresetWithShadersMethod;
    private static Method getHardwareTierMethod;
    private static Method getCulledEntitiesLastFrameMethod;
    private static Method isFeatureActiveMethod;

    private OptimizedCoreSettingsBridge() {
    }

    public static boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static boolean isApiPresent() {
        resolveApi();
        return applyOleafPresetMethod != null
                || applyOleafPresetWithShadersMethod != null
                || getHardwareTierMethod != null
                || getCulledEntitiesLastFrameMethod != null
                || isFeatureActiveMethod != null;
    }

    public static void applyFromProfile(GraphicsProfile profile) {
        if (!isAvailable() || profile == null) {
            return;
        }

        try {
            resolveApi();
            boolean shadersActive = IrisUpscaleCompat.isShaderPackInUse();
            if (applyOleafPresetWithShadersMethod != null) {
                applyOleafPresetWithShadersMethod.invoke(
                        null,
                        profile.getPreset().name(),
                        profile.getGraphicsQuality().name(),
                        profile.getStabilityBias().name(),
                        shadersActive
                );
            } else if (applyOleafPresetMethod != null) {
                applyOleafPresetMethod.invoke(
                        null,
                        profile.getPreset().name(),
                        profile.getGraphicsQuality().name(),
                        profile.getStabilityBias().name()
                );
            } else {
                OleafClient.LOGGER.debug("OptimizedCore is loaded but OptimizedCoreApi.applyOleafPreset is missing");
                return;
            }
            OleafClient.LOGGER.info(
                    "Applied OptimizedCore preset {} (quality={}, stability={}, shaders={})",
                    profile.getPreset().name(),
                    profile.getGraphicsQuality().name(),
                    profile.getStabilityBias().name(),
                    shadersActive
            );
        } catch (ReflectiveOperationException | RuntimeException exception) {
            OleafClient.LOGGER.warn("Failed to apply OptimizedCore settings", exception);
        }
    }

    public static String getHardwareTier() {
        if (!isAvailable()) {
            return null;
        }
        try {
            resolveApi();
            if (getHardwareTierMethod == null) {
                return null;
            }
            Object result = getHardwareTierMethod.invoke(null);
            return result instanceof String text && !text.isBlank() ? text : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            OleafClient.LOGGER.debug("Unable to read OptimizedCore hardware tier", exception);
            return null;
        }
    }

    public static int getCulledEntitiesLastFrame() {
        if (!isAvailable()) {
            return -1;
        }
        try {
            resolveApi();
            if (getCulledEntitiesLastFrameMethod == null) {
                return -1;
            }
            Object result = getCulledEntitiesLastFrameMethod.invoke(null);
            if (result instanceof Integer count) {
                return count;
            }
            if (result instanceof Number number) {
                return number.intValue();
            }
            return -1;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            OleafClient.LOGGER.debug("Unable to read OptimizedCore culled-entity count", exception);
            return -1;
        }
    }

    public static boolean isFeatureActive(String featureId) {
        if (!isAvailable() || featureId == null) {
            return false;
        }
        try {
            resolveApi();
            if (isFeatureActiveMethod == null) {
                return false;
            }
            Object result = isFeatureActiveMethod.invoke(null, featureId);
            return result instanceof Boolean flag && flag;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            OleafClient.LOGGER.debug("Unable to query OptimizedCore feature {}", featureId, exception);
            return false;
        }
    }

    private static void resolveApi() {
        if (resolved) {
            return;
        }
        synchronized (OptimizedCoreSettingsBridge.class) {
            if (resolved) {
                return;
            }
            if (!isAvailable()) {
                resolved = true;
                return;
            }
            try {
                Class<?> api = Class.forName(API_CLASS);
                applyOleafPresetMethod = findMethod(api, "applyOleafPreset", String.class, String.class, String.class);
                applyOleafPresetWithShadersMethod = findMethod(
                        api,
                        "applyOleafPreset",
                        String.class,
                        String.class,
                        String.class,
                        Boolean.class
                );
                getHardwareTierMethod = findMethod(api, "getHardwareTier");
                getCulledEntitiesLastFrameMethod = findMethod(api, "getCulledEntitiesLastFrame");
                isFeatureActiveMethod = findMethod(api, "isFeatureActive", String.class);
            } catch (ClassNotFoundException exception) {
                OleafClient.LOGGER.debug("OptimizedCoreApi class missing: {}", API_CLASS);
            } catch (LinkageError | RuntimeException exception) {
                OleafClient.LOGGER.warn("Unable to bind OptimizedCoreApi", exception);
            }
            resolved = true;
        }
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            return type.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException exception) {
            OleafClient.LOGGER.debug("OptimizedCoreApi method missing: {}", name);
            return null;
        }
    }
}
