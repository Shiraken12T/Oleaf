#version 400

// AMD FidelityFX FSR1 — RCAS (Robust Contrast Adaptive Sharpening) fragment pass.
// Applied AFTER EASU on the upscaled image.

#extension GL_ARB_shading_language_packing : enable
#extension GL_ARB_gpu_shader5 : enable

#define A_GPU 1
#define A_GLSL 1
#define FSR_RCAS_F 1

#moj_import <oleaf:ffx_a.glsl>

uniform sampler2D InSampler;

layout(std140) uniform UpscaleParams {
    vec4 Data; // x = sharpen 0..1
};

out vec4 fragColor;

AF4 FsrRcasLoadF(ASU2 p) {
    ivec2 size = textureSize(InSampler, 0);
    ivec2 position = clamp(ivec2(p), ivec2(0), size - ivec2(1));
    return texelFetch(InSampler, position, 0);
}

void FsrRcasInputF(inout AF1 r, inout AF1 g, inout AF1 b) {
}

#moj_import <oleaf:ffx_fsr1.glsl>

void main() {
    AU2 outputPixel = AU2(gl_FragCoord.xy);
    AU4 fsrConst0;

    float sharpen = clamp(Data.x, 0.0, 1.0);
    // FSR RCAS: 0 stops = sharpest; higher stops = softer.
    float stops = (1.0 - sharpen) * 2.0;
    FsrRcasCon(fsrConst0, stops);

    AF3 colour;
    FsrRcasF(
            colour.r,
            colour.g,
            colour.b,
            outputPixel,
            fsrConst0
    );

    fragColor = vec4(colour, 1.0);
}
