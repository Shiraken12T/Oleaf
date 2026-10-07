package com.shiraken.optimizationcompanion.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.shiraken.optimizationcompanion.gui.graphics.GraphicsHubScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.Supplier;

/**
 * Redirects Options → Video Settings to the custom Graphics Hub.
 */
@Mixin(value = OptionsScreen.class, priority = 1100)
public abstract class OptionsScreenMixin {
    @WrapMethod(method = "openScreenButton")
    private Button optimizationCompanion$redirectVideoSettings(
            Component message,
            Supplier<Screen> screenSupplier,
            Operation<Button> original
    ) {
        if (isVideoSettings(message)) {
            OptionsScreen self = (OptionsScreen) (Object) this;
            return original.call(message, (Supplier<Screen>) () -> new GraphicsHubScreen(self));
        }
        return original.call(message, screenSupplier);
    }

    private static boolean isVideoSettings(Component message) {
        if (message.getContents() instanceof TranslatableContents content) {
            String key = content.getKey();
            return "options.video".equals(key) || "options.videoTitle".equals(key);
        }
        return false;
    }
}
