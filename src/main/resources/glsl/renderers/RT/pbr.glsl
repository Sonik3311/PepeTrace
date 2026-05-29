#include "../../constants.glsl"

vec3 fresnelSchlick(vec3 F0, float cosTheta) {
    return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}

float ggxNDF(vec3 N, vec3 H, float alpha) {
    float a2 = alpha * alpha;
    float NdotH = max(dot(N, H), 0.0);
    float d = NdotH * NdotH * (a2 - 1.0) + 1.0;
    return a2 / (PI * d * d);
}

float smithGGX_G1(vec3 N, vec3 V, float alpha) {
    float a2 = alpha * alpha;
    float NdotV = max(dot(N, V), 1e-5);
    float cos2 = NdotV * NdotV;
    float tan2 = (1.0 - cos2) / cos2;
    return 2.0 / (1.0 + sqrt(1.0 + a2 * tan2));
}

float smithGGX(vec3 N, vec3 V, vec3 L, float alpha) {
    return smithGGX_G1(N, V, alpha) * smithGGX_G1(N, L, alpha);
}

vec3 sampleGGXVNDF(vec3 V, vec3 N, float alpha, vec2 rnd) {
    vec3 T1 = abs(N.y) < 0.999 ? normalize(cross(vec3(0, 1, 0), N)) : normalize(cross(vec3(1, 0, 0), N));
    vec3 T2 = cross(N, T1);

    vec3 V_local = normalize(vec3(dot(V, T1), dot(V, T2), dot(V, N)));

    vec3 V_stretched = normalize(vec3(V_local.x * alpha, V_local.y * alpha, V_local.z));

    float len2 = V_stretched.x * V_stretched.x + V_stretched.y * V_stretched.y;
    vec3 T1_stretched = len2 > 0.0 ? vec3(-V_stretched.y, V_stretched.x, 0.0) / sqrt(len2) : vec3(1.0, 0.0, 0.0);
    vec3 T2_stretched = cross(V_stretched, T1_stretched);

    float r = sqrt(rnd.x);
    float phi = TWO_PI * rnd.y;
    float t1 = r * cos(phi);
    float t2_uncorrected = r * sin(phi);
    float s = 0.5 * (1.0 + V_stretched.z);
    float t2 = (1.0 - s) * sqrt(max(0.0, 1.0 - t1 * t1)) + s * t2_uncorrected;

    float hz = sqrt(max(0.0, 1.0 - t1 * t1 - t2 * t2));
    vec3 H_stretched = t1 * T1_stretched + t2 * T2_stretched + hz * V_stretched;

    vec3 H = normalize(vec3(H_stretched.x / alpha, H_stretched.y / alpha, max(H_stretched.z, 0.0)));

    vec3 L_local = reflect(-V_local, H);

    return L_local.x * T1 + L_local.y * T2 + L_local.z * N;
}

vec3 sampleCosineHemisphere(vec3 N, vec2 rnd) {
    vec3 T1 = abs(N.y) < 0.999 ? normalize(cross(vec3(0, 1, 0), N)) : normalize(cross(vec3(1, 0, 0), N));
    vec3 T2 = cross(N, T1);
    float r = sqrt(rnd.x);
    float theta = TWO_PI * rnd.y;
    float x = r * cos(theta);
    float y = r * sin(theta);
    float z = sqrt(max(0.0, 1.0 - rnd.x));
    return x * T1 + y * T2 + z * N;
}
