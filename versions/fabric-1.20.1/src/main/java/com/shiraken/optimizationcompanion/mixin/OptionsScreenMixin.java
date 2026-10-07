package com.shiraken.optimizationcompanion.mixin;

import com.shiraken.optimizationcompanion.gui.graphics.GraphicsHubScreen;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Redirects Options → Video Settings to the custom Graphics Hub on 1.20.1,
 * where OptionsScreen builds buttons inline instead of via createButton().
 */
@Mixin(value = OptionsScreen.class, priority = 1100)
public abstract class OptionsScreenMixin extends Screen {
    protected OptionsScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void optimizationCompanion$redirectVideoSettings(CallbackInfo ci) {
        List<? extends Element> snapshot = new ArrayList<>(this.children());
        for (Element element : snapshot) {
            if (!(element instanceof ButtonWidget button)) {
                continue;
            }
            if (!isVideoSettings(button.getMessage())) {
                continue;
            }

            int x = button.getX();
            int y = button.getY();
            int width = button.getWidth();
            int height = button.getHeight();
            Text message = button.getMessage();

            this.remove(button);
            this.addDrawableChild(ButtonWidget.builder(message, ignored -> {
                if (this.client != null) {
                    this.client.setScreen(new GraphicsHubScreen(this));
                }
            }).dimensions(x, y, width, height).build());
            return;
        }
    }

    private static boolean isVideoSettings(Text message) {
        if (message.getContent() instanceof TranslatableTextContent content) {
            return "options.video".equals(content.getKey());
        }
        return false;
    }
}
