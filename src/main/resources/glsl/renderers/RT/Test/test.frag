#version 460 core

in vec2 v_uv;
out vec4 FragColor;

void main() {
    FragColor = vec4(vec3(v_uv, 0), 1);
}