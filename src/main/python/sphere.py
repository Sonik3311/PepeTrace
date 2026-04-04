import math


def generate_sphere(radius=1.0, sectors=6, stacks=6):
    # Step 1: generate unique vertices, normals, and UVs
    vertices = []  # x, y, z
    normals = []  # nx, ny, nz
    uvs = []  # u, v

    for i in range(stacks + 1):
        theta = math.pi * i / stacks - math.pi / 2  # -pi/2 to pi/2
        sin_theta = math.sin(theta)
        cos_theta = math.cos(theta)

        v = 1.0 - i / stacks  # V: 1 at north pole, 0 at south pole

        for j in range(sectors + 1):
            phi = 2 * math.pi * j / sectors
            sin_phi = math.sin(phi)
            cos_phi = math.cos(phi)

            x = radius * cos_theta * cos_phi
            y = radius * sin_theta
            z = radius * cos_theta * sin_phi

            # Vertex position
            vertices.extend([x, y, z])

            # Normal (unit vector from center)
            normals.extend([x / radius, y / radius, z / radius])

            # UV coordinates: U = phi / 2pi, V = (theta + pi/2)/pi
            u = phi / (2 * math.pi)  # 0 to 1 around equator
            uvs.extend([u, v])

    # Step 2: build triangle indices
    indices = []
    for i in range(stacks):
        for j in range(sectors):
            first = i * (sectors + 1) + j
            second = first + sectors + 1
            # Triangle 1
            indices.extend([second, first, first + 1])
            # Triangle 2
            indices.extend([second + 1, second, first + 1])

    # Step 3: expand vertices, normals, UVs according to indices
    triangle_vertices = []
    triangle_normals = []
    triangle_uvs = []

    for idx in indices:
        # Position
        triangle_vertices.extend(vertices[idx * 3 : idx * 3 + 3])
        # Normal
        triangle_normals.extend(normals[idx * 3 : idx * 3 + 3])
        # UV
        triangle_uvs.extend(uvs[idx * 2 : idx * 2 + 2])

    return triangle_vertices, triangle_normals, triangle_uvs


# Generate sphere (low resolution for example)
vertices, normals, uvs = generate_sphere(1.0, 6, 6)

# Print vertex array
print("float[] sphereVertices = {")
for i, v in enumerate(vertices):
    end = "," if i < len(vertices) - 1 else ""
    print(f"    {v:.6f}f{end}")
print("};")

# Print normal array
print("\nfloat[] sphereNormals = {")
for i, n in enumerate(normals):
    end = "," if i < len(normals) - 1 else ""
    print(f"    {n:.6f}f{end}")
print("};")

# Print UV array
print("\nfloat[] sphereUVs = {")
for i, uv in enumerate(uvs):
    end = "," if i < len(uvs) - 1 else ""
    print(f"    {uv:.6f}f{end}")
print("};")
