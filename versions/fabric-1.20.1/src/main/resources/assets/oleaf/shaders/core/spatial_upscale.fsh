#version 150

uniform sampler2D Sampler0;
uniform float InWidth;
uniform float InHeight;
uniform float Sharpen;
uniform float Algo;

in vec2 texCoord;
out vec4 fragColor;

vec4 cubic(float v) {
    vec4 n = vec4(1.0, 2.0, 3.0, 4.0) - v;
    vec4 s = n * n * n;
    float x = s.x;
    float y = s.y - 4.0 * s.x;
    float z = s.z - 4.0 * s.y + 6.0 * s.x;
    float w = 6.0 - x - y - z;
    return vec4(x, y, z, w) * (1.0 / 6.0);
}

vec3 bicubicSample(sampler2D tex, vec2 uv, vec2 texSize) {
    vec2 invSize = 1.0 / texSize;
    vec2 pixel = uv * texSize - 0.5;
    vec2 f = fract(pixel);
    pixel -= f;

    vec4 xCubic = cubic(f.x);
    vec4 yCubic = cubic(f.y);

    vec4 c = pixel.xxyy + vec2(-0.5, 1.5).xyxy;
    vec4 s = vec4(xCubic.xz + xCubic.yw, yCubic.xz + yCubic.yw);
    vec4 offset = c + vec4(xCubic.yw, yCubic.yw) / s;

    vec2 t0 = offset.xz * invSize;
    vec2 t1 = offset.yw * invSize;

    float sx = s.x / (s.x + s.y);
    float sy = s.z / (s.z + s.w);

    vec3 sample0 = texture(tex, vec2(t0.x, t0.y)).rgb;
    vec3 sample1 = texture(tex, vec2(t1.x, t0.y)).rgb;
    vec3 sample2 = texture(tex, vec2(t0.x, t1.y)).rgb;
    vec3 sample3 = texture(tex, vec2(t1.x, t1.y)).rgb;

    return mix(mix(sample3, sample2, sx), mix(sample1, sample0, sx), sy);
}

vec3 contrastAdaptiveSharpen(sampler2D tex, vec2 uv, vec2 invSize, vec3 center, float sharpen) {
    vec3 up = texture(tex, uv + vec2(0.0, -invSize.y)).rgb;
    vec3 down = texture(tex, uv + vec2(0.0, invSize.y)).rgb;
    vec3 left = texture(tex, uv + vec2(-invSize.x, 0.0)).rgb;
    vec3 right = texture(tex, uv + vec2(invSize.x, 0.0)).rgb;

    vec3 mn = min(center, min(min(up, down), min(left, right)));
    vec3 mx = max(center, max(max(up, down), max(left, right)));

    vec3 amp = clamp(min(mn, 1.0 - mx) / max(mx, 1.0e-4), 0.0, 1.0);
    amp = sqrt(amp);

    vec3 w = amp * (-0.2 * sharpen);
    return clamp((center + (up + down + left + right) * w) / (1.0 + 4.0 * w), 0.0, 1.0);
}

void main() {
    vec2 inputSize = max(vec2(InWidth, InHeight), vec2(textureSize(Sampler0, 0)));
    vec2 invSize = 1.0 / inputSize;
    int algo = int(Algo + 0.5);
    float sharpen = clamp(Sharpen, 0.0, 1.0);

    vec3 color;
    if (algo == 4) {
        vec2 px = (floor(texCoord * inputSize) + 0.5) / inputSize;
        color = texture(Sampler0, px).rgb;
    } else if (algo == 3) {
        color = texture(Sampler0, texCoord).rgb;
    } else {
        color = bicubicSample(Sampler0, texCoord, inputSize);
    }

    if (sharpen > 0.001) {
        color = contrastAdaptiveSharpen(Sampler0, texCoord, invSize, color, sharpen);
    }

    fragColor = vec4(color, 1.0);
}
