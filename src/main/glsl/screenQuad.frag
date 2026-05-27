// Фрагментный шейдер
#version 460
in vec2 v_uv;
out vec4 FragColor;

#include "./util/pastel.glsl"

uniform sampler2D u_tex;

void main() {
    vec4 texel = texture(u_tex, v_uv);
    float count = texel.a;
    vec3 color = count > 0.0 ? texel.rgb / count : vec3(0.0);
    FragColor = vec4(color, 1);
}