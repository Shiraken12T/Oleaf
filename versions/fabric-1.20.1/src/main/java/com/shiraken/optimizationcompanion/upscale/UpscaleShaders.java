package com.shiraken.optimizationcompanion.upscale;

import com.shiraken.optimizationcompanion.OleafClient;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

/**
 * Registers classic core shaders used by the 1.20.1 upscale / frame-gen paths.
 */
public final class UpscaleShaders {
    private static ShaderProgram easu;
    private static ShaderProgram rcas;
    private static ShaderProgram sgsr1;
    private static ShaderProgram spatial;
    private static ShaderProgram frameBlend;
    private static boolean registered;

    private UpscaleShaders() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(new Identifier(OleafClient.MOD_ID, "easu"), VertexFormats.POSITION_TEXTURE, program -> easu = program);
            context.register(new Identifier(OleafClient.MOD_ID, "rcas"), VertexFormats.POSITION_TEXTURE, program -> rcas = program);
            context.register(new Identifier(OleafClient.MOD_ID, "sgsr1"), VertexFormats.POSITION_TEXTURE, program -> sgsr1 = program);
            context.register(new Identifier(OleafClient.MOD_ID, "spatial_upscale"), VertexFormats.POSITION_TEXTURE, program -> spatial = program);
            context.register(new Identifier(OleafClient.MOD_ID, "frame_blend"), VertexFormats.POSITION_TEXTURE, program -> frameBlend = program);
            OleafClient.LOGGER.info("Registered Oleaf core shaders for 1.20.1 upscale/FG");
        });
    }

    public static ShaderProgram easu() {
        return easu;
    }

    public static ShaderProgram rcas() {
        return rcas;
    }

    public static ShaderProgram sgsr1() {
        return sgsr1;
    }

    public static ShaderProgram spatial() {
        return spatial;
    }

    public static ShaderProgram frameBlend() {
        return frameBlend;
    }
}
