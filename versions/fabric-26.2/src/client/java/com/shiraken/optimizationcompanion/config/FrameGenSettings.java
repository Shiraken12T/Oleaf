package com.shiraken.optimizationcompanion.config;

/**
 * Persisted in-mod frame generation settings.
 * <p>
 * The generator interpolates between the last two fully rendered frames and
 * presents extra frames in between, raising the displayed frame rate without an
 * external app. It is interpolation-based, so it adds a little latency and can
 * show minor artifacts on fast motion — best for exploration/building.
 */
public class FrameGenSettings {
    private boolean enabled = false;
    /** Total presented frames per real frame: 2–6. */
    private int multiplier = 2;
    /** Evenly pace injected frames across the real frame interval. */
    private boolean paceFrames = true;
    /**
     * When real frames hitch (chunk load / world gen), temporarily reduce or
     * disable generated frames and skip blending stale history.
     */
    private boolean adaptiveFg = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(int multiplier) {
        this.multiplier = Math.max(2, Math.min(6, multiplier));
    }

    public boolean isPaceFrames() {
        return paceFrames;
    }

    public void setPaceFrames(boolean paceFrames) {
        this.paceFrames = paceFrames;
    }

    public boolean isAdaptiveFg() {
        return adaptiveFg;
    }

    public void setAdaptiveFg(boolean adaptiveFg) {
        this.adaptiveFg = adaptiveFg;
    }

    public boolean isActive() {
        return enabled && multiplier > 1;
    }

    public void sanitize() {
        setMultiplier(multiplier);
    }
}
