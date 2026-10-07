package com.shiraken.optimizationcompanion.mixin;

import com.shiraken.optimizationcompanion.framegen.FrameInterpolator;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Frame-generation timing hooks. Present-path blend is stubbed on 26.2.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftClientRenderMixin {
    @Inject(method = "renderFrame", at = @At("HEAD"))
    private void optimizationCompanion$beginFrame(boolean tick, CallbackInfo ci) {
        FrameInterpolator.get().beginFrame();
    }

    @Inject(method = "renderFrame", at = @At("RETURN"))
    private void optimizationCompanion$afterPresent(boolean tick, CallbackInfo ci) {
        FrameInterpolator.get().processAfterPresent();
    }
}
