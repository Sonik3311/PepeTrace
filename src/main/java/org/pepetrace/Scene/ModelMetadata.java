package org.pepetrace.Scene;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ModelMetadata {
    private String name;
    private Vector3f position;
    private Quaternionf rotation;
    private Vector3f scale;
    private int startTriangleIndex;
    private int triangleCount;

    public ModelMetadata(String name, int startTriangleIndex, int triangleCount) {
        this.name = name;
        this.startTriangleIndex = startTriangleIndex;
        this.triangleCount = triangleCount;
        this.position = new Vector3f(0, 0, 0);
        this.rotation = new Quaternionf();
        this.scale = new Vector3f(1, 1, 1);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Vector3f getPosition() { return position; }
    public void setPosition(Vector3f position) { this.position.set(position); }

    public Quaternionf getRotation() { return rotation; }
    public void setRotation(Quaternionf rotation) { this.rotation.set(rotation); }

    public Vector3f getScale() { return scale; }
    public void setScale(Vector3f scale) { this.scale.set(scale); }

    public int getStartTriangleIndex() { return startTriangleIndex; }
    public void setStartTriangleIndex(int startTriangleIndex) { this.startTriangleIndex = startTriangleIndex; }

    public int getTriangleCount() { return triangleCount; }
    public void setTriangleCount(int triangleCount) { this.triangleCount = triangleCount; }

    /**
     * Вычисляет матрицу преобразования модели из локального пространства в мировое:
     * M = T * R * S
     */
    public Matrix4f getModelMatrix() {
        return new Matrix4f()
                .translate(position)
                .rotate(rotation)
                .scale(scale);
    }

    /**
     * Вычисляет матрицу преобразования из мирового пространства в локальное пространство модели.
     * Для трансформации лучей (ray tracing) удобнее использовать обратную матрицу.
     */
    public Matrix4f getInverseModelMatrix() {
        return getModelMatrix().invert();
    }
}