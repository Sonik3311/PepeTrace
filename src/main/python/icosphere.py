import math

import math

def normalize(v):
    x, y, z = v
    length = math.sqrt(x*x + y*y + z*z)
    return (x/length, y/length, z/length) if length != 0 else (0,0,0)

def generate_sphere_shared(radius=1.0, subdivisions=2):
    """
    Generate sphere by subdividing an octahedron.
    Returns: vertices, normals, uvs, indices (all shared, no duplicates)
    """
    # Octahedron vertices (6)
    base_vertices = [
        (0, 1, 0), (0, -1, 0),
        (1, 0, 0), (-1, 0, 0),
        (0, 0, 1), (0, 0, -1)
    ]
    base_vertices = [normalize(v) for v in base_vertices]

    # Octahedron faces (8 triangles)
    faces = [
        (0, 2, 4), (0, 4, 3), (0, 3, 5), (0, 5, 2),
        (1, 4, 2), (1, 3, 4), (1, 5, 3), (1, 2, 5)
    ]

    vertices = []
    normals = []
    uvs = []
    indices = []
    cache = {}

    def add_vertex(nx, ny, nz):
        x, y, z = nx*radius, ny*radius, nz*radius
        u = math.atan2(ny, nx) / (2*math.pi) + 0.5
        v = math.asin(nz) / math.pi + 0.5
        idx = len(vertices)//3
        vertices.extend([x, y, z])
        normals.extend([nx, ny, nz])
        uvs.extend([u, v])
        return idx

    def get_midpoint(i1, i2):
        key = (i1, i2) if i1 < i2 else (i2, i1)
        if key in cache:
            return cache[key]
        n1 = normals[i1*3:i1*3+3]
        n2 = normals[i2*3:i2*3+3]
        mid = (n1[0]+n2[0], n1[1]+n2[1], n1[2]+n2[2])
        nx, ny, nz = normalize(mid)
        idx = add_vertex(nx, ny, nz)
        cache[key] = idx
        return idx

    # Add base vertices
    for nx, ny, nz in base_vertices:
        add_vertex(nx, ny, nz)

    # Subdivide
    current_faces = list(faces)
    for _ in range(subdivisions):
        new_faces = []
        for a,b,c in current_faces:
            ab = get_midpoint(a, b)
            bc = get_midpoint(b, c)
            ca = get_midpoint(c, a)
            new_faces.extend([
                (a, ab, ca),
                (b, bc, ab),
                (c, ca, bc),
                (ab, bc, ca)
            ])
        current_faces = new_faces
        cache.clear()

    # Build indices
    for a,b,c in current_faces:
        indices.extend([a, b, c])

    return vertices, normals, uvs, indices
# Generate sphere
radius = 1.0
subdivisions = 1
vertices, normals, uvs, indices = generate_sphere_shared(radius, subdivisions)

# Print arrays (same as before)
print("float[] vertices = {")
for i in range(0, len(vertices), 3):
    end = "," if i < len(vertices) - 3 else ""
    print(f"    {vertices[i]:.6f}f, {vertices[i+1]:.6f}f, {vertices[i+2]:.6f}f{end}")
print("};")

print("\nfloat[] normals = {")
for i in range(0, len(normals), 3):
    end = "," if i < len(normals) - 3 else ""
    print(f"    {normals[i]:.6f}f, {normals[i+1]:.6f}f, {normals[i+2]:.6f}f{end}")
print("};")

print("\nfloat[] uvs = {")
for i in range(0, len(uvs), 2):
    end = "," if i < len(uvs) - 2 else ""
    print(f"    {uvs[i]:.6f}f, {uvs[i+1]:.6f}f{end}")
print("};")

print("\nint[] indices = {")
for i in range(0, len(indices), 3):
    end = "," if i < len(indices) - 3 else ""
    print(f"    {indices[i]}, {indices[i+1]}, {indices[i+2]}{end}")
print("};")