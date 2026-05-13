package org.pepetrace.Scene;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.pepetrace.Scene.OptimizationStructure.AABB;

/**
 * Метаданные модели: имя, трансформация (позиция, поворот, масштаб),
 * диапазон треугольников в сцене, а также локальный и мировой AABB.
 * AABB используются для построения BLAS (модель) и TLAS (вся сцена).
 */
public class ModelMetadata {
    private String name;
    private Vector3f position;
    private Quaternionf rotation;
    private Vector3f scale;
    private int startTriangleIndex;
    private int triangleCount;

    // AABB в локальном пространстве модели (вычисляется один раз при загрузке)
    private final AABB localAABB;
    // AABB в мировом пространстве (пересчитывается при изменении трансформации)
    private AABB worldAABB;

    /**
     * Конструктор метаданных модели.
     *
     * @param name               имя модели
     * @param startTriangleIndex индекс первого треугольника в глобальном буфере сцены
     * @param triangleCount      количество треугольников модели
     * @param localMin           минимальная точка локального AABB
     * @param localMax           максимальная точка локального AABB
     */
    public ModelMetadata(String name, int startTriangleIndex, int triangleCount,
                         Vector3f localMin, Vector3f localMax) {
        this.name = name;
        this.startTriangleIndex = startTriangleIndex;
        this.triangleCount = triangleCount;
        this.position = new Vector3f(0, 0, 0);
        this.rotation = new Quaternionf();
        this.scale = new Vector3f(1, 1, 1);
        this.localAABB = new AABB(new Vector3f(localMin), new Vector3f(localMax));
        updateWorldAABB(); // сразу вычисляем мировой AABB
    }

    // --- Геттеры и сеттеры для трансформаций (с автоматическим обновлением мирового AABB) ---

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Vector3f getPosition() { return position; }
    public void setPosition(Vector3f position) {
        this.position.set(position);
        updateWorldAABB();
    }

    public Quaternionf getRotation() { return rotation; }
    public void setRotation(Quaternionf rotation) {
        this.rotation.set(rotation);
        updateWorldAABB();
    }

    public Vector3f getScale() { return scale; }
    public void setScale(Vector3f scale) {
        this.scale.set(scale);
        updateWorldAABB();
    }

    public int getStartTriangleIndex() { return startTriangleIndex; }
    public void setStartTriangleIndex(int startTriangleIndex) { this.startTriangleIndex = startTriangleIndex; }

    public int getTriangleCount() { return triangleCount; }
    public void setTriangleCount(int triangleCount) { this.triangleCount = triangleCount; }

    // --- AABB ---
    public AABB getLocalAABB() { return localAABB; }
    public AABB getWorldAABB() { return worldAABB; }

    /**
     * Пересчитывает мировой AABB на основе локального AABB и текущей трансформации.
     * Вызывается автоматически при изменении позиции, поворота или масштаба.
     */
    public void updateWorldAABB() {
        Vector3f localMin = localAABB.getStartPoint();
        Vector3f localMax = localAABB.getEndPoint();

        // Восемь углов локального AABB
        Vector3f[] corners = {
                new Vector3f(localMin.x, localMin.y, localMin.z),
                new Vector3f(localMax.x, localMin.y, localMin.z),
                new Vector3f(localMin.x, localMax.y, localMin.z),
                new Vector3f(localMax.x, localMax.y, localMin.z),
                new Vector3f(localMin.x, localMin.y, localMax.z),
                new Vector3f(localMax.x, localMin.y, localMax.z),
                new Vector3f(localMin.x, localMax.y, localMax.z),
                new Vector3f(localMax.x, localMax.y, localMax.z)
        };

        Matrix4f modelMatrix = getModelMatrix();
        Vector3f worldMin = new Vector3f(Float.POSITIVE_INFINITY);
        Vector3f worldMax = new Vector3f(Float.NEGATIVE_INFINITY);
        for (Vector3f corner : corners) {
            Vector3f worldCorner = modelMatrix.transformPosition(corner);
            worldMin.min(worldCorner);
            worldMax.max(worldCorner);
        }
        this.worldAABB = new AABB(worldMin, worldMax);
    }

    /**
     * Возвращает матрицу преобразования модели из локального пространства в мировое:
     * M = T * R * S
     */
    public Matrix4f getModelMatrix() {
        return new Matrix4f()
                .translate(position)
                .rotate(rotation)
                .scale(scale);
    }

    /**
     * Возвращает обратную матрицу преобразования (мировое → локальное).
     * Полезна для преобразования лучей при пересечении.
     */
    public Matrix4f getInverseModelMatrix() {
        return getModelMatrix().invert();
    }
}