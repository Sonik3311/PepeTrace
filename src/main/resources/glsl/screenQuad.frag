// Фрагментный шейдер
#version 460
in vec2 v_uv;
out vec4 FragColor;

#include "./util/pastel.glsl"

uniform sampler2D u_tex;

// Narkowicz 2015, "ACES Filmic Tone Mapping Curve"
vec3 aces(vec3 x) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

float aces(float x) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main() {
    vec4 texel = texture(u_tex, v_uv);
    float count = texel.a;
    vec3 color = count > 0.0 ? texel.rgb / count : vec3(0.0);
    // Reinhard tone mapping + gamma
    //color = color / (1.0 + color);
    //color = pow(color, vec3(1.0 / 2.2));
    color = aces(color);
    FragColor = vec4(color, 1.0);
}