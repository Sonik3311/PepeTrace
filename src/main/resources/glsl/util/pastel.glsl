#include "random.glsl"

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

vec3 getPastelColor(uint index) {
    // Generate three independent pseudo-random values
    float r0 = float(hash(index))      / 4294967295.0;  // 2^32 - 1
    float r1 = float(hash(index + 1u)) / 4294967295.0;
    float r2 = float(hash(index + 2u)) / 4294967295.0;

    // Hue: full circle
    float h = r0;

    // Saturation: keep it low for a pastel look (0.1 to 0.5)
    float s = 0.1 + r1 * 0.4;

    // Value: stay bright (0.8 to 1.0), with slight variation
    float v = 0.8 + r2 * 0.2;

    return hsv2rgb(vec3(h, s, v));
}