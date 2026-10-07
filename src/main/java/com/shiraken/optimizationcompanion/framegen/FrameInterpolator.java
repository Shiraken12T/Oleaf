package com.shiraken.optimizationcompanion.framegen;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.shiraken.optimizationcompanion.OleafClient;
import com.shiraken.optimizationcompanion.config.ConfigManager;
import com.shiraken.optimizationcompanion.config.FrameGenSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalInt;
import java.util.concurrent.locks.LockSupport;

/**
 * In-mod frame generation for the Minecraft 1.21.11 Blaze3D backend.
 * <p>
 * Instead of the (incompatible) trick of injecting extra {@code glfwSwapBuffers}
 * calls, this uses the game's own present path: for a multiplier {@code M}, the
 * world is fully rendered on 1 of every {@code M} frames, and the remaining
 * {@code M-1} frames skip the expensive world render and instead show an
 * interpolated blend of the two most recent real frames. Because generated
 * frames are cheap, the render loop iterates faster and the displayed frame rate
 * rises (turn VSync off + raise the FPS cap to see the gain).
 * <p>
 * Soft pacing (when enabled) spaces presented frames toward
 * {@code realFrameEma / M} so high multipliers do not free-fire bursts of
 * near-zero-duration fakes between hitchy reals.
 * <p>
 * Adaptive FG (default on) detects real-frame hitches (chunk load / world gen),
 * invalidates blend history, forces a short stretch of real frames, skips soft
 * pacing during recovery, and temporarily lowers the effective multiplier when
 * the real-frame EMA is elevated — preventing 4x/6x from amplifying stutter.
 * <p>
 * All GPU work is done through the supported device API (render passes +
 * texture copies), exactly like the spatial upscaler, so it cannot leave the GL
 * state in a bad shape or black-screen the game.
 */
public final class FrameInterpolator {
    private static final FrameInterpolator INSTANCE = new FrameInterpolator();

    /** Soft floor so pacing never stalls the loop on tiny EMA estimates. */
    private static final long MIN_PACE_NANOS = 200_000L; // 0.2 ms
    /**
     * Soft ceiling on park duration. Keep low so pacing cannot drag a 700–800
     * presented FPS stream down toward ~120.
     */
    private static final long MAX_PACE_NANOS = 2_500_000L; // 2.5 ms
    private static final double REAL_EMA_ALPHA = 0.12D;
    /** Cap EMA samples so a single spike cannot permanently inflate pacing. */
    private static final long MAX_EMA_SAMPLE_NANOS = 50_000_000L; // ~50 ms
    /**
     * Absolute hitch threshold for the *render cost* of one real frame
     * (not the wall-clock gap between reals — that gap includes fake frames).
     */
    private static final long HITCH_THRESHOLD_NANOS = 55_000_000L; // 55 ms
    /** Relative hitch: real cost vs current EMA (only if also above min floor). */
    private static final double HITCH_EMA_FACTOR = 3.25D;
    private static final long HITCH_RELATIVE_MIN_NANOS = 28_000_000L; // 28 ms
    /** Skip blending (show curr) when reals are this far apart — do NOT cancel FG. */
    private static final long BLEND_MAX_INTERVAL_NANOS = 80_000_000L; // 80 ms
    /** Enter reduced (2x) mode only after EMA stays above this for several reals. */
    private static final long ADAPTIVE_REDUCE_ENTER_NANOS = 32_000_000L; // 32 ms
    /** Leave reduced mode once EMA falls back under this (hysteresis). */
    private static final long ADAPTIVE_REDUCE_EXIT_NANOS = 22_000_000L; // 22 ms
    private static final int ADAPTIVE_ENTER_STREAK = 4;
    private static final int ADAPTIVE_EXIT_STREAK = 6;

