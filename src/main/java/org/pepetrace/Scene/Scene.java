package org.pepetrace.Scene;

import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BARRIER_BIT;

import java.util.ArrayList;
import java.util.Arrays;
import org.pepetrace.Buffers.SSBO;
import org.pepetrace.Scene.Loader.AssimpLoader;
import org.pepetrace.Scene.Loader.MeshData;
import org.pepetrace.Scene.Loader.MeshLoader;
import org.pepetrace.Scene.Material.TextureMaterial;

public class Scene {
    private final ArrayList<Float> vertices = new ArrayList<>();
    private final ArrayList<Float> normals = new ArrayList<>();
    private final ArrayList<Float> uvs = new ArrayList<>();
    private final ArrayList<Float> tangents = new ArrayList<>();
    private final ArrayList<Float> bitangents = new ArrayList<>();
    private final ArrayList<Integer> indices = new ArrayList<>();
    private final ArrayList<TextureMaterial> materials = new ArrayList<>();

    private int triangleCount = 0;
    private final MeshLoader loader = new AssimpLoader();

    public Scene() {
        loadModel("src/main/resources/models/dragon87k.obj", 0);
        materials.add(TextureMaterial.create(
                "./src/main/java/org/pepetrace/unsplash-purple.jpg",
                "./src/main/java/org/pepetrace/sunny_rose_garden_2k.hdr",
                "./src/main/java/org/pepetrace/sunny_rose_garden_2k.hdr"
        ));
    }

    public void loadModel(String path, int materialIndex) {
        MeshData data = loader.load(path);
        int baseIndex = vertices.size() / 3;
        vertices.addAll(data.getVertices());
        normals.addAll(data.getNormals());
        uvs.addAll(data.getUVs());
        tangents.addAll(data.getTangents());
        bitangents.addAll(data.getBitangents());
        for (int idx : data.getIndices()) {
            indices.add(baseIndex + idx);
        }
        triangleCount += data.getTriangleCount();
    }

    public int getTriangleCount() { return triangleCount; }

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

    public void packScene(SSBO geometryBuffer, SSBO indexBuffer, SSBO materialIndicesBuffer, SSBO materialHandlesBuffer) {
        int vertexCount = vertices.size() / 3;
        float[] geometryData = new float[vertexCount * 20];
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

        int[] indexData = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indexData[i] = indices.get(i);
        }
        indexBuffer.fillBuffer(indexData);

        int[] materialIndicesData = new int[triangleCount];
        Arrays.fill(materialIndicesData, 0);
        materialIndicesBuffer.fillBuffer(materialIndicesData);

        packMaterials(materialHandlesBuffer);
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
    }
}