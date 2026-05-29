vec3 localToGlobal(vec3 vec, mat4 matrix) {
    return (matrix * vec4(vec, 1)).xyz;
}