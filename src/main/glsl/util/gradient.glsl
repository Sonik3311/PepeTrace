// RGB to HSV conversion
vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

// HSV to RGB conversion
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}
// Helper for hue interpolation
float interpolateHue(float h1, float h2, float t) {
    h1 = mod(h1, 1.0);
    h2 = mod(h2, 1.0);

    float diff = h2 - h1;
    if (diff > 0.5) diff -= 1.0;
    if (diff < -0.5) diff += 1.0;

    return mod(h1 + diff * t, 1.0);
}

vec3 hotMap5(float t) {
    // Define the 5 colors in RGB (easier to visualize)
    vec3 colorsRGB[5] = vec3[](
            vec3(0.0, 0.0, 1.0), // Blue at 0 % coverage
            vec3(1.0, 1.0, 0.0), // yellow at 35% coverage
            vec3(1.0, 0.0, 0.0), // Red at 50% coverage
            vec3(0.75, 1.0, 0.0), // Lime at 80% coverage
            vec3(0.0, 0.0, 1.0) //  Green at 100% coverage
        );

    // Convert to HSV once
    vec3 colorsHSV[5];
    for (int i = 0; i < 5; i++) {
        colorsHSV[i] = rgb2hsv(colorsRGB[i]);
    }

    // Define positions
    float positions[6] = float[](0.0, 0.15, 0.5, 0.85, 1.0, 1.1);

    t = clamp(t, 0.0, 1.0);

    // Find segment
    for (int i = 0; i < 4; i++) {
        if (t >= positions[i] && t <= positions[i + 1]) {
            float segmentT = (t - positions[i]) / (positions[i + 1] - positions[i]);

            vec3 hsv1 = colorsHSV[i];
            vec3 hsv2 = colorsHSV[i + 1];

            //// Special handling for black (hue undefined, use destination hue)
            //if (i == 0) { // Black to Blue segment
            //    return hsv2rgb(vec3(
            //            hsv2.x, // Use blue's hue directly
            //            mix(0.0, hsv2.y, segmentT), // Saturate from 0
            //            mix(0.0, hsv2.z, segmentT) // Brighten from 0
            //        ));
            //}

            // Special handling for white (hue undefined, use source hue)
            //if (i == 3) { // Red to White segment
            //    return hsv2rgb(vec3(
            //            hsv1.x, // Keep red's hue
            //            mix(hsv1.y, 0.0, segmentT), // Desaturate to white
            //            mix(hsv1.z, 1.0, segmentT) // Brighten to 1.0
            //        ));
            //}

            // Normal segments (Blue->Yellow, Yellow->Red)
            vec3 resultHSV = vec3(
                    interpolateHue(hsv1.x, hsv2.x, segmentT),
                    mix(hsv1.y, hsv2.y, segmentT),
                    mix(hsv1.z, hsv2.z, segmentT)
                );

            return hsv2rgb(resultHSV);
        }
    }

    return colorsRGB[4]; // White fallback
}