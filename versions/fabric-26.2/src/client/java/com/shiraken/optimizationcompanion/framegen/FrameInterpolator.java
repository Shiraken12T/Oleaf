package com.shiraken.optimizationcompanion.framegen;

import com.shiraken.optimizationcompanion.OleafClient;
import com.shiraken.optimizationcompanion.config.ConfigManager;
import com.shiraken.optimizationcompanion.config.FrameGenSettings;

/**
 * Frame-generation facade for Minecraft 26.2.
 * <p>
 * The 1.21.11 blend/present path is stubbed: settings persist and the HUD can
 * still report FG state, but generated frames are not injected until the 26.2
 * renderFrame/present hooks are reimplemented.
 */
public final class FrameInterpolator {
    private static final FrameInterpolator INSTANCE = new FrameInterpolator();

    private boolean loggedStub;
    private boolean generatedFrame;
    private int presentedFps;
    private int realFps;
    private int effectiveMultiplier = 1;

    private FrameInterpolator() {
    }

    public static FrameInterpolator get() {
        return INSTANCE;
    }

    private FrameGenSettings settings() {
        return ConfigManager.getConfig().getFrameGen();
    }

    public void beginFrame() {
        generatedFrame = false;
        if (settings().isEnabled() && !loggedStub) {
            loggedStub = true;
            OleafClient.LOGGER.info(
                    "Frame generation settings enabled ({}x) — GPU FG path is stubbed on 26.2 for now",
                    settings().getMultiplier()
            );
        }
    }

    public boolean shouldSkipWorldRender() {
        return false;
    }

    public void processBeforeBlit() {
        // no-op stub
    }

    public void processAfterPresent() {
        // no-op stub
    }

    public boolean isGeneratedFrame() {
        return generatedFrame;
    }

    public int getPresentedFps() {
        return presentedFps;
    }

    public int getRealFps() {
        return realFps;
    }

    public int getEffectiveMultiplier() {
        return settings().isEnabled() ? Math.max(1, settings().getMultiplier()) : 1;
    }

    public void notifySettingsChanged() {
        effectiveMultiplier = getEffectiveMultiplier();
        loggedStub = false;
    }
}
