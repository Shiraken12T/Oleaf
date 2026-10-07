package com.shiraken.optimizationcompanion.hud;

import com.shiraken.optimizationcompanion.config.CompanionConfig;
import com.shiraken.optimizationcompanion.config.ConfigManager;
import com.shiraken.optimizationcompanion.config.FrameGenSettings;
import com.shiraken.optimizationcompanion.config.OverlayAnchor;
import com.shiraken.optimizationcompanion.framegen.FrameInterpolator;
import com.shiraken.optimizationcompanion.settings.OptimizedCoreSettingsBridge;
import com.shiraken.optimizationcompanion.upscale.UpscaleSettings;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class PerformanceOverlay {
    private static final int PADDING = 5;
    private static final int LINE_HEIGHT = 10;
    private static boolean editMode = false;
    private static long lastRenderNanos = 0L;
    private static double averageFrameMs = 0.0D;

    private PerformanceOverlay() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> render(drawContext));
    }

    public static void toggleEditMode() {
        editMode = !editMode;
    }

    private static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        CompanionConfig.OverlaySettings settings = ConfigManager.getConfig().getOverlay();

        if (!settings.isEnabled() || client.options.hudHidden || client.player == null) {
            lastRenderNanos = System.nanoTime();
            return;
        }

        updateFrameTime();

        List<String> lines = collectLines(client, settings);
        if (lines.isEmpty()) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        int contentWidth = 0;
        for (String line : lines) {
            contentWidth = Math.max(contentWidth, textRenderer.getWidth(line));
        }

        if (editMode) {
            String editLine = Text.translatable("text.oleaf.overlay.editing").getString();
            lines.add(editLine);
            contentWidth = Math.max(contentWidth, textRenderer.getWidth(editLine));
        }

        int panelWidth = contentWidth + (PADDING * 2);
        int panelHeight = (lines.size() * LINE_HEIGHT) + (PADDING * 2);
        float scale = settings.getScale();
        int scaledPanelWidth = Math.round(panelWidth * scale);
        int scaledPanelHeight = Math.round(panelHeight * scale);
        int[] position = resolvePosition(client, settings, scaledPanelWidth, scaledPanelHeight);

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(position[0], position[1]);
        context.getMatrices().scale(scale, scale);

        int backgroundColor = alphaColor(settings.getOpacity(), 0x101010);
        int borderColor = editMode ? 0xFFFFCC33 : 0xFF3FA7FF;
        context.fill(0, 0, panelWidth, panelHeight, backgroundColor);
        if (editMode) {
            context.fill(0, 0, panelWidth, 1, borderColor);
            context.fill(0, panelHeight - 1, panelWidth, panelHeight, borderColor);
            context.fill(0, 0, 1, panelHeight, borderColor);
            context.fill(panelWidth - 1, 0, panelWidth, panelHeight, borderColor);
        }

        int y = PADDING;
        for (String line : lines) {
            context.drawText(textRenderer, line, PADDING, y, lineColor(line), false);
            y += LINE_HEIGHT;
        }

        context.getMatrices().popMatrix();
    }

    private static void updateFrameTime() {
        long now = System.nanoTime();
        if (lastRenderNanos != 0L) {
            double frameMs = (now - lastRenderNanos) / 1_000_000.0D;
            averageFrameMs = averageFrameMs == 0.0D ? frameMs : (averageFrameMs * 0.9D) + (frameMs * 0.1D);
        }
        lastRenderNanos = now;
    }

    private static List<String> collectLines(MinecraftClient client, CompanionConfig.OverlaySettings settings) {
        List<String> lines = new ArrayList<>();

        if (settings.isShowFps()) {
            lines.add("FPS: " + client.getCurrentFps());
        }

        if (settings.isShowFrameTime()) {
            lines.add("Frame: " + Math.round(averageFrameMs * 10.0D) / 10.0D + " ms (" + stabilityLabel() + ")");
        }

        if (settings.isShowMemory()) {
            Runtime runtime = Runtime.getRuntime();
            long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / 1024L / 1024L;
            long maxMb = runtime.maxMemory() / 1024L / 1024L;
            int percent = maxMb == 0L ? 0 : Math.round((usedMb * 100.0F) / maxMb);
            lines.add("Memory: " + usedMb + "/" + maxMb + " MB (" + percent + "%)");
        }

        if (settings.isShowPing() && client.getNetworkHandler() != null && client.player != null) {
            var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            if (entry != null) {
                lines.add("Ping: " + entry.getLatency() + " ms");
            }
        }

        if (settings.isShowEntities() && client.world != null) {
            lines.add("Entities: " + countEntities(client));
        }

        if (settings.isShowFsr()) {
            lines.add(fsrStatusLine());
        }

        if (settings.isShowFrameGen()) {
            lines.add(frameGenStatusLine());
        }

        if (settings.isShowOptimizedCore()) {
            String ocLine = optimizedCoreStatusLine();
            if (ocLine != null) {
                lines.add(ocLine);
            }
        }

        return lines;
    }

    private static String optimizedCoreStatusLine() {
        if (!OptimizedCoreSettingsBridge.isAvailable() || !OptimizedCoreSettingsBridge.isApiPresent()) {
            return null;
        }
        int culled = OptimizedCoreSettingsBridge.getCulledEntitiesLastFrame();
        if (culled < 0) {
            return null;
        }
        return "OC: culled " + culled;
    }

    private static String fsrStatusLine() {
        UpscaleSettings upscale = ConfigManager.getConfig().getUpscale();
        if (upscale == null || !upscale.isEnabled()) {
            return "FSR: Off";
        }
        return "FSR: " + upscale.getQuality().getDisplayName();
    }

    private static String frameGenStatusLine() {
        FrameGenSettings frameGen = ConfigManager.getConfig().getFrameGen();
        if (frameGen == null || !frameGen.isEnabled()) {
            return "FG: Off";
        }
        int configured = frameGen.getMultiplier();
        FrameInterpolator fg = FrameInterpolator.get();
        int effective = fg.getEffectiveMultiplier();
        if (frameGen.isAdaptiveFg() && effective > 0 && effective < configured) {
            return "FG: " + configured + "x→" + effective + "x";
        }
        return "FG: On (" + configured + "x)";
    }

    private static int countEntities(MinecraftClient client) {
        int count = 0;
        for (Entity ignored : client.world.getEntities()) {
            count++;
        }
        return count;
    }

    private static String stabilityLabel() {
        if (averageFrameMs <= 18.0D) {
            return "Stable";
        }
        if (averageFrameMs <= 28.0D) {
            return "Mixed";
        }
        return "Spiky";
    }

    private static int[] resolvePosition(
            MinecraftClient client,
            CompanionConfig.OverlaySettings settings,
            int panelWidth,
            int panelHeight
    ) {
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int offsetX = settings.getOffsetX();
        int offsetY = settings.getOffsetY();
        OverlayAnchor anchor = settings.getAnchor();

        return switch (anchor) {
            case TOP_LEFT -> new int[]{offsetX, offsetY};
            case TOP_RIGHT -> new int[]{screenWidth - panelWidth - offsetX, offsetY};
            case BOTTOM_LEFT -> new int[]{offsetX, screenHeight - panelHeight - offsetY};
            case BOTTOM_RIGHT -> new int[]{screenWidth - panelWidth - offsetX, screenHeight - panelHeight - offsetY};
            case CENTER -> new int[]{(screenWidth - panelWidth) / 2 + offsetX, (screenHeight - panelHeight) / 2 + offsetY};
        };
    }

    private static int alphaColor(float opacity, int rgb) {
        int alpha = Math.round(Math.max(0.0F, Math.min(1.0F, opacity)) * 255.0F);
        return (alpha << 24) | rgb;
    }

    private static int lineColor(String line) {
        if (line.contains("Spiky") || line.contains("90%")) {
            return 0xFFFF6666;
        }
        if (line.contains("Mixed")) {
            return 0xFFFFFF66;
        }
        return 0xFFFFFFFF;
    }
}
