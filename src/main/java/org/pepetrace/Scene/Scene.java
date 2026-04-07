package org.pepetrace.Scene;

import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BARRIER_BIT;

import java.sql.Array;
import java.util.ArrayList;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.pepetrace.Buffers.SSBO;

public class Scene {

    private final ArrayList<Float> vertices = new ArrayList<Float>();
    private final ArrayList<Float> normals = new ArrayList<Float>();
    private final ArrayList<Float> UVCoordinates = new ArrayList<Float>();

    public int getTriangleAmount() {
        return triangleAmount;
    }

    private int triangleAmount = 0;

    public Scene() {

        for (float v : TestTriangleScene.vertices) {
            vertices.add(v);
        }
        for (float n : TestTriangleScene.normals) {
            normals.add(n);
        }
        for (float uv : TestTriangleScene.uv) {
            UVCoordinates.add(uv);
        }
        triangleAmount = TestTriangleScene.vertices.length;
    }

    private static float[] arrayListToArray(ArrayList<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    public void packTriangles(SSBO triangleBuffer) {
        triangleBuffer.fillBuffer(arrayListToArray(vertices));
    }

    public void packNormals(SSBO normalBuffer) {
        normalBuffer.fillBuffer(arrayListToArray(normals));
    }

    public void packUVs(SSBO UVBuffer) {
        UVBuffer.fillBuffer(arrayListToArray(UVCoordinates));
    }

    public void packScene(
        SSBO triangleBuffer,
        SSBO normalBuffer,
        SSBO UVBuffer
    ) {
        packTriangles(triangleBuffer);
        packUVs(UVBuffer);
        packNormals(normalBuffer);
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
    }
}
