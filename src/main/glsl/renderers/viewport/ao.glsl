#include "../../constants.glsl"

vec3 fibonacciHemispherePoint(uint i, uint N) {
    const float goldenRatio = 1.618033988749895;
    float theta = TWO_PI * float(i) / goldenRatio;
    float phi = acos(1.0 - (2.0 * float(i) + 1.0) / float(2.0 * float(N)));
    float sinPhi = sin(phi);
    return vec3(cos(theta) * sinPhi, sin(theta) * sinPhi, cos(phi));
}