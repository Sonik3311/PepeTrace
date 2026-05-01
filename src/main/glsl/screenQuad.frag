// Фрагментный шейдер
#version 460
in vec2 v_uv;
out vec4 FragColor;

#include "./util/pastel.glsl"

uniform usampler2D tex;
layout(std140, binding = 2) uniform TestUBO {
    float tata;
};


void main() {
    uint id = texture(tex, v_uv).r;
    if (id == 0){
        FragColor = vec4(vec3(0), 1);
        return;
    }
    FragColor = vec4(getPastelColor(id-1), 1);
}