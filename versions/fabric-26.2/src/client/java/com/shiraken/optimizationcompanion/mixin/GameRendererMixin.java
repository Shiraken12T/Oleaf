package com.shiraken.optimizationcompanion.mixin;

import com.shiraken.optimizationcompanion.upscale.UpscalePipeline;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks world rendering for future upscale enable/disable.
 * GPU scale is currently stubbed in UpscalePipeline.
 */
@Mixin(value = GameRenderer.class, priority = 900)
public abstract class GameRendererMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void optimizationCompanion$beginWorldUpscale(DeltaTracker deltaTracker, CallbackInfo ci) {
        UpscalePipeline.get().setShouldScale(true);
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void optimizationCompanion$endWorldUpscale(DeltaTracker deltaTracker, CallbackInfo ci) {
        UpscalePipeline.get().setShouldScale(false);
    }
}
