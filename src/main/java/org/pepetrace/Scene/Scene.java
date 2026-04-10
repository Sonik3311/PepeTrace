package org.pepetrace.Scene;

import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BARRIER_BIT;

import java.util.ArrayList;
import java.util.Arrays;

import org.pepetrace.Buffers.SSBO;
import org.pepetrace.Scene.Material.TextureMaterial;

public class Scene {

    private final ArrayList<Integer> indices = new ArrayList<Integer>();
    private final ArrayList<Float> vertices = new ArrayList<Float>();
    private final ArrayList<Float> normals = new ArrayList<Float>();
    private final ArrayList<Float> uv = new ArrayList<Float>();

    private final ArrayList<TextureMaterial> materials = new ArrayList<TextureMaterial>();

    public int getTriangleAmount() {
        return triangleAmount;
    }

    private int triangleAmount = 0;

    public Scene() {
        for (int i : TestTriangleScene.indices) {
            indices.add(i);
        }
        for (float v : TestTriangleScene.vertices) {
            vertices.add(v);
        }
        for (float n : TestTriangleScene.normals) {
            normals.add(n);
        }
        for (float uvCoord : TestTriangleScene.uvs) {
            uv.add(uvCoord);
        }

        materials.add(TextureMaterial.create("./src/main/java/org/pepetrace/unsplash-purple.jpg","./src/main/java/org/pepetrace/sunny_rose_garden_2k.hdr","./src/main/java/org/pepetrace/sunny_rose_garden_2k.hdr"));
        materials.add(TextureMaterial.create("./src/main/java/org/pepetrace/sunny_rose_garden_2k.hdr","./src/main/java/org/pepetrace/sunny_rose_garden_2k.hdr","./src/main/java/org/pepetrace/sunny_rose_garden_2k.hdr"));
        triangleAmount = TestTriangleScene.indices.length;
    }

    private static float[] FloatarrayListToArray(ArrayList<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    private static int[] IntegerarrayListToArray(ArrayList<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    public void packIndices(SSBO indicesBuffer) { indicesBuffer.fillBuffer(IntegerarrayListToArray(indices)); }

    public void packVertices(SSBO vertexBuffer) {
        vertexBuffer.fillBuffer(FloatarrayListToArray(vertices));
    }

    public void packNormals(SSBO normalBuffer) {
        normalBuffer.fillBuffer(FloatarrayListToArray(normals));
    }

    public void packUVs(SSBO UVBuffer) {
        UVBuffer.fillBuffer(FloatarrayListToArray(uv));
    }

    public void packMaterials(SSBO TextureMaterialBuffer) {
        long[] handles = new long[materials.size() * 3];
        int i = 0;
        for (TextureMaterial mat : materials) {
            for (long handle : mat.getTextureHandles()) {
                handles[i] = handle;
                i++;
            }
        }
        TextureMaterialBuffer.fillBuffer(handles);
    }

    public void packScene(SSBO geometryBuffer, SSBO materialIndicesBuffer, SSBO materialHandlesBuffer) {
        float[] geometryData = new float[indices.size() * 8];
        for (int i = 0; i < indices.size(); i++) {
            int index = indices.get(i);
            float vertexX = vertices.get(index * 3 + 0);
            float vertexY = vertices.get(index * 3 + 1);
            float vertexZ = vertices.get(index * 3 + 2);
            float normalX = normals.get(index * 3 + 0);
            float normalY = normals.get(index * 3 + 1);
            float normalZ = normals.get(index * 3 + 2);
            float uvX = uv.get(index * 2 + 0);
            float uvY = uv.get(index * 2 + 1);

            geometryData[i * 8 + 0] = vertexX;
            geometryData[i * 8 + 1] = vertexY;
            geometryData[i * 8 + 2] = vertexZ;
            geometryData[i * 8 + 3] = uvX;
            geometryData[i * 8 + 4] = normalX;
            geometryData[i * 8 + 5] = normalY;
            geometryData[i * 8 + 6] = normalZ;
            geometryData[i * 8 + 7] = uvY;
        }
        geometryBuffer.fillBuffer(geometryData);
        int[] materialIndicesData = new int[indices.size() / 3];
        for (int i = 0; i < indices.size() /3; i++) {
            materialIndicesData[i] = i > (indices.size() /3 - 4) ? 1: 0;
        }
        materialIndicesBuffer.fillBuffer(materialIndicesData);
        packMaterials(materialHandlesBuffer);
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
    }
}
