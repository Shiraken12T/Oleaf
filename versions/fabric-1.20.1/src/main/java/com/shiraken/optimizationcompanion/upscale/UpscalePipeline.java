package com.shiraken.optimizationcompanion.upscale;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import com.shiraken.optimizationcompanion.OleafClient;
import com.shiraken.optimizationcompanion.config.ConfigManager;
import com.shiraken.optimizationcompanion.mixin.MinecraftClientAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/**
 * Spatial upscaler for Minecraft 1.20.1's classic OpenGL framebuffer path.
 * <p>
 * Renders the world into a lower-resolution target, then upscales with FSR1
 * (EASU→RCAS), SGSR1, or a spatial fallback before the HUD.
 */
public final class UpscalePipeline {
    private static final UpscalePipeline INSTANCE = new UpscalePipeline();

    private Framebuffer scaledTarget;
    private Framebuffer nativeTarget;
    private Framebuffer fsrIntermediate;
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

        try {
            captureNativeFramebuffer(client);
            ensureTargets(client);

            if (enable) {
                shouldScale = true;
                setClientFramebuffer(client, scaledTarget);
                scaledTarget.beginWrite(true);
                logActiveOnce(scale);
            } else {
                shouldScale = false;
                blitUpscaled();
                if (nativeTarget != null) {
                    setClientFramebuffer(client, nativeTarget);
                    nativeTarget.beginWrite(false);
                }
            }
        } catch (Exception exception) {
            OleafClient.LOGGER.warn("Spatial upscale pass failed", exception);
            shouldScale = false;
            restoreNativeFramebuffer(client);
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
            nativeTarget.beginWrite(false);
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
            scaledTarget = new SimpleFramebuffer(scaledW, scaledH, true, MinecraftClient.IS_SYSTEM_MAC);
        } else {
            resizeFramebuffer(scaledTarget, scaledW, scaledH);
        }

        if (nativeTarget != null) {
            resizeFramebuffer(nativeTarget, nativeW, nativeH);
        }

        if (fsrIntermediate == null) {
            fsrIntermediate = new SimpleFramebuffer(nativeW, nativeH, true, MinecraftClient.IS_SYSTEM_MAC);
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
            framebuffer.resize(width, height, MinecraftClient.IS_SYSTEM_MAC);
        }
    }

    private void blitUpscaled() {
        if (scaledTarget == null || nativeTarget == null) {
            return;
        }

        RenderSystem.assertOnRenderThread();
        float sharpen = settings().getSharpen();
        UpscaleAlgorithm algorithm = settings().getAlgorithm();

        nativeTarget.beginWrite(true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();

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
                        blitWithShader(UpscaleShaders.spatial(), sharpen, UpscaleAlgorithm.BICUBIC.getShaderIndex());
                        return;
                    }
                } else if (algorithm.isSgsr1() && sgsr1Available) {
                    try {
                        blitWithShader(UpscaleShaders.sgsr1(), sharpen, algorithm.getShaderIndex());
                        return;
                    } catch (Exception sgsrError) {
                        sgsr1Available = false;
                        OleafClient.LOGGER.warn(
                                "SGSR1 unavailable; using bicubic fallback. Cause: {}",
                                sgsrError.toString()
                        );
                        blitWithShader(UpscaleShaders.spatial(), sharpen, UpscaleAlgorithm.BICUBIC.getShaderIndex());
                        return;
                    }
                } else {
                    int index = algorithm.isRealFsr1() || algorithm.isSgsr1()
                            ? UpscaleAlgorithm.BICUBIC.getShaderIndex()
                            : algorithm.getShaderIndex();
                    blitWithShader(UpscaleShaders.spatial(), sharpen, index);
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

        // Linear framebuffer blit fallback.
        scaledTarget.draw(lastNativeW, lastNativeH);
    }

    private void blitFsr1(float sharpen) {
        if (fsrIntermediate == null) {
            fsrIntermediate = new SimpleFramebuffer(lastNativeW, lastNativeH, true, MinecraftClient.IS_SYSTEM_MAC);
        }

        fsrIntermediate.beginWrite(true);
        drawTextured(UpscaleShaders.easu(), scaledTarget, lastNativeW, lastNativeH, sharpen, 0);
        nativeTarget.beginWrite(true);
        drawTextured(UpscaleShaders.rcas(), fsrIntermediate, lastNativeW, lastNativeH, sharpen, 0);
    }

    private void blitWithShader(ShaderProgram shader, float sharpen, int algoIndex) {
        if (shader == null) {
            throw new IllegalStateException("Upscale shader not loaded");
        }
        nativeTarget.beginWrite(true);
        drawTextured(shader, scaledTarget, lastNativeW, lastNativeH, sharpen, algoIndex);
    }

    private void drawTextured(ShaderProgram shader, Framebuffer source, int outW, int outH, float sharpen, int algoIndex) {
        if (shader == null) {
            throw new IllegalStateException("Shader is null");
        }

        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, source.getColorAttachment());
        setUniform(shader, "Sharpen", sharpen);
        setUniform(shader, "InWidth", (float) source.textureWidth);
        setUniform(shader, "InHeight", (float) source.textureHeight);
        setUniform(shader, "OutWidth", (float) outW);
        setUniform(shader, "OutHeight", (float) outH);
        setUniform(shader, "Algo", (float) algoIndex);

        Matrix4f projection = new Matrix4f().setOrtho(0.0F, outW, outH, 0.0F, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(projection, VertexSorter.BY_Z);
        MatrixStack modelView = RenderSystem.getModelViewStack();
        modelView.push();
        modelView.loadIdentity();
        modelView.translate(0.0F, 0.0F, -2000.0F);
        RenderSystem.applyModelViewMatrix();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(0.0D, outH, 0.0D).texture(0.0F, 0.0F).next();
        buffer.vertex(outW, outH, 0.0D).texture(1.0F, 0.0F).next();
        buffer.vertex(outW, 0.0D, 0.0D).texture(1.0F, 1.0F).next();
        buffer.vertex(0.0D, 0.0D, 0.0D).texture(0.0F, 1.0F).next();
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        modelView.pop();
        RenderSystem.applyModelViewMatrix();
    }

    private static void setUniform(ShaderProgram shader, String name, float value) {
        var uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
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
