#version 400

// Snapdragon Game Super Resolution 1 (SGSR1) — spatial edge-aware upscale.
// Adapted from Qualcomm SGSR1 (BSD-3-Clause) for Minecraft Blaze3D screenquad.

#extension GL_ARB_gpu_shader5 : enable
#extension GL_ARB_texture_gather : enable

uniform sampler2D InSampler;

layout(std140) uniform UpscaleParams {
    // x = sharpen 0..1 (maps to EdgeSharpness), y = inW, z = inH
    vec4 Data;
};

in vec2 texCoord;
out vec4 fragColor;

#define EdgeThreshold (8.0 / 255.0)

float fastLanczos2(float x) {
    float wA = x - 4.0;
    float wB = x * wA - wA;
    wA *= wA;
    return wB * wA;
}

vec2 weightY(float dx, float dy, float c, float stdDev) {
    float x = ((dx * dx) + (dy * dy)) * 0.55 + clamp(abs(c) * stdDev, 0.0, 1.0);
    float w = fastLanczos2(x);
    return vec2(w, w * c);
}

void main() {
    vec2 inputSize = max(vec2(Data.y, Data.z), vec2(textureSize(InSampler, 0)));
    vec2 invSize = 1.0 / inputSize;
    float edgeSharpness = mix(1.0, 4.0, clamp(Data.x, 0.0, 1.0));

    vec3 color = textureLod(InSampler, texCoord, 0.0).rgb;
    float luma = color.g;

    vec2 imgCoord = (texCoord * inputSize) + vec2(-0.5, 0.5);
    vec2 imgCoordPixel = floor(imgCoord);
    vec2 coord = imgCoordPixel * invSize;
    vec2 pl = imgCoord - imgCoordPixel;

    vec4 left = textureGather(InSampler, coord, 1);

    float edgeVote = abs(left.z - left.y) + abs(luma - left.y) + abs(luma - left.z);
    if (edgeVote > EdgeThreshold) {
        coord.x += invSize.x;

        vec4 right = textureGather(InSampler, coord + vec2(invSize.x, 0.0), 1);
        vec4 upDown;
        upDown.xy = textureGather(InSampler, coord + vec2(0.0, -invSize.y), 1).wz;
        upDown.zw = textureGather(InSampler, coord + vec2(0.0, invSize.y), 1).yx;

        float mean = (left.y + left.z + right.x + right.w) * 0.25;
        left -= vec4(mean);
        right -= vec4(mean);
        upDown -= vec4(mean);
        float colorW = luma - mean;

        float sum = abs(left.x) + abs(left.y) + abs(left.z) + abs(left.w)
                + abs(right.x) + abs(right.y) + abs(right.z) + abs(right.w)
                + abs(upDown.x) + abs(upDown.y) + abs(upDown.z) + abs(upDown.w);
        float sumMean = 1.014185e+01 / max(sum, 1.0e-4);
        float stdDev = sumMean * sumMean;

        vec2 aWY = weightY(pl.x, pl.y + 1.0, upDown.x, stdDev);
        aWY += weightY(pl.x - 1.0, pl.y + 1.0, upDown.y, stdDev);
        aWY += weightY(pl.x - 1.0, pl.y - 2.0, upDown.z, stdDev);
        aWY += weightY(pl.x, pl.y - 2.0, upDown.w, stdDev);
        aWY += weightY(pl.x + 1.0, pl.y - 1.0, left.x, stdDev);
        aWY += weightY(pl.x, pl.y - 1.0, left.y, stdDev);
        aWY += weightY(pl.x, pl.y, left.z, stdDev);
        aWY += weightY(pl.x + 1.0, pl.y, left.w, stdDev);
        aWY += weightY(pl.x - 1.0, pl.y - 1.0, right.x, stdDev);
        aWY += weightY(pl.x - 2.0, pl.y - 1.0, right.y, stdDev);
        aWY += weightY(pl.x - 2.0, pl.y, right.z, stdDev);
        aWY += weightY(pl.x - 1.0, pl.y, right.w, stdDev);

        float finalY = aWY.y / max(aWY.x, 1.0e-4);
        float maxY = max(max(left.y, left.z), max(right.x, right.w));
        float minY = min(min(left.y, left.z), min(right.x, right.w));
        float deltaY = clamp(edgeSharpness * finalY, minY, maxY) - colorW;
        deltaY = clamp(deltaY, -23.0 / 255.0, 23.0 / 255.0);

        color = clamp(color + vec3(deltaY), 0.0, 1.0);
    }

    fragColor = vec4(color, 1.0);
}
