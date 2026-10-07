#version 150

// AMD FidelityFX FSR1 — RCAS-inspired contrast-adaptive sharpening (GLSL 150).

uniform sampler2D Sampler0;
uniform float Sharpen;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
    vec3 e = texture(Sampler0, texCoord).rgb;
    vec3 b = texture(Sampler0, texCoord + vec2(0.0, -texel.y)).rgb;
    vec3 d = texture(Sampler0, texCoord + vec2(-texel.x, 0.0)).rgb;
    vec3 f = texture(Sampler0, texCoord + vec2(texel.x, 0.0)).rgb;
    vec3 h = texture(Sampler0, texCoord + vec2(0.0, texel.y)).rgb;

    vec3 mn = min(e, min(min(b, d), min(f, h)));
    vec3 mx = max(e, max(max(b, d), max(f, h)));
    vec3 amp = clamp(min(mn, 1.0 - mx) / max(mx, 1.0e-4), 0.0, 1.0);
    amp = sqrt(amp);

    float sharpen = clamp(Sharpen, 0.0, 1.0);
    vec3 w = amp * (-0.2 * sharpen);
    vec3 color = (e + (b + d + f + h) * w) / (1.0 + 4.0 * w);
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
