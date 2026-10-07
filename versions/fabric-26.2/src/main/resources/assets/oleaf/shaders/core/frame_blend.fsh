#version 330

uniform sampler2D PrevSampler;
uniform sampler2D CurrSampler;

layout(std140) uniform BlendParams {
    vec4 Data; // x = mix factor (0 = prev frame, 1 = current frame)
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec3 prev = texture(PrevSampler, texCoord).rgb;
    vec3 curr = texture(CurrSampler, texCoord).rgb;
    float t = clamp(Data.x, 0.0, 1.0);
    fragColor = vec4(mix(prev, curr, t), 1.0);
}
