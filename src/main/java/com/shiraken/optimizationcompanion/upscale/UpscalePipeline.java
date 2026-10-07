package com.shiraken.optimizationcompanion.upscale;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.shiraken.optimizationcompanion.OleafClient;
import com.shiraken.optimizationcompanion.config.ConfigManager;
import com.shiraken.optimizationcompanion.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gl.WindowFramebuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profilers;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalInt;

/**
 * Spatial upscaler: render the world at a lower resolution, then upscale
 * before the HUD.
 * <p>
 * FSR 1 path uses real AMD FidelityFX EASU → RCAS (two passes). SGSR1 is a
 * single Qualcomm spatial pass. Other filters use {@code spatial_upscale.fsh}.
 */
public final class UpscalePipeline {
    private static final UpscalePipeline INSTANCE = new UpscalePipeline();

    private static final RenderPipeline EASU_PIPELINE = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
            .withLocation(Identifier.of(OleafClient.MOD_ID, "pipeline/fsr_easu"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(Identifier.of(OleafClient.MOD_ID, "core/easu"))
            .withSampler("InSampler")
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
            .build();

    private static final RenderPipeline RCAS_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.of(OleafClient.MOD_ID, "pipeline/fsr_rcas"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(Identifier.of(OleafClient.MOD_ID, "core/rcas"))
            .withSampler("InSampler")
            .withUniform("UpscaleParams", UniformType.UNIFORM_BUFFER)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
            .build();

    private static final RenderPipeline SGSR1_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.of(OleafClient.MOD_ID, "pipeline/sgsr1"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(Identifier.of(OleafClient.MOD_ID, "core/sgsr1"))
            .withSampler("InSampler")
            .withUniform("UpscaleParams", UniformType.UNIFORM_BUFFER)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
            .build();

    private static final RenderPipeline FALLBACK_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.of(OleafClient.MOD_ID, "pipeline/spatial_upscale"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(Identifier.of(OleafClient.MOD_ID, "core/spatial_upscale"))
            .withSampler("InSampler")
            .withUniform("UpscaleParams", UniformType.UNIFORM_BUFFER)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
            .build();

    private Framebuffer scaledTarget;
    private Framebuffer nativeTarget;
    private Framebuffer fsrIntermediate;
    private GpuBuffer paramsBuffer;
    private boolean shouldScale;
    private boolean useCustomShader = true;
    private boolean fsr1Available = true;
    private boolean sgsr1Available = true;
    private boolean loggedFallback;
    private boolean loggedActiveScale;
    private int lastScaledW = -1;
    private int lastScaledH = -1;
    private int lastNativeW = -1;
    private int lastNativeH = -1;
    private float lastAppliedScale = 1.0F;

    private UpscalePipeline() {
    }

    public static UpscalePipeline get() {
        return INSTANCE;
    }

    public UpscaleSettings settings() {
        return ConfigManager.getConfig().getUpscale();
    }

    public boolean isIrisLoaded() {
        return IrisUpscaleCompat.isIrisLoaded();
    }

    public boolean isBlockedByIrisShaders() {
        return IrisUpscaleCompat.isShaderPackInUse();
    }

    public float getActiveScale() {
        UpscaleSettings settings = settings();
        if (!settings.isEnabled()) {
            return 1.0F;
        }
        return Math.max(0.25F, Math.min(1.0F, settings.getScale()));
    }

    public double getCurrentScaleFactor() {
        return shouldScale ? getActiveScale() : 1.0D;
    }

    public void onResolutionChanged() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }
        if (getActiveScale() >= 0.999F) {
            return;
        }
        try {
            ensureTargets(client);
        } catch (Exception exception) {
            OleafClient.LOGGER.warn("Failed to resize upscale targets", exception);
        }
    }

    public void notifySettingsChanged() {
        loggedActiveScale = false;
        lastScaledW = -1;
        lastScaledH = -1;
        lastAppliedScale = 1.0F;
        onResolutionChanged();
        if (IrisUpscaleCompat.isShaderPackInUse()) {
            IrisUpscaleCompat.requestPipelineRebuild("upscale settings changed");
        }
    }

    public void setShouldScale(boolean enable) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) {
            return;
        }

        float scale = getActiveScale();
        if (scale >= 0.999F) {
            restoreNativeFramebuffer(client);
            shouldScale = false;
            return;
        }

        Profilers.get().push("oleaf_upscale");
        try {
            captureNativeFramebuffer(client);
            ensureTargets(client);

            if (enable) {
                shouldScale = true;
                setClientFramebuffer(client, scaledTarget);
                resizeFramebuffer(scaledTarget, lastScaledW, lastScaledH);
                logActiveOnce(scale);
            } else {
                shouldScale = false;
                blitUpscaled();
                if (nativeTarget != null) {
                    setClientFramebuffer(client, nativeTarget);
                }
            }
        } catch (Exception exception) {
            OleafClient.LOGGER.warn("Spatial upscale pass failed", exception);
            shouldScale = false;
            restoreNativeFramebuffer(client);
        } finally {
            Profilers.get().pop();
        }
    }

    private void captureNativeFramebuffer(MinecraftClient client) {
        Framebuffer current = client.getFramebuffer();
        if (current != null && current != scaledTarget && current != fsrIntermediate) {
            nativeTarget = current;
        }
        if (nativeTarget == null) {
            nativeTarget = current;
        }
    }

    private void restoreNativeFramebuffer(MinecraftClient client) {
        if (nativeTarget != null && client.getFramebuffer() != nativeTarget) {
            setClientFramebuffer(client, nativeTarget);
        }
    }

    private void ensureTargets(MinecraftClient client) {
        Window window = client.getWindow();
        boolean prev = shouldScale;
        shouldScale = false;
        int nativeW;
        int nativeH;
        try {
            nativeW = Math.max(1, window.getFramebufferWidth());
            nativeH = Math.max(1, window.getFramebufferHeight());
        } finally {
            shouldScale = prev;
        }

        float scale = getActiveScale();
        int scaledW = Math.max(1, Math.round(nativeW * scale));
        int scaledH = Math.max(1, Math.round(nativeH * scale));

        boolean sizeChanged = scaledW != lastScaledW || scaledH != lastScaledH
                || nativeW != lastNativeW || nativeH != lastNativeH
                || Math.abs(scale - lastAppliedScale) > 0.001F;
        lastScaledW = scaledW;
        lastScaledH = scaledH;
        lastNativeW = nativeW;
        lastNativeH = nativeH;
        lastAppliedScale = scale;

        if (scaledTarget == null) {
            scaledTarget = new WindowFramebuffer(scaledW, scaledH);
        } else {
            resizeFramebuffer(scaledTarget, scaledW, scaledH);
        }

        if (nativeTarget != null) {
            resizeFramebuffer(nativeTarget, nativeW, nativeH);
        }

        if (fsrIntermediate == null) {
            fsrIntermediate = new WindowFramebuffer(nativeW, nativeH);
        } else {
            resizeFramebuffer(fsrIntermediate, nativeW, nativeH);
        }

        if (sizeChanged && IrisUpscaleCompat.isShaderPackInUse()) {
            IrisUpscaleCompat.requestPipelineRebuild("framebuffer scale " + scaledW + "x" + scaledH);
        }
    }

    private static void resizeFramebuffer(Framebuffer framebuffer, int width, int height) {
        if (framebuffer == null) {
            return;
        }
        if (framebuffer.textureWidth != width || framebuffer.textureHeight != height) {
            framebuffer.resize(width, height);
        }
    }

    private void blitUpscaled() {
        if (scaledTarget == null || nativeTarget == null) {
            return;
        }

        RenderSystem.assertOnRenderThread();
        float sharpen = settings().getSharpen();
        UpscaleAlgorithm algorithm = settings().getAlgorithm();

        if (useCustomShader) {
            try {
                if (algorithm.isRealFsr1() && fsr1Available) {
                    try {
                        blitFsr1(sharpen);
                        return;
                    } catch (Exception fsrError) {
                        fsr1Available = false;
                        OleafClient.LOGGER.warn(
                                "FSR1 EASU/RCAS unavailable; using bicubic fallback. Cause: {}",
                                fsrError.toString()
                        );
                        blitSinglePass(FALLBACK_PIPELINE, sharpen, UpscaleAlgorithm.BICUBIC.getShaderIndex());
                        return;
                    }
                } else if (algorithm.isSgsr1() && sgsr1Available) {
                    try {
                        blitSinglePass(SGSR1_PIPELINE, sharpen, algorithm.getShaderIndex());
                        return;
                    } catch (Exception sgsrError) {
                        sgsr1Available = false;
                        OleafClient.LOGGER.warn(
                                "SGSR1 unavailable; using bicubic fallback. Cause: {}",
                                sgsrError.toString()
                        );
                        blitSinglePass(FALLBACK_PIPELINE, sharpen, UpscaleAlgorithm.BICUBIC.getShaderIndex());
                        return;
                    }
                } else {
                    int index = algorithm.isRealFsr1() || algorithm.isSgsr1()
                            ? UpscaleAlgorithm.BICUBIC.getShaderIndex()
                            : algorithm.getShaderIndex();
                    blitSinglePass(FALLBACK_PIPELINE, sharpen, index);
                    return;
                }
            } catch (Exception exception) {
                useCustomShader = false;
                if (!loggedFallback) {
                    OleafClient.LOGGER.warn(
                            "Custom spatial upscale shader unavailable; falling back to linear blit "
                                    + "(sharpen slider will not apply). Cause: {}",
                            exception.toString()
                    );
                    loggedFallback = true;
                }
            }
        }

        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Oleaf Upscale Fallback",
                nativeTarget.getColorAttachmentView(),
                OptionalInt.empty()
        )) {
            renderPass.setPipeline(RenderPipelines.TRACY_BLIT);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.bindTexture(
                    "InSampler",
                    scaledTarget.getColorAttachmentView(),
                    RenderSystem.getSamplerCache().get(FilterMode.LINEAR)
            );
            renderPass.draw(0, 3);
        }
    }

    /** Real FSR1: EASU (scaled → intermediate) then RCAS (intermediate → native). */
    private void blitFsr1(float sharpen) {
        if (fsrIntermediate == null) {
            fsrIntermediate = new WindowFramebuffer(lastNativeW, lastNativeH);
        }

        try (RenderPass easu = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Oleaf FSR1 EASU",
                fsrIntermediate.getColorAttachmentView(),
                OptionalInt.empty()
        )) {
            easu.setPipeline(EASU_PIPELINE);
            RenderSystem.bindDefaultUniforms(easu);
            easu.bindTexture(
                    "InSampler",
                    scaledTarget.getColorAttachmentView(),
                    RenderSystem.getSamplerCache().get(FilterMode.LINEAR)
            );
            easu.draw(0, 3);
        }

        writeParams(sharpen, 0);

        try (RenderPass rcas = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Oleaf FSR1 RCAS",
                nativeTarget.getColorAttachmentView(),
                OptionalInt.empty()
        )) {
            rcas.setPipeline(RCAS_PIPELINE);
            RenderSystem.bindDefaultUniforms(rcas);
            rcas.setUniform("UpscaleParams", paramsBuffer);
            rcas.bindTexture(
                    "InSampler",
                    fsrIntermediate.getColorAttachmentView(),
                    RenderSystem.getSamplerCache().get(FilterMode.NEAREST)
            );
            rcas.draw(0, 3);
        }
    }

    private void blitSinglePass(RenderPipeline pipeline, float sharpen, int algoIndex) {
        writeParams(sharpen, algoIndex);

        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Oleaf Upscale",
                nativeTarget.getColorAttachmentView(),
                OptionalInt.empty()
        )) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("UpscaleParams", paramsBuffer);
            renderPass.bindTexture(
                    "InSampler",
                    scaledTarget.getColorAttachmentView(),
                    RenderSystem.getSamplerCache().get(FilterMode.LINEAR)
            );
            renderPass.draw(0, 3);
        }
    }

    private void writeParams(float sharpen, int algoIndex) {
        if (paramsBuffer == null) {
            paramsBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "OptimizationCompanion UpscaleParams",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    16L
            );
        }

        ByteBuffer data = MemoryUtil.memAlloc(16);
        try {
            data.putFloat(sharpen);
            data.putFloat(lastScaledW);
            data.putFloat(lastScaledH);
            data.putFloat(algoIndex);
            data.flip();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(paramsBuffer.slice(), data);
        } finally {
            MemoryUtil.memFree(data);
        }
    }

    private void logActiveOnce(float scale) {
        if (loggedActiveScale) {
            return;
        }
        loggedActiveScale = true;
        OleafClient.LOGGER.info(
                "Spatial upscale ACTIVE: algo={} quality={} scale={} → {}x{} (IrisPack={}) sharpen={}",
                settings().getAlgorithm().getDisplayName(),
                settings().getQuality().getDisplayName(),
                scale,
                lastScaledW,
                lastScaledH,
                IrisUpscaleCompat.isShaderPackInUse(),
                settings().getSharpen()
        );
    }

    private static void setClientFramebuffer(MinecraftClient client, Framebuffer framebuffer) {
        if (framebuffer == null) {
            return;
        }
        ((MinecraftClientAccessor) (Object) client).optimizationCompanion$setFramebuffer(framebuffer);
    }
}
