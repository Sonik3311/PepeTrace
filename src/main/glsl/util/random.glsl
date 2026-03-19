const float PI = 3.14159265;
// A single iteration of Bob Jenkins' One-At-A-Time hashing algorithm.
uint hash( uint x ) {
    x += ( x << 10u );
    x ^= ( x >>  6u );
    x += ( x <<  3u );
    x ^= ( x >> 11u );
    x += ( x << 15u );
    return x;
}



// Compound versions of the hashing algorithm I whipped together.
uint hash( uvec2 v ) { return hash( v.x ^ hash(v.y)                         ); }
uint hash( uvec3 v ) { return hash( v.x ^ hash(v.y) ^ hash(v.z)             ); }
uint hash( uvec4 v ) { return hash( v.x ^ hash(v.y) ^ hash(v.z) ^ hash(v.w) ); }



// Construct a float with half-open range [0:1] using low 23 bits.
// All zeroes yields 0.0, all ones yields the next smallest representable value below 1.0.
float floatConstruct( uint m ) {
    const uint ieeeMantissa = 0x007FFFFFu; // binary32 mantissa bitmask
    const uint ieeeOne      = 0x3F800000u; // 1.0 in IEEE binary32

    m &= ieeeMantissa;                     // Keep only mantissa bits (fractional part)
    m |= ieeeOne;                          // Add fractional part to 1.0

    float  f = uintBitsToFloat( m );       // Range [1:2]
    return f - 1.0;                        // Range [0:1]
}



// Pseudo-random value in half-open range [0:1].
float random( inout uvec3 v ) {
    uint hash1 = hash(v);
    uint hash2 = hash(hash1);
    uint hash3 = hash(hash1 + hash2);
    v = uvec3(hash1, hash2, hash3);
    return floatConstruct(hash(floatBitsToUint(v)));
}

vec3 randomUnitVector(uvec3 x) {
    return normalize(vec3(random(x),random(x), random(x)));
}

vec3 randomHemisphereUnitVector(uvec3 x, vec3 N) {
    vec3 vector = randomUnitVector(x);
    return dot(vector, N) >= 0 ? vector : -vector;
}

vec3 RandomUnitVector(inout uvec3 state)
{
    float z = random(state) * 2.0f - 1.0f;
    float a = random(state) * PI*2;
    float r = sqrt(1.0f - z * z);
    float x = r * cos(a);
    float y = r * sin(a);
    return vec3(x, y, z);
}
vec3 RandomHemisphereUnitVector(inout uvec3 state, in vec3 n){
    vec3 v = RandomUnitVector(state);
    return normalize(dot(v,n) * v);
}