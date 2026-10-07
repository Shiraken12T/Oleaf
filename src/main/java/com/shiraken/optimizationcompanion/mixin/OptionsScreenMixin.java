package com.shiraken.optimizationcompanion.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.shiraken.optimizationcompanion.gui.graphics.GraphicsHubScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Supplier;

/**
 * Redirects Options → Video Settings to the custom Graphics Hub.
 * Priority is elevated so this remains the entry point even if other mods also touch OptionsScreen.
 */
@Mixin(value = OptionsScreen.class, priority = 1100)
public abstract class OptionsScreenMixin {
    @WrapOperation(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/option/OptionsScreen;createButton(Lnet/minecraft/text/Text;Ljava/util/function/Supplier;)Lnet/minecraft/client/gui/widget/ButtonWidget;"
            )
    )
    private ButtonWidget optimizationCompanion$redirectVideoSettings(
            OptionsScreen instance,
            Text message,
            Supplier<Screen> screenSupplier,
            Operation<ButtonWidget> original
    ) {
        if (isVideoSettings(message)) {
            Supplier<Screen> hubSupplier = () -> new GraphicsHubScreen(instance);
            return original.call(instance, message, hubSupplier);
        }
        return original.call(instance, message, screenSupplier);
    }

    private static boolean isVideoSettings(Text message) {
        if (message.getContent() instanceof TranslatableTextContent content) {
            return "options.video".equals(content.getKey());
        }
        return false;
    }
}
