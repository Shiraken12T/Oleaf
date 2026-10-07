package com.shiraken.optimizationcompanion.mixin;

import com.shiraken.optimizationcompanion.framegen.FrameInterpolator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives in-mod frame generation through the game's own present path:
 * <ul>
 *   <li>{@link #optimizationCompanion$beginFrame} decides whether this frame is a
 *       real (world-rendered) frame or a generated (interpolated) one.</li>
 *   <li>{@link #optimizationCompanion$maybeSkipWorldRender} skips the expensive
 *       world pass on generated frames so the loop runs faster.</li>
 *   <li>{@link #optimizationCompanion$beforeBlit} captures real frames and blits
 *       an interpolated blend for generated ones, right before it reaches the
 *       screen.</li>
 *   <li>{@link #optimizationCompanion$afterPresent} soft-paces presented frames
 *       when Pace Frames is enabled.</li>
 * </ul>
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
                    target = "Lnet/minecraft/client/render/GameRenderer;render(Lnet/minecraft/client/render/RenderTickCounter;Z)V"
            )
    )
    private void optimizationCompanion$maybeSkipWorldRender(GameRenderer instance, RenderTickCounter counter, boolean tick) {
        if (!FrameInterpolator.get().shouldSkipWorldRender()) {
            instance.render(counter, tick);
        }
    }

    @Inject(
            method = "render(Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gl/Framebuffer;blitToScreen()V"
            )
    )
    private void optimizationCompanion$beforeBlit(boolean tick, CallbackInfo ci) {
        FrameInterpolator.get().processBeforeBlit();
    }

    @Inject(
            method = "render(Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gl/Framebuffer;blitToScreen()V",
                    shift = At.Shift.AFTER
            )
    )
    private void optimizationCompanion$afterPresent(boolean tick, CallbackInfo ci) {
        FrameInterpolator.get().processAfterPresent();
    }
}
