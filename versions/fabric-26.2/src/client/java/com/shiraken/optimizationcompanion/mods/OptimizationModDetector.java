package com.shiraken.optimizationcompanion.mods;

import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

public final class OptimizationModDetector {
    private static final List<KnownOptimizationMod> KNOWN_MODS = List.of(
            new KnownOptimizationMod("sodium", "Sodium", "Renderer and video settings performance"),
            new KnownOptimizationMod("lithium", "Lithium", "Game logic and tick optimization"),
            new KnownOptimizationMod("ferritecore", "FerriteCore", "Memory usage reduction"),
            new KnownOptimizationMod("entityculling", "Entity Culling", "Skips rendering hidden entities"),
            new KnownOptimizationMod("moreculling", "More Culling", "Additional block/entity culling"),
            new KnownOptimizationMod("immediatelyfast", "ImmediatelyFast", "Rendering batching and UI speedups"),
            new KnownOptimizationMod("modernfix", "ModernFix", "Startup, memory, and bug fixes"),
            new KnownOptimizationMod("dynamic_fps", "Dynamic FPS", "Reduces FPS while idle or unfocused"),
            new KnownOptimizationMod("iris", "Iris", "Shader support and shader pipeline"),
            new KnownOptimizationMod("krypton", "Krypton", "Network stack optimization")
    );

    private OptimizationModDetector() {
    }

    public static List<DetectedMod> detect() {
        FabricLoader loader = FabricLoader.getInstance();
        return KNOWN_MODS.stream()
                .map(mod -> new DetectedMod(
                        mod.modId(),
                        mod.displayName(),
                        mod.purpose(),
                        loader.isModLoaded(mod.modId())
                ))
                .toList();
    }

    private record KnownOptimizationMod(String modId, String displayName, String purpose) {
    }
}
