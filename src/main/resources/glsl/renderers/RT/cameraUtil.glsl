vec3 getRayDirection(vec2 uv, vec2 yawPitch) {
    float yawRad = radians(yawPitch.x);
    float pitchRad = radians(yawPitch.y);

    vec3 ray_dir = normalize(vec3(
                cos(pitchRad) * sin(yawRad),
                sin(pitchRad),
                cos(pitchRad) * cos(yawRad)
            ));

    vec3 right = normalize(cross(ray_dir, vec3(0.0, 1.0, 0.0)));
    vec3 up = cross(right, ray_dir);
    ray_dir = normalize(ray_dir + right * uv.x + up * uv.y);

    return ray_dir;
}
