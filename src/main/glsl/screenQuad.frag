// Фрагментный шейдер
#version 330 core
in vec2 v_uv;
out vec4 FragColor;

uniform sampler2D tex;

void main() {
    // v_uv будет интерполировано, но из-за того что треугольник 
    // выходит за пределы экрана, нам нужно скорректировать координаты
    vec2 uv = fract(v_uv); // Берем дробную часть для повторения

    vec3 texCol = texture(tex, uv).rgb;

    FragColor = vec4(texCol, 1.0);
}