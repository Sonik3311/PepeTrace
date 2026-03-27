vec3 hotnessMap(float t) {
    t = clamp(t, 0.0, 1.0);

    // 6 equal segments: violet, blue, cyan, green, yellow, red, white
    // Actually 7 colors = 6 segments
    float segment = 1.0 / 6.0; // 0.1666...

    vec3 color;

    if (t < segment * 1.0) {
        // Violet to Blue
        float p = t / segment;
        color = vec3(0.67 * (1.0 - p), 0.0, 1.0);
    }
    else if (t < segment * 2.0) {
        // Blue to Cyan
        float p = (t - segment) / segment;
        color = vec3(0.0, p, 1.0);
    }
    else if (t < segment * 3.0) {
        // Cyan to Green
        float p = (t - segment * 2.0) / segment;
        color = vec3(0.0, 1.0, 1.0 - p);
    }
    else if (t < segment * 4.0) {
        // Green to Yellow
        float p = (t - segment * 3.0) / segment;
        color = vec3(p, 1.0, 0.0);
    }
    else if (t < segment * 5.0) {
        // Yellow to Red
        float p = (t - segment * 4.0) / segment;
        color = vec3(1.0, 1.0 - p, 0.0);
    }
    else {
        // Red to White
        float p = (t - segment * 5.0) / segment;
        color = vec3(1.0, p, p);
    }

    return color;
}