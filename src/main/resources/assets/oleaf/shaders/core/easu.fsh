#version 400

// AMD FidelityFX FSR1 — EASU (Edge Adaptive Spatial Upsampling) fragment pass.
// Based on AMD FidelityFX Super Resolution 1.0 (MIT).

#extension GL_ARB_shading_language_packing : enable
#extension GL_ARB_gpu_shader5 : enable
#extension GL_ARB_texture_gather : enable

#define A_GPU 1
#define A_GLSL 1
#define FSR_EASU_F 1

#moj_import <minecraft:globals.glsl>
#moj_import <oleaf:ffx_a.glsl>

uniform sampler2D InSampler;

out vec4 fragColor;

AF4 FsrEasuRF(AF2 p) {
    return textureGather(InSampler, p, 0);
}

AF4 FsrEasuGF(AF2 p) {
    return textureGather(InSampler, p, 1);
}

AF4 FsrEasuBF(AF2 p) {
    return textureGather(InSampler, p, 2);
}

#moj_import <oleaf:ffx_fsr1.glsl>

void main() {
    AU2 outputPixel = AU2(gl_FragCoord.xy);
    vec2 inputSize = vec2(textureSize(InSampler, 0));
    AU4 fsrConst0;
    AU4 fsrConst1;
    AU4 fsrConst2;
    AU4 fsrConst3;

    FsrEasuCon(
            fsrConst0,
            fsrConst1,
            fsrConst2,
            fsrConst3,
            inputSize.x,
            inputSize.y,
            inputSize.x,
            inputSize.y,
            ScreenSize.x,
            ScreenSize.y
    );

    AF3 colour;
    FsrEasuF(
            colour,
            outputPixel,
            fsrConst0,
            fsrConst1,
            fsrConst2,
            fsrConst3
    );

    fragColor = vec4(colour, 1.0);
}
