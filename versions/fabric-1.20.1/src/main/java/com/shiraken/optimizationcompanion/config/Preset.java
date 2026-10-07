package com.shiraken.optimizationcompanion.config;

import java.util.List;

public enum Preset {
    LOW_END_LAPTOP(
            "Low-End Laptop",
            List.of(
                    "Lower render distance and visual extras",
                    "Prefer aggressive entity/block culling",
                    "Favor memory-saving options in FerriteCore and ModernFix",
                    "Strongly reduce unfocused FPS through Dynamic FPS"
            )
    ),
    BALANCED_SURVIVAL(
            "Balanced Survival",
            List.of(
                    "Use medium render distance and balanced quality settings",
                    "Prefer moderate culling",
                    "Keep conservative ImmediatelyFast options",
                    "Use normal Dynamic FPS idle behavior"
            )
    ),
    PVP_COMPETITIVE(
            "PvP / Competitive",
            List.of(
                    "Favor stable frame times and clear visuals",
                    "Avoid distracting visual effects",
                    "Keep culling tuned to reduce distracting pop-in",
                    "Keep unfocused FPS reduction mild or disabled"
            )
    ),
    BUILDER_SHADERS(
            "Builder + Shaders",
            List.of(
                    "Favor visual quality with reasonable FPS",
                    "Use gentle culling to avoid sudden build pop-in",
                    "Keep shader-friendly Sodium and Iris recommendations",
                    "Prefer memory settings suitable for larger worlds"
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
