#include "../../constants.glsl"
// ----------------------------------------------------------------------------
// Returns a unit direction on the hemisphere aligned with 'axis'.
//   axis      : the central direction (hemisphere "top") – will be normalized
//   idx       : ray index (0 .. totalRays-1)
//   totalRays : total number of rays to generate
// ----------------------------------------------------------------------------
vec3 getRayDirection(vec3 axis, int idx, int totalRays) {
    // First ray = exactly the axis (the top)
    if (idx == 0) return normalize(axis);

    // ----- Build an orthonormal basis (T, B, axis) -----
    // Choose a vector not parallel to 'axis' for cross product
    vec3 up = abs(axis.y) < 0.999 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
    vec3 T = normalize(cross(up, axis));
    vec3 B = cross(axis, T); // already normalized because T and axis are orthonormal

    // ----- Deterministic pseudo‑random numbers (not equally spaced) -----
    // These hashes produce different values for each idx without needing a uniform distribution.
    float seed1 = fract(sin(float(idx) * 12.9898) * 43758.5453);
    float seed2 = fract(cos(float(idx) * 78.233) * 43758.5453);

    // Uniform distribution over the hemisphere's solid angle.
    // phi   : azimuthal angle in [0, 2π]
    // theta : polar angle measured from the axis (0 = axis direction)
    float phi = 2.0 * 3.14159265359 * seed1;
    float theta = acos(seed2); // because pdf ~ sin(theta) → uniform on hemisphere

    // Local direction in the (T, B, axis) coordinate system
    vec3 localDir = vec3(sin(theta) * cos(phi),
            sin(theta) * sin(phi),
            cos(theta));

    // Transform from tangent space to world space
    return normalize(T * localDir.x + B * localDir.y + axis * localDir.z);
}