    private static final RenderPipeline BLEND_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.of(OleafClient.MOD_ID, "pipeline/frame_blend"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(Identifier.of(OleafClient.MOD_ID, "core/frame_blend"))
            .withSampler("PrevSampler")
            .withSampler("CurrSampler")
            .withUniform("BlendParams", UniformType.UNIFORM_BUFFER)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
            .build();

    private GpuTexture prevTex;
    private GpuTexture currTex;
    private GpuTextureView prevView;
    private GpuTextureView currView;
    private GpuBuffer paramsBuffer;

    private int texW = -1;
    private int texH = -1;
    private boolean prevValid;
    private boolean currValid;

    private long frameIndex;
    private boolean failed;

    // Per-frame decision (set in beginFrame, read by the mixin + processBeforeBlit).
    private boolean activeThisFrame;
    private boolean generatedThisFrame;
    private int phaseThisFrame;
    private int multiplierThisFrame = 2;
    private int configuredMultiplierThisFrame = 2;

    // Soft pacing / hitch state.
    /** Wall-clock mark when the current real frame's world render began. */
    private long realFrameStartNanos;
    /** Wall-clock mark of the previous real frame present (for blur interval). */
    private long lastRealPresentNanos;
    /** EMA of real-frame *render cost* (beginFrame → blit), not inter-real gap. */
    private long realEmaNanos;
    private long lastPresentNanos;
    private long lastRealIntervalNanos;
    private boolean pacingSuspended;
    /** Sticky adaptive level: configured, or min(2, configured). Never 1 (that caused 800→300 dips). */
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

    /** Effective multiplier after adaptive hitch protection (1 = FG suppressed). */
    public int getEffectiveMultiplier() {
        return multiplierThisFrame;
    }

    /** Configured multiplier for the current frame (before adaptive reduction). */
    public int getConfiguredMultiplier() {
        return configuredMultiplierThisFrame;
    }

    public boolean isInHitchRecovery() {
        // Kept for overlay API: true while sticky adaptive is holding a lower multiplier.
        return stickyEffectiveMultiplier > 0
                && configuredMultiplierThisFrame > stickyEffectiveMultiplier
                && stickyEffectiveMultiplier >= 2;
    }

    /** Real-frame EMA in milliseconds; 0 if unknown. */
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

    /** Called at the very start of {@code MinecraftClient.render}. */
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

        // Always keep the configured cadence when possible. Never cancel generated
        // frames because of blend-interval — that used to force all-real frames
        // and drop 800 FPS to ~300. Blur protection lives in presentGenerated().
        phaseThisFrame = (int) (frameIndex % multiplierThisFrame);
        boolean ready = prevValid && currValid;
        generatedThisFrame = ready && phaseThisFrame != 0;
        if (!generatedThisFrame) {
            realFrameStartNanos = System.nanoTime();
        }
        frameIndex++;
    }

    /**
     * Adaptive multiplier with hysteresis. Worst case is 2x (never fully off) so
     * FPS does not cliff from ~800 to ~300. Needs a sustained EMA streak to enter
     * or leave reduced mode.
     */
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

    /** Redirects {@code gameRenderer.render}: skip the world pass on generated frames. */
    public boolean shouldSkipWorldRender() {
        return activeThisFrame && generatedThisFrame;
    }

