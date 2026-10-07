package com.shiraken.optimizationcompanion.mixin;

import com.mojang.blaze3d.platform.Window;
import com.shiraken.optimizationcompanion.upscale.UpscalePipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Resolution-change hook for upscale target resize (stubbed pipeline).
 */
@Mixin(Window.class)
public abstract class WindowMixin {
    @Inject(method = "setWidth", at = @At("RETURN"))
    private void optimizationCompanion$onWidth(int width, CallbackInfo ci) {
        UpscalePipeline.get().onResolutionChanged();
    }

    @Inject(method = "setHeight", at = @At("RETURN"))
    private void optimizationCompanion$onHeight(int height, CallbackInfo ci) {
        UpscalePipeline.get().onResolutionChanged();
    }
}
