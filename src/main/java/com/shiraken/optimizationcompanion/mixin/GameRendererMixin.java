package com.shiraken.optimizationcompanion.mixin;

import com.shiraken.optimizationcompanion.upscale.UpscalePipeline;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Scale the world pass (Sodium / Iris inclusive), then restore native FB for HUD.
 */
@Mixin(value = GameRenderer.class, priority = 900)
public abstract class GameRendererMixin {
    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void optimizationCompanion$beginWorldUpscale(CallbackInfo ci) {
        UpscalePipeline.get().setShouldScale(true);
    }

    @Inject(method = "renderWorld", at = @At("RETURN"))
    private void optimizationCompanion$endWorldUpscale(CallbackInfo ci) {
        UpscalePipeline.get().setShouldScale(false);
    }
}
