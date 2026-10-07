#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform float Mix;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec3 prev = texture(Sampler0, texCoord).rgb;
    vec3 curr = texture(Sampler1, texCoord).rgb;
    float t = clamp(Mix, 0.0, 1.0);
    fragColor = vec4(mix(prev, curr, t), 1.0);
}