    /** Called just before {@code framebuffer.blitToScreen()}. */
    public void processBeforeBlit() {
        if (!activeThisFrame || failed) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        Framebuffer fb = client.getFramebuffer();
        if (fb == null || fb.getColorAttachment() == null || fb.getColorAttachmentView() == null) {
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
        // Blur protection: if the two reals are temporally far apart, show curr only.
        if (!prevValid || !currValid
                || (lastRealIntervalNanos > 0L && lastRealIntervalNanos > BLEND_MAX_INTERVAL_NANOS)) {
            copyTexture(currTex, fb.getColorAttachment());
            return;
        }
        float t = (float) phaseThisFrame / (float) multiplierThisFrame;
        blendInto(fb, smoothstep(t));
    }

    private void captureRealFrame(Framebuffer fb) {
        long now = System.nanoTime();
        boolean hitch = false;

        // Wall gap between reals (includes fake frames) — blur guard only.
        if (lastRealPresentNanos > 0L) {
            lastRealIntervalNanos = now - lastRealPresentNanos;
        }
        lastRealPresentNanos = now;

        // Render cost of THIS real frame (world pass → blit), not the 4x gap.
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

        // On hitch, drop prev so we never blend across the spike; only keep curr.
        if (hitch) {
            prevValid = false;
            copyTexture(fb.getColorAttachment(), currTex);
            currValid = true;
            return;
        }

        if (currValid) {
            copyTexture(currTex, prevTex);
            prevValid = true;
        }
        copyTexture(fb.getColorAttachment(), currTex);
        currValid = true;
    }

    private boolean isHitch(long realCost) {
        if (realCost >= HITCH_THRESHOLD_NANOS) {
            return true;
        }
        // Relative spikes only count when the frame is already somewhat slow —
        // avoids false "Recover" when EMA is tiny and a normal frame looks 2×.
        return realCost >= HITCH_RELATIVE_MIN_NANOS
                && realEmaNanos > 0L
                && realCost >= (long) (HITCH_EMA_FACTOR * realEmaNanos);
    }

    private void onHitchDetected() {
        // Invalidate blend history only — do NOT force a stretch of all-real frames
        // (that is what made FPS randomly cliff to ~300).
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

    /**
     * Soft-pace after the frame has been presented so real/fake cadence is even
     * instead of free-firing near-zero-duration generated frames.
     * Skipped during hitch recovery so we do not park after a spike.
     */
    public void processAfterPresent() {
        if (!activeThisFrame || failed || !settings().isPaceFrames()) {
            pacingSuspended = false;
            lastPresentNanos = System.nanoTime();
            return;
        }
        if (pacingSuspended) {
            // One-frame pause after a hitch, then resume — no multi-frame lockout.
            pacingSuspended = false;
            lastPresentNanos = System.nanoTime();
            return;
        }
        if (realEmaNanos <= 0L || multiplierThisFrame < 2) {
            lastPresentNanos = System.nanoTime();
            return;
        }

        // Pace only generated frames lightly; never park long enough to kill the FPS counter.
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
        if (prevTex != null && texW == w && texH == h) {
            return;
        }
        releaseTextures();

        prevTex = RenderSystem.getDevice().createTexture(
                () -> "OptimizationCompanion FG Prev", 15, TextureFormat.RGBA8, w, h, 1, 1);
        currTex = RenderSystem.getDevice().createTexture(
                () -> "OptimizationCompanion FG Curr", 15, TextureFormat.RGBA8, w, h, 1, 1);
        prevView = RenderSystem.getDevice().createTextureView(prevTex);
        currView = RenderSystem.getDevice().createTextureView(currTex);
        texW = w;
        texH = h;
        prevValid = false;
        currValid = false;
    }

    private void releaseTextures() {
        if (prevView != null) {
            prevView.close();
            prevView = null;
        }
        if (currView != null) {
            currView.close();
            currView = null;
        }
        if (prevTex != null) {
            prevTex.close();
            prevTex = null;
        }
        if (currTex != null) {
            currTex.close();
            currTex = null;
        }
        texW = -1;
        texH = -1;
        prevValid = false;
        currValid = false;
    }

    private void copyTexture(GpuTexture src, GpuTexture dst) {
        RenderSystem.getDevice().createCommandEncoder()
                .copyTextureToTexture(src, dst, 0, 0, 0, 0, 0, texW, texH);
    }

    private void blendInto(Framebuffer target, float mix) {
        if (paramsBuffer == null) {
            paramsBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "OptimizationCompanion FG BlendParams",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    16L);
        }

        ByteBuffer data = MemoryUtil.memAlloc(16);
        try {
            data.putFloat(mix);
            data.putFloat(0.0F);
            data.putFloat(0.0F);
            data.putFloat(0.0F);
            data.flip();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(paramsBuffer.slice(), data);
        } finally {
            MemoryUtil.memFree(data);
        }

        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "OptimizationCompanion FrameGen Blend",
                target.getColorAttachmentView(),
                OptionalInt.empty())) {
            renderPass.setPipeline(BLEND_PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("BlendParams", paramsBuffer);
            renderPass.bindTexture("PrevSampler", prevView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.bindTexture("CurrSampler", currView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.draw(0, 3);
        }
    }
}
