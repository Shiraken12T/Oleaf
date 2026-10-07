package com.shiraken.optimizationcompanion.mixin;

import com.shiraken.optimizationcompanion.framegen.FrameInterpolator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives in-mod frame generation through the 1.20.1 present path
 * ({@code GameRenderer.render(FJZ)} + {@code Framebuffer.draw(II)}).
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientRenderMixin {
    @Inject(method = "render(Z)V", at = @At("HEAD"))
    private void optimizationCompanion$beginFrame(boolean tick, CallbackInfo ci) {
        FrameInterpolator.get().beginFrame();
    }

    @Redirect(
            method = "render(Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/GameRenderer;render(FJZ)V"
            )
    )
    private void optimizationCompanion$maybeSkipWorldRender(GameRenderer instance, float tickDelta, long startTime, boolean tick) {
        if (!FrameInterpolator.get().shouldSkipWorldRender()) {
            instance.render(tickDelta, startTime, tick);
        }
    }

    @Inject(
            method = "render(Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gl/Framebuffer;draw(II)V"
            )
    )
    private void optimizationCompanion$beforeBlit(boolean tick, CallbackInfo ci) {
        FrameInterpolator.get().processBeforeBlit();
    }

    @Inject(
            method = "render(Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gl/Framebuffer;draw(II)V",
                    shift = At.Shift.AFTER
            )
    )
    private void optimizationCompanion$afterPresent(boolean tick, CallbackInfo ci) {
        FrameInterpolator.get().processAfterPresent();
    }
}
