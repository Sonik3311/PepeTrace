struct HitResult {
    bool isValid;
    vec3 position;
    float distance;
    vec3 normal;
};

struct TriangleHitResult {
    bool isValid;
    vec3 position;
    vec3 normal; // interpolated vertex normal (smooth)
    vec3 tangent;
    vec3 bitangent;
    vec3 geometricNormal;
    float distance;
    vec2 uv; // interpolated texture coordinates
    // optional: barycentric coordinates (u, v) for custom interpolation
    float baryU;
    float baryV;

};

struct sceneIntersectionResult {
    TriangleHitResult hit;
    int materialIndex;
    int checkCount;
    int hitCount;
};

TriangleHitResult rayTriangleIntersect(
    vec3 ro, vec3 rd,
    vec3 v0, vec3 v1, vec3 v2, // positions
    vec3 n0, vec3 n1, vec3 n2, // vertex normals
    vec3 t0, vec3 t1, vec3 t2,
    vec3 b0, vec3 b1, vec3 b2,
    vec2 uv0, vec2 uv1, vec2 uv2 // texture coordinates
) {
    TriangleHitResult hit_result;
    hit_result.isValid = false;
    hit_result.position = vec3(0);
    hit_result.normal = vec3(0);
    hit_result.geometricNormal = vec3(0);
    hit_result.distance = -1.0;
    hit_result.uv = vec2(-1);
    hit_result.baryU = -1;
    hit_result.baryV = -1;
    hit_result.tangent = vec3(-1);
    hit_result.bitangent = vec3(-1);

    vec3 v0v1 = v1 - v0;
    vec3 v0v2 = v2 - v0;
    vec3 pvec = cross(rd, v0v2);
    float det = dot(v0v1, pvec);

    if (abs(det) < EPSILON) return hit_result;

    float invDet = 1.0 / det;

    vec3 tvec = ro - v0;
    float u = dot(tvec, pvec) * invDet;
    if (u < 0.0 || u > 1.0) return hit_result;

    vec3 qvec = cross(tvec, v0v1);
    float v = dot(rd, qvec) * invDet;
    if (v < 0.0 || u + v > 1.0) return hit_result;

    float dist = dot(v0v2, qvec) * invDet;
    if (dist < 0.0) return hit_result;

    // Barycentric coordinates for interpolation
    float w = 1.0 - u - v;
    hit_result.baryU = u;
    hit_result.baryV = v;

    hit_result.geometricNormal = normalize(cross(normalize(v0v1), normalize(v0v2)));
    if (det < 0.0) hit_result.geometricNormal = -hit_result.geometricNormal;

    // Interpolate vertex normals (smooth shading)
    vec3 smoothNormal = normalize(w * n0 + u * n1 + v * n2);
    vec3 smoothTangent = normalize(w * t0 + u * t1 + v * t2);
    vec3 smoothBitangent = normalize(w * b0 + u * b1 + v * b2);
    // If the ray hits the back face (det < 0), flip the normal
    // so it always points toward the ray origin.
    if (det < 0.0) {
        smoothNormal = -smoothNormal;
        smoothTangent = -smoothTangent;
        smoothBitangent = -smoothBitangent;
    }
    hit_result.normal = smoothNormal;
    hit_result.tangent = smoothTangent;
    hit_result.bitangent = smoothBitangent;

    // Interpolate texture coordinates
    hit_result.uv = w * uv0 + u * uv1 + v * uv2;

    hit_result.isValid = true;
    hit_result.position = ro + rd * dist;
    hit_result.distance = dist;

    return hit_result;
}
