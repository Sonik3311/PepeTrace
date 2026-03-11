struct HitResult {
    bool isValid;
    vec3 position;
    float distance;
    vec3 normal;
};

HitResult triIntersect(vec3 ro, vec3 rd, vec3 A, vec3 B, vec3 C) {
    vec3 edgeAB = B - A;
    vec3 edgeAC = C - A;
    vec3 h = cross(rd, edgeAC);
    float det = dot(edgeAB, h);

    HitResult hit_result;
    hit_result.isValid = false;
    hit_result.position = vec3(0);
    hit_result.normal = vec3(0);
    hit_result.distance = -1;

    if (det < EPSILON) return hit_result;
    float invDet = 1.0 / det;

    vec3 s = ro - A;
    float u = dot(s, h) * invDet;

    if (u < 0 || u > 1) return hit_result;

    vec3 q = cross(s, edgeAB);
    float v = dot(rd, q) * invDet;

    if (v < 0 || u + v > 1) return hit_result;

    // Calculate t (distance along ray)
    float t = dot(edgeAC, q) * invDet;

    vec3 normal = cross(edgeAB, edgeAC);
    float switch_factor = ceil(abs(max(0,dot(normal, rd))));

    hit_result.isValid = true;
    hit_result.position = ro + rd * t;
    hit_result.distance = t;
    hit_result.normal = mix(normal, -normal, switch_factor);




    return hit_result;
}