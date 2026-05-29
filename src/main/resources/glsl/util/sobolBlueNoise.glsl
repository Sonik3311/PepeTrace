uint pcgHash(uint v) {
    uint state = v * 747796405u + 2891336453u;
    uint word = ((state >> ((state >> 28u) + 4u)) ^ state) * 277803737u;
    return (word >> 22u) ^ word;
}

uint owenScramble(uint x, uint seed) {
    x ^= x >> 16u;
    x ^= x >> 8u;
    x *= 0x9e3779b9u;
    x ^= x >> 16u;
    x ^= x >> 8u;
    x *= seed;
    x ^= x >> 16u;
    return x;
}

uint sobol1D(uint index) {
    const uint sobolDir[32] = uint[](
    0x80000000u, 0x40000000u, 0x20000000u, 0x10000000u,
    0x08000000u, 0x04000000u, 0x02000000u, 0x01000000u,
    0x00800000u, 0x00400000u, 0x00200000u, 0x00100000u,
    0x00080000u, 0x00040000u, 0x00020000u, 0x00010000u,
    0x00008000u, 0x00004000u, 0x00002000u, 0x00001000u,
    0x00000800u, 0x00000400u, 0x00000200u, 0x00000100u,
    0x00000080u, 0x00000040u, 0x00000020u, 0x00000010u,
    0x00000008u, 0x00000004u, 0x00000002u, 0x00000001u
    );
    uint result = 0u;
    uint i = index;
    for (uint bit = 0u; bit < 32u && i != 0u; bit++) {
        if ((i & 1u) != 0u) {
            result ^= sobolDir[bit];
        }
        i >>= 1u;
    }
    return result;
}

// Blue‑noise random function that mutates the state.
// rngState.xy should be initialized with pixel coordinates (or a per‑pixel seed).
// rngState.z will be incremented on each call.
float randomBlue(inout uvec3 rngState) {
    // Use the current sample index
    uint sampleIdx = rngState.z;
    // Increment for next call
    rngState.z = pcgHash(rngState.z);

    // Compute a per‑pixel seed from the fixed coordinates
    uint pixelSeed = pcgHash(rngState.x * 1973u + rngState.y * 9277u);
    // Combine with sample index (hash to avoid correlation)
    uint combined = pcgHash(pixelSeed ^ sampleIdx);

    // Generate Sobol number for this sample index
    uint sobolVal = sobol1D(sampleIdx);
    // Owen scramble with the combined seed to decorrelate pixels
    uint scrambled = owenScramble(sobolVal, combined);

    return float(scrambled) / 4294967295.0;
}