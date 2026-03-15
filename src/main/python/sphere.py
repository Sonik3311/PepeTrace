import math

def generate_sphere(radius=1.0, sectors=36, stacks=18):
    vertices = []

    for i in range(stacks + 1):
        theta = math.pi * i / stacks - math.pi / 2
        sin_theta = math.sin(theta)
        cos_theta = math.cos(theta)

        for j in range(sectors + 1):
            phi = 2 * math.pi * j / sectors
            sin_phi = math.sin(phi)
            cos_phi = math.cos(phi)

            x = radius * cos_theta * cos_phi
            y = radius * sin_theta
            z = radius * cos_theta * sin_phi

            vertices.extend([x, y, z])

    indices = []
    for i in range(stacks):
        for j in range(sectors):
            first = i * (sectors + 1) + j
            second = first + sectors + 1
            indices.extend([second, first, first + 1])
            indices.extend([second + 1, second, first + 1])

    triangle_vertices = []
    for idx in indices:
        triangle_vertices.extend(vertices[idx*3:(idx*3 + 3)])

    return triangle_vertices

# Генерируем и выводим
vertices = generate_sphere(1.0, 6, 6)
print("float[] sphereVertices = {")
for i, v in enumerate(vertices):
    end = "," if i < len(vertices) - 1 else ""
    print(f"    {v:.6f}f{end}")
print("};")