package org.pepetrace.Scene;

import java.util.ArrayList;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.pepetrace.Buffers.SSBO;

public class Scene {
    final private ArrayList<Float> vertices = new ArrayList<>();
    final private ArrayList<Float> UVCoordinates = new ArrayList<>();

    public int getTriangleAmount() {
        return triangleAmount;
    }

    private int triangleAmount = 0;

    public Scene() {
        for (float v : TestTriangleScene.vertices) {
            vertices.add(v);
        };
        triangleAmount = TestTriangleScene.vertices.length;
    }

    static private float[] arrayListToArray(ArrayList<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    public void packTriangles(SSBO triangleBuffer) {
        triangleBuffer.fillBuffer(arrayListToArray(vertices));
    }
}
