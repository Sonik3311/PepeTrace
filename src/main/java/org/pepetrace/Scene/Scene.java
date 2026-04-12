package org.pepetrace.Scene;

import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BARRIER_BIT;

import java.util.ArrayList;
import org.pepetrace.Buffers.SSBO;
import org.pepetrace.Scene.Loader.AssimpLoader;
import org.pepetrace.Scene.Loader.MeshData;
import org.pepetrace.Scene.Loader.MeshLoader;
import org.pepetrace.Scene.Material.TextureMaterial;

public class Scene {
    // Плоские списки атрибутов (без индексов)
    private final ArrayList<Float> vertices = new ArrayList<>();
    private final ArrayList<Float> normals = new ArrayList<>();
    private final ArrayList<Float> uvs = new ArrayList<>();
    private final ArrayList<Float> tangents = new ArrayList<>();
    private final ArrayList<Float> bitangents = new ArrayList<>();
    private final ArrayList<TextureMaterial> materials = new ArrayList<>();

    private int triangleAmount = 0;
    private final MeshLoader loader = new AssimpLoader();

    public Scene() {
        // Загружаем тестовую сцену из OBJ (замените путь на свой)
        loadModel("src/main/resources/models/cube.obj", 0);
        materials.add(TextureMaterial.create(
                "./src/main/java/org/pepetrace/unsplash-purple.jpg",
                "./src/main/java/org/pepetrace/sunny_rose_garden_2k.hdr",
                "./src/main/java/org/pepetrace/sunny_rose_garden_2k.hdr"
        ));
    }

    public void loadModel(String path, int materialIndex) {
        MeshData data = loader.load(path);
        // Добавляем вершины в общие списки
        vertices.addAll(data.getVertices());
        normals.addAll(data.getNormals());
        uvs.addAll(data.getUVs());
        tangents.addAll(data.getTangents());
        bitangents.addAll(data.getBitangents());
        triangleAmount += data.getVertexCount() / 3;
    }

    public int getTriangleAmount() { return triangleAmount; }

    public void packMaterials(SSBO textureMaterialBuffer) {
        long[] handles = new long[materials.size() * 3];
        int i = 0;
        for (TextureMaterial mat : materials) {
            for (long handle : mat.getTextureHandles()) {
                handles[i++] = handle;
            }
        }
        textureMaterialBuffer.fillBuffer(handles);
    }

    public void packScene(SSBO geometryBuffer, SSBO materialIndicesBuffer, SSBO materialHandlesBuffer) {
        float[] geometryData = new float[vertices.size() / 3 * 20]; // 5 векторов × 4 float = 20
        int vertexCount = vertices.size() / 3;
        for (int i = 0; i < vertexCount; i++) {
            int base = i * 20;
            // position (xyz, 1)
            geometryData[base + 0] = vertices.get(i * 3);
            geometryData[base + 1] = vertices.get(i * 3 + 1);
            geometryData[base + 2] = vertices.get(i * 3 + 2);
            geometryData[base + 3] = 1.0f;
            // normal (xyz, 0)
            geometryData[base + 4] = normals.get(i * 3);
            geometryData[base + 5] = normals.get(i * 3 + 1);
            geometryData[base + 6] = normals.get(i * 3 + 2);
            geometryData[base + 7] = 0.0f;
            // uv (u, v, 0, 0)
            geometryData[base + 8] = uvs.get(i * 2);
            geometryData[base + 9] = uvs.get(i * 2 + 1);
            geometryData[base + 10] = 0.0f;
            geometryData[base + 11] = 0.0f;
            // tangent (xyz, 0)
            geometryData[base + 12] = tangents.get(i * 3);
            geometryData[base + 13] = tangents.get(i * 3 + 1);
            geometryData[base + 14] = tangents.get(i * 3 + 2);
            geometryData[base + 15] = 0.0f;
            // bitangent (xyz, 0)
            geometryData[base + 16] = bitangents.get(i * 3);
            geometryData[base + 17] = bitangents.get(i * 3 + 1);
            geometryData[base + 18] = bitangents.get(i * 3 + 2);
            geometryData[base + 19] = 0.0f;
        }
        geometryBuffer.fillBuffer(geometryData);

        // materialIndices: один индекс на треугольник
        int[] materialIndicesData = new int[triangleAmount];
        for (int i = 0; i < triangleAmount; i++) {
            materialIndicesData[i] = 0; // пока все треугольники используют материал 0
        }
        materialIndicesBuffer.fillBuffer(materialIndicesData);

        packMaterials(materialHandlesBuffer);
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
    }
}