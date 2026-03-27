#include "../constants.glsl"

struct HitResult {
    bool isValid;
    vec3 position;
    float distance;
    vec3 normal;
};

HitResult rayTriangleIntersect(vec3 ro, vec3 rd, vec3 v0, vec3 v1, vec3 v2) {
    vec3 v0v1 = v1 - v0;
    vec3 v0v2 = v2 - v0;
    vec3 pvec = cross(rd, v0v2);
    float det = dot(v0v1, pvec);

    HitResult hit_result;
    hit_result.isValid = false;
    hit_result.position = vec3(0);
    hit_result.normal = vec3(0);
    hit_result.distance = -1;

    // Обрабатываем случай, когда луч параллелен треугольнику
    if (abs(det) < EPSILON) return hit_result;

    // Важно: сохраняем знак det для нормали
    float invDet = 1.0 / det;

    vec3 tvec = ro - v0;
    float u = dot(tvec, pvec) * invDet;
    if (u < 0.0 || u > 1.0) return hit_result;

    vec3 qvec = cross(tvec, v0v1);
    float v = dot(rd, qvec) * invDet;
    if (v < 0.0 || u + v > 1.0) return hit_result;

    float dist = dot(v0v2, qvec) * invDet;

    // Нормаль всегда должна быть направлена против луча (к камере)
    vec3 normal = normalize(cross(v0v1, v0v2));

    // Проверяем, с какой стороны треугольника пришел луч
    // Если det < 0, луч попадает в обратную сторону треугольника
    if (det < 0.0) {
        normal = -normal; // Переворачиваем нормаль
    }

    // Дополнительная проверка: луч не должен идти от треугольника
    if (dist < 0.0) return hit_result;

    hit_result.isValid = true;
    hit_result.position = ro + rd * dist;
    hit_result.distance = dist;
    hit_result.normal = normal;

    return hit_result;
}
