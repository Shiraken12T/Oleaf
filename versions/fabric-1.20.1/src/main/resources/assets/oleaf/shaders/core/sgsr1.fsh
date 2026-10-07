#version 150

// Lightweight SGSR-style spatial upscale for 1.20.1.

uniform sampler2D Sampler0;
uniform float InWidth;
uniform float InHeight;
uniform float Sharpen;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 inputSize = max(vec2(InWidth, InHeight), vec2(textureSize(Sampler0, 0)));
    vec2 inv = 1.0 / inputSize;

    vec3 c = texture(Sampler0, texCoord).rgb;
    vec3 n = texture(Sampler0, texCoord + vec2(0.0, -inv.y)).rgb;
    vec3 s = texture(Sampler0, texCoord + vec2(0.0, inv.y)).rgb;
    vec3 e = texture(Sampler0, texCoord + vec2(inv.x, 0.0)).rgb;
    vec3 w = texture(Sampler0, texCoord + vec2(-inv.x, 0.0)).rgb;

    vec3 blur = (n + s + e + w) * 0.25;
    float edge = clamp(length(c - blur) * 3.0, 0.0, 1.0);
    vec3 color = mix(blur, c, 0.65 + 0.35 * edge);

    float sharpen = clamp(Sharpen, 0.0, 1.0);
    color = clamp(color + (c - blur) * sharpen * 0.75, 0.0, 1.0);
    fragColor = vec4(color, 1.0);
}
