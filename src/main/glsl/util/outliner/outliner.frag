#version 460 core

#include "../pastel.glsl"

uniform sampler2D u_colorTexture;
uniform usampler2D u_stencilTexture;
uniform int u_selectedID;

in vec2 v_uv;
out vec4 FragColor;

const vec3 outlineColor = vec3(1, 0.5, 0);
const int outlineThickness = 2;

void main() {
    uint myID = texture(u_stencilTexture, v_uv).r;

    if (myID - 1 == u_selectedID || u_selectedID == -1) {
        // Внутри выбранного объекта – отображаем обычный цвет (не обводку)
        FragColor = texture(u_colorTexture, v_uv);
        return;
    }

    // Проверка соседей (8-связность)
    ivec2 texSize = textureSize(u_stencilTexture, 0);
    vec2 pixelStep = 1.0 / vec2(texSize);

    bool edge = false;
    for (int dx = -outlineThickness; dx <= outlineThickness && !edge; dx++) {
        for (int dy = -outlineThickness; dy <= outlineThickness && !edge; dy++) {
            if (dx == 0 && dy == 0) continue;
            vec2 offset = vec2(dx, dy) * pixelStep;
            uint neighborID = texture(u_stencilTexture, v_uv + offset).r;
            if (neighborID - 1 == u_selectedID) {
                edge = true;
                break;
            }
        }
    }

    if (edge) {
        // Рисуем обводку (например, красный цвет)
        FragColor = vec4(outlineColor, 1.0);
    } else {
        // Обычный цвет сцены
        FragColor = texture(u_colorTexture, v_uv);
    }
}