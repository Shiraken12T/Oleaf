package com.shiraken.optimizationcompanion.framegen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import com.shiraken.optimizationcompanion.OleafClient;
import com.shiraken.optimizationcompanion.config.ConfigManager;
import com.shiraken.optimizationcompanion.config.FrameGenSettings;
import com.shiraken.optimizationcompanion.upscale.UpscaleShaders;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.util.concurrent.locks.LockSupport;

/**
 * In-mod frame generation for Minecraft 1.20.1's classic framebuffer present path.
 * <p>
 * Real frames capture the main color buffer; generated frames skip the world pass
 * and present a blend (or hold) of the two most recent real frames.
 */
public final class FrameInterpolator {
    private static final FrameInterpolator INSTANCE = new FrameInterpolator();

    private static final long MIN_PACE_NANOS = 200_000L;
    private static final long MAX_PACE_NANOS = 2_500_000L;
    private static final double REAL_EMA_ALPHA = 0.12D;
    private static final long MAX_EMA_SAMPLE_NANOS = 50_000_000L;
    private static final long HITCH_THRESHOLD_NANOS = 55_000_000L;
    private static final double HITCH_EMA_FACTOR = 3.25D;
    private static final long HITCH_RELATIVE_MIN_NANOS = 28_000_000L;
    private static final long BLEND_MAX_INTERVAL_NANOS = 80_000_000L;
    private static final long ADAPTIVE_REDUCE_ENTER_NANOS = 32_000_000L;
    private static final long ADAPTIVE_REDUCE_EXIT_NANOS = 22_000_000L;
    private static final int ADAPTIVE_ENTER_STREAK = 4;
    private static final int ADAPTIVE_EXIT_STREAK = 6;

    private SimpleFramebuffer prevFb;
    private SimpleFramebuffer currFb;

    private int texW = -1;
    private int texH = -1;
    private boolean prevValid;
    private boolean currValid;

    private long frameIndex;
    private boolean failed;

    private boolean activeThisFrame;
    private boolean generatedThisFrame;
    private int phaseThisFrame;
    private int multiplierThisFrame = 2;
    private int configuredMultiplierThisFrame = 2;

    private long realFrameStartNanos;
    private long lastRealPresentNanos;
    private long realEmaNanos;
    private long lastPresentNanos;
    private long lastRealIntervalNanos;
    private boolean pacingSuspended;
    private int stickyEffectiveMultiplier = 2;
    private int reduceEnterStreak;
    private int reduceExitStreak;

    private FrameInterpolator() {
    }

    public static FrameInterpolator get() {
        return INSTANCE;
    }

    private static FrameGenSettings settings() {
        return ConfigManager.getConfig().getFrameGen();
    }

    public int getEffectiveMultiplier() {
        return multiplierThisFrame;
    }

    public int getConfiguredMultiplier() {
        return configuredMultiplierThisFrame;
    }

    public boolean isInHitchRecovery() {
        return stickyEffectiveMultiplier > 0
                && configuredMultiplierThisFrame > stickyEffectiveMultiplier
                && stickyEffectiveMultiplier >= 2;
    }

    public double getRealEmaMs() {
        return realEmaNanos <= 0L ? 0.0D : realEmaNanos / 1_000_000.0D;
    }

    private boolean isActiveNow(MinecraftClient client) {
        if (failed || client == null) {
            return false;
        }
        if (client.world == null || client.currentScreen != null) {
            return false;
        }
        return settings().isActive();
    }

