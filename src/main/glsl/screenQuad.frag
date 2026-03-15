// Фрагментный шейдер
#version 460
in vec2 v_uv;
out vec4 FragColor;

uniform sampler2D tex;
layout(std140, binding = 2) uniform TestUBO {
    float tata;
};


void main() {
    vec3 texCol = texture(tex, v_uv).rgb;
    FragColor = vec4(texCol, 1.0);
}