// Фрагментный шейдер
#version 460
in vec2 v_uv;
out vec4 FragColor;

#include "./util/pastel.glsl"

uniform sampler2D u_tex;

void main() {
    vec3 color = texture(u_tex, v_uv).rgb;
    //if (id == 0){
    //    FragColor = vec4(vec3(0), 1);
    //    return;
    //}
    FragColor = vec4(color, 1);
}