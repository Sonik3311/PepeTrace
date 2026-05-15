struct VertexData { // 16 * 5
    vec4 position;
    vec4 normal;
    vec4 uv;
    vec4 tangent;
    vec4 bitangent;
};

struct ModelMatricies {
    mat4 forwardM;
    mat4 inverseM;
};
