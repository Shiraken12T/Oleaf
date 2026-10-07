package com.shiraken.optimizationcompanion.config;

import java.util.List;

public enum Preset {
    LOW_END_LAPTOP(
            "Low-End Laptop",
            List.of(
                    "Lower render distance and visual extras",
                    "Aggressive OptimizedCore culling, particle budget, and idle FPS",
                    "Hardware auto-tune leans stability on weak machines",
                    "Skip distant client ticks when OptimizedCore is loaded"
            )
    ),
    BALANCED_SURVIVAL(
            "Balanced Survival",
            List.of(
                    "Medium render distance and balanced quality",
                    "Normal OptimizedCore culling — hidden entities only",
                    "Particle budget and idle FPS tuned for everyday play",
                    "Hardware auto-tune stays on for mixed machines"
            )
    ),
    PVP_COMPETITIVE(
            "PvP / Competitive",
            List.of(
                    "Favor stable frame times and clear visuals",
                    "Avoid distracting visual effects",
                    "OptimizedCore culling tuned to reduce pop-in, not on-screen entities",
                    "Idle FPS reduction stays mild so alt-tab does not throttle"
            )
    ),
    BUILDER_SHADERS(
            "Builder + Shaders",
            List.of(
                    "Favor visual quality with reasonable FPS",
                    "Gentle OptimizedCore culling to avoid sudden build pop-in",
                    "Shader-friendly quality; particle budget stays generous",
                    "Hardware auto-tune leans quality on capable machines"
            )
    );

    private final String displayName;
    private final List<String> summary;

    Preset(String displayName, List<String> summary) {
        this.displayName = displayName;
        this.summary = summary;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getSummary() {
        return summary;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