    public void beginFrame() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!isActiveNow(client)) {
            activeThisFrame = false;
            generatedThisFrame = false;
            frameIndex = 0;
            prevValid = false;
            currValid = false;
            realFrameStartNanos = 0L;
            lastRealPresentNanos = 0L;
            realEmaNanos = 0L;
            lastPresentNanos = 0L;
            lastRealIntervalNanos = 0L;
            pacingSuspended = false;
            stickyEffectiveMultiplier = 2;
            reduceEnterStreak = 0;
            reduceExitStreak = 0;
            multiplierThisFrame = 2;
            configuredMultiplierThisFrame = 2;
            return;
        }

        FrameGenSettings fg = settings();
        activeThisFrame = true;
        configuredMultiplierThisFrame = Math.max(2, Math.min(6, fg.getMultiplier()));
        multiplierThisFrame = resolveEffectiveMultiplier(fg, configuredMultiplierThisFrame);

        phaseThisFrame = (int) (frameIndex % multiplierThisFrame);
        boolean ready = prevValid && currValid;
        generatedThisFrame = ready && phaseThisFrame != 0;
        if (!generatedThisFrame) {
            realFrameStartNanos = System.nanoTime();
        }
        frameIndex++;
    }

    private int resolveEffectiveMultiplier(FrameGenSettings fg, int configured) {
        if (!fg.isAdaptiveFg() || realEmaNanos <= 0L) {
            stickyEffectiveMultiplier = configured;
            reduceEnterStreak = 0;
            reduceExitStreak = 0;
            return configured;
        }

        int reduced = Math.min(2, configured);
        boolean currentlyReduced = stickyEffectiveMultiplier < configured;

        if (!currentlyReduced) {
            if (realEmaNanos >= ADAPTIVE_REDUCE_ENTER_NANOS) {
                reduceEnterStreak++;
            } else {
                reduceEnterStreak = 0;
            }
            if (reduceEnterStreak >= ADAPTIVE_ENTER_STREAK) {
                stickyEffectiveMultiplier = reduced;
                reduceEnterStreak = 0;
                reduceExitStreak = 0;
            } else {
                stickyEffectiveMultiplier = configured;
            }
        } else {
            if (realEmaNanos <= ADAPTIVE_REDUCE_EXIT_NANOS) {
                reduceExitStreak++;
            } else {
                reduceExitStreak = 0;
            }
            if (reduceExitStreak >= ADAPTIVE_EXIT_STREAK) {
                stickyEffectiveMultiplier = configured;
                reduceEnterStreak = 0;
                reduceExitStreak = 0;
            } else {
                stickyEffectiveMultiplier = reduced;
            }
        }

        return Math.max(2, stickyEffectiveMultiplier);
    }

    public boolean shouldSkipWorldRender() {
        return activeThisFrame && generatedThisFrame;
    }

    public void processBeforeBlit() {
        if (!activeThisFrame || failed) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        Framebuffer fb = client.getFramebuffer();
        if (fb == null) {
            return;
        }

        try {
            ensureTextures(fb.textureWidth, fb.textureHeight);

            if (generatedThisFrame) {
                presentGenerated(fb);
            } else {
                captureRealFrame(fb);
            }
        } catch (Exception exception) {
            failed = true;
            OleafClient.LOGGER.warn(
                    "In-mod frame generation disabled after an error: {}", exception.toString());
            releaseTextures();
        }
    }

    private void presentGenerated(Framebuffer fb) {
        if (!prevValid || !currValid
                || (lastRealIntervalNanos > 0L && lastRealIntervalNanos > BLEND_MAX_INTERVAL_NANOS)) {
            copyFramebuffer(currFb, fb);
            return;
        }
        float t = (float) phaseThisFrame / (float) multiplierThisFrame;
        blendInto(fb, smoothstep(t));
    }

    private void captureRealFrame(Framebuffer fb) {
        long now = System.nanoTime();
        boolean hitch = false;

        if (lastRealPresentNanos > 0L) {
            lastRealIntervalNanos = now - lastRealPresentNanos;
        }
        lastRealPresentNanos = now;

        if (realFrameStartNanos > 0L) {
            long realCost = now - realFrameStartNanos;
            if (realCost > 0L) {
                hitch = settings().isAdaptiveFg() && isHitch(realCost);
                updateRealEma(realCost);
                if (hitch) {
                    onHitchDetected();
                }
            }
        }
        realFrameStartNanos = 0L;

        if (hitch) {
            prevValid = false;
            copyFramebuffer(fb, currFb);
            currValid = true;
            return;
        }

        if (currValid) {
            copyFramebuffer(currFb, prevFb);
            prevValid = true;
        }
        copyFramebuffer(fb, currFb);
        currValid = true;
    }

    private boolean isHitch(long realCost) {
        if (realCost >= HITCH_THRESHOLD_NANOS) {
            return true;
        }
        return realCost >= HITCH_RELATIVE_MIN_NANOS
                && realEmaNanos > 0L
                && realCost >= (long) (HITCH_EMA_FACTOR * realEmaNanos);
    }

    private void onHitchDetected() {
        pacingSuspended = true;
        if (realEmaNanos > MAX_EMA_SAMPLE_NANOS) {
            realEmaNanos = MAX_EMA_SAMPLE_NANOS;
        }
    }

    private void updateRealEma(long realDur) {
        long sample = Math.min(realDur, MAX_EMA_SAMPLE_NANOS);
        if (realEmaNanos <= 0L) {
            realEmaNanos = sample;
        } else {
            realEmaNanos = (long) (REAL_EMA_ALPHA * sample + (1.0D - REAL_EMA_ALPHA) * realEmaNanos);
        }
    }

    public void processAfterPresent() {
        if (!activeThisFrame || failed || !settings().isPaceFrames()) {
            pacingSuspended = false;
            lastPresentNanos = System.nanoTime();
            return;
        }
        if (pacingSuspended) {
            pacingSuspended = false;
            lastPresentNanos = System.nanoTime();
            return;
        }
        if (realEmaNanos <= 0L || multiplierThisFrame < 2) {
            lastPresentNanos = System.nanoTime();
            return;
        }

        long target = Math.min(MAX_PACE_NANOS, Math.max(MIN_PACE_NANOS, realEmaNanos / (multiplierThisFrame * 2L)));

        long now = System.nanoTime();
        if (lastPresentNanos > 0L && generatedThisFrame) {
            long elapsed = now - lastPresentNanos;
            long remaining = target - elapsed;
            if (remaining > 50_000L) {
                LockSupport.parkNanos(remaining);
            }
        }
        lastPresentNanos = System.nanoTime();
    }

    private static float smoothstep(float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        return t * t * (3.0F - 2.0F * t);
    }

    private void ensureTextures(int width, int height) {
        int w = Math.max(1, width);
        int h = Math.max(1, height);
        if (prevFb != null && texW == w && texH == h) {
            return;
        }
        releaseTextures();

        prevFb = new SimpleFramebuffer(w, h, true, MinecraftClient.IS_SYSTEM_MAC);
        currFb = new SimpleFramebuffer(w, h, true, MinecraftClient.IS_SYSTEM_MAC);
        texW = w;
        texH = h;
        prevValid = false;
        currValid = false;
    }

    private void releaseTextures() {
        if (prevFb != null) {
            prevFb.delete();
            prevFb = null;
        }
        if (currFb != null) {
            currFb.delete();
            currFb = null;
        }
        texW = -1;
        texH = -1;
        prevValid = false;
        currValid = false;
    }

    private void copyFramebuffer(Framebuffer src, Framebuffer dst) {
        dst.beginWrite(true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        src.draw(texW, texH);
        dst.endWrite();
    }

    private void blendInto(Framebuffer target, float mix) {
        ShaderProgram blend = UpscaleShaders.frameBlend();
        if (blend == null) {
            copyFramebuffer(currFb, target);
            return;
        }

        target.beginWrite(true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.setShader(() -> blend);
        RenderSystem.setShaderTexture(0, prevFb.getColorAttachment());
        RenderSystem.setShaderTexture(1, currFb.getColorAttachment());
        var mixUniform = blend.getUniform("Mix");
        if (mixUniform != null) {
            mixUniform.set(mix);
        }

        Matrix4f projection = new Matrix4f().setOrtho(0.0F, texW, texH, 0.0F, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(projection, VertexSorter.BY_Z);
        MatrixStack modelView = RenderSystem.getModelViewStack();
        modelView.push();
        modelView.loadIdentity();
        modelView.translate(0.0F, 0.0F, -2000.0F);
        RenderSystem.applyModelViewMatrix();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(0.0D, texH, 0.0D).texture(0.0F, 0.0F).next();
        buffer.vertex(texW, texH, 0.0D).texture(1.0F, 0.0F).next();
        buffer.vertex(texW, 0.0D, 0.0D).texture(1.0F, 1.0F).next();
        buffer.vertex(0.0D, 0.0D, 0.0D).texture(0.0F, 1.0F).next();
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        modelView.pop();
        RenderSystem.applyModelViewMatrix();
        target.endWrite();
    }
}
