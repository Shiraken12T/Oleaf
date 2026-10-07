#version 150

// AMD FidelityFX FSR1 — EASU-inspired spatial upsampling for 1.20.1 core shaders.
// Uses edge-aware Lanczos-ish weights (portable GLSL 150, no textureGather).

uniform sampler2D Sampler0;
uniform float InWidth;
uniform float InHeight;
uniform float OutWidth;
uniform float OutHeight;

in vec2 texCoord;
out vec4 fragColor;

vec3 sampleColor(vec2 uv) {
    return texture(Sampler0, clamp(uv, vec2(0.0), vec2(1.0))).rgb;
}

void main() {
    vec2 inputSize = max(vec2(InWidth, InHeight), vec2(1.0));
    vec2 outputSize = max(vec2(OutWidth, OutHeight), vec2(1.0));
    vec2 invInput = 1.0 / inputSize;

    // Map output pixel to input space.
    vec2 src = (gl_FragCoord.xy - 0.5) * (inputSize / outputSize) + 0.5;
    vec2 baseUv = src * invInput;
    vec2 f = fract(src);

    vec3 a = sampleColor(baseUv + vec2(-invInput.x, -invInput.y));
    vec3 b = sampleColor(baseUv + vec2(0.0, -invInput.y));
    vec3 c = sampleColor(baseUv + vec2(invInput.x, -invInput.y));
    vec3 d = sampleColor(baseUv + vec2(-invInput.x, 0.0));
    vec3 e = sampleColor(baseUv);
    vec3 fcol = sampleColor(baseUv + vec2(invInput.x, 0.0));
    vec3 g = sampleColor(baseUv + vec2(-invInput.x, invInput.y));
    vec3 h = sampleColor(baseUv + vec2(0.0, invInput.y));
    vec3 i = sampleColor(baseUv + vec2(invInput.x, invInput.y));

    // Edge strength from ring contrast (EASU-inspired directionality).
    float lumaE = dot(e, vec3(0.299, 0.587, 0.114));
    float lumaRing = (
        dot(b, vec3(0.299, 0.587, 0.114)) +
        dot(d, vec3(0.299, 0.587, 0.114)) +
        dot(fcol, vec3(0.299, 0.587, 0.114)) +
        dot(h, vec3(0.299, 0.587, 0.114))
    ) * 0.25;
    float edge = clamp(abs(lumaE - lumaRing) * 4.0, 0.0, 1.0);

    // Bicubic-ish weights with edge-aware sharpening of center tap.
    vec2 w1 = f * f * (3.0 - 2.0 * f);
    vec3 top = mix(mix(a, b, w1.x), mix(b, c, w1.x), 0.5);
    vec3 mid = mix(d, mix(e, fcol, w1.x), 0.5 + 0.5 * (1.0 - edge));
    mid = mix(d, fcol, w1.x) * (1.0 - 0.35 * edge) + e * (0.35 * edge + (1.0 - w1.x) * w1.x);
    // Stable bilinear base with edge boost toward center.
    vec3 bil = mix(mix(e, fcol, w1.x), mix(h, i, w1.x), w1.y);
    bil = mix(mix(d, e, w1.x), mix(g, h, w1.x), w1.y) * 0.35 + bil * 0.65;
    vec3 color = mix(bil, e, edge * 0.25);

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
