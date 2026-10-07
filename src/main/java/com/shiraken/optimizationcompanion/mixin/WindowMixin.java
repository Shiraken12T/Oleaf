package com.shiraken.optimizationcompanion.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.shiraken.optimizationcompanion.upscale.UpscalePipeline;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While the world pass is active, report a scaled framebuffer size so Sodium and
 * vanilla viewports match the swapped low-res target. HUD uses native size again after.
 * <p>
 * Critical: 1.21.x rendering uses {@code getFramebufferWidth/Height}, not {@code getWidth/Height}.
 */
@Mixin(Window.class)
public abstract class WindowMixin {
    @ModifyReturnValue(method = "getFramebufferWidth", at = @At("RETURN"))
    private int optimizationCompanion$scaleFramebufferWidth(int original) {
        return scale(original);
    }

    @ModifyReturnValue(method = "getFramebufferHeight", at = @At("RETURN"))
    private int optimizationCompanion$scaleFramebufferHeight(int original) {
        return scale(original);
    }

    @Inject(method = "onFramebufferSizeChanged", at = @At("RETURN"))
    private void optimizationCompanion$onFramebufferSizeChanged(long window, int width, int height, CallbackInfo ci) {
        UpscalePipeline.get().onResolutionChanged();
    }

    @Inject(method = "updateFramebufferSize", at = @At("RETURN"))
    private void optimizationCompanion$updateFramebufferSize(CallbackInfo ci) {
        UpscalePipeline.get().onResolutionChanged();
    }

    private static int scale(int value) {
        double factor = UpscalePipeline.get().getCurrentScaleFactor();
        if (factor >= 0.999D) {
            return value;
        }
        return Math.max(1, (int) Math.round(value * factor));
    }
}
