package org.pepetrace.Scene;

import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BARRIER_BIT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.pepetrace.Buffers.SSBO;
import org.pepetrace.Camera;
import org.pepetrace.Scene.Loader.AssimpLoader;
import org.pepetrace.Scene.Loader.MeshData;
import org.pepetrace.Scene.Loader.MeshLoader;
import org.pepetrace.Scene.Material.Material;
import org.pepetrace.Scene.Material.TextureMaterial;

public class Scene implements AutoCloseable {
    private final ArrayList<Float> vertices = new ArrayList<>();
    private final ArrayList<Float> normals = new ArrayList<>();
    private final ArrayList<Float> uvs = new ArrayList<>();
    private final ArrayList<Float> tangents = new ArrayList<>();
    private final ArrayList<Float> bitangents = new ArrayList<>();
    private final ArrayList<Integer> indices = new ArrayList<>();
    private final ArrayList<TextureMaterial> materials = new ArrayList<>();
    private final ArrayList<Integer> materialIndicesPerTriangle = new ArrayList<>();
    private int triangleCount = 0;
    private int modelCount = 0;
    private final MeshLoader loader = new AssimpLoader();
    private final ArrayList<ModelMetadata> models = new ArrayList<>();
    private final ArrayList<Integer> modelTriangleStartIndices = new ArrayList<>();

    public Scene() {
        materials.add(TextureMaterial.create(
                "./src/main/resources/Textures/defaulta.png",
                "./src/main/resources/Textures/defaultn.png",
                "./src/main/resources/sunny_rose_garden_2k.hdr"
        ));
    }

    @Override
    public void close() throws Exception {
        for (Material m : materials) {
            m.close();
        }
    }

    public void loadModel(String path, int materialIndex, String modelName) {
        MeshData data = loader.load(path);

        float offsetX = modelCount * 2.0f;
        data.translate(0, 0, 0);

        int baseIndex = vertices.size() / 3;
        vertices.addAll(data.getVertices());
        normals.addAll(data.getNormals());
        uvs.addAll(data.getUVs());
        tangents.addAll(data.getTangents());
        bitangents.addAll(data.getBitangents());
        for (int idx : data.getIndices()) {
            indices.add(baseIndex + idx);
        }

        int newTriangles = data.getTriangleCount();
        for (int i = 0; i < newTriangles; i++) {
            materialIndicesPerTriangle.add(materialIndex);
        }

        int startTriangle = triangleCount;
        ModelMetadata meta = new ModelMetadata(modelName, startTriangle, newTriangles);
        models.add(meta);
        modelTriangleStartIndices.add(startTriangle);

        triangleCount += newTriangles;
        modelCount++;
    }

    public ModelMetadata getModelByTriangleIndex(int triangleIdx) {
        int pos = Collections.binarySearch(modelTriangleStartIndices, triangleIdx);
        if (pos < 0) {
            pos = -pos - 2;
        }
        if (pos >= 0 && pos < models.size()) {
            return models.get(pos);
        }
        return null;
    }

    public List<ModelMetadata> getModels() {
        return Collections.unmodifiableList(models);
    }

    public int getTriangleCount() {
        return triangleCount;
    }

    public int getModelCount() {
        return modelCount;
    }

    public ArrayList<TextureMaterial> getMaterials() {
        return (ArrayList<TextureMaterial>) materials.clone();
    }

    public Vector3f calculateModelCenter(int modelIndex) {
        ModelMetadata model = models.get(modelIndex);
        int startTri = model.getStartTriangleIndex();
        int triCount = model.getTriangleCount();
        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
        Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
        for (int i = startTri; i < startTri + triCount; i++) {
            int i0 = indices.get(i*3);
            int i1 = indices.get(i*3+1);
            int i2 = indices.get(i*3+2);
            Vector3f v0 = new Vector3f(vertices.get(i0*3), vertices.get(i0*3+1), vertices.get(i0*3+2));
            Vector3f v1 = new Vector3f(vertices.get(i1*3), vertices.get(i1*3+1), vertices.get(i1*3+2));
            Vector3f v2 = new Vector3f(vertices.get(i2*3), vertices.get(i2*3+1), vertices.get(i2*3+2));
            min.min(v0).min(v1).min(v2);
            max.max(v0).max(v1).max(v2);
        }
        return new Vector3f(min).add(max).mul(0.5f);
    }

    public void removeModel(int index) {
        if (index < 0 || index >= models.size()) return;

        ModelMetadata model = models.get(index);
        int startTri = model.getStartTriangleIndex();
        int triCount = model.getTriangleCount();

        // Удаляем индексы (3 индекса на треугольник)
        int idxPos = startTri * 3;
        for (int i = 0; i < triCount; i++) {
            indices.remove(idxPos);
            indices.remove(idxPos);
            indices.remove(idxPos);
        }
        // Удаляем материалы треугольников
        for (int i = 0; i < triCount; i++) {
            materialIndicesPerTriangle.remove(startTri);
        }

        // Корректируем стартовые индексы у оставшихся моделей
        for (int i = index + 1; i < models.size(); i++) {
            ModelMetadata next = models.get(i);
            next.setStartTriangleIndex(next.getStartTriangleIndex() - triCount);
        }

        models.remove(index);
        triangleCount -= triCount;
        modelCount--;

        // Обновляем modelTriangleStartIndices для быстрого поиска (если используется)
        modelTriangleStartIndices.clear();
        for (ModelMetadata m : models) {
            modelTriangleStartIndices.add(m.getStartTriangleIndex());
        }
    }

    private boolean rayTriangleIntersect(Vector3fc ro, Vector3fc rd, Vector3f v0, Vector3f v1, Vector3f v2, float[] outDist) {
        Vector3f v0v1 = new Vector3f(v1);
        v0v1.sub(v0);
        Vector3f v0v2 = new Vector3f(v2);
        v0v2.sub(v0);
        Vector3f pvec = new Vector3f(rd);
        pvec.cross(v0v2);
        float det = v0v1.dot(pvec);
        if (Math.abs(det) < 1e-8) return false;
        float invDet = 1.0f / det;
        Vector3f tvec = new Vector3f(ro);
        tvec.sub(v0);
        float u = tvec.dot(pvec) * invDet;
        if (u < 0 || u > 1) return false;
        Vector3f qvec = new Vector3f(tvec);
        qvec.cross(v0v1);
        float v = rd.dot(qvec) * invDet;
        if (v < 0 || u + v > 1) return false;
        float dist = v0v2.dot(qvec) * invDet;
        if (dist < 0) return false;
        outDist[0] = dist;
        return true;
    }

    private Vector3f getRayDirection(float screenX, float screenY, Camera camera, int viewportWidth, int viewportHeight) {
        float ndcX = (2.0f * screenX) / viewportWidth - 1.0f;
        float ndcY = 1.0f - (2.0f * screenY) / viewportHeight;
        return new Vector3f(ndcX, ndcY, 1.0f).normalize();
    }

    public int pickModel(float screenX, float screenY, Camera camera, int viewportWidth, int viewportHeight) {
        // Тут произошла великая битва меня и ебанного JOML
        // Короче, когда делаешь .cross .dot и другие и сохраняешь результат в переменную, то этот результат также сохранится туда, откуда делал данный запрос
        // Даже если из new Vector3f().cross, да-да.

        // 1. Вычисление NDC и aspect ratio
        float ndcX = (2.0f * screenX) / viewportWidth - 1.0f;
        float ndcY = 1.0f - (2.0f * screenY) / viewportHeight;
        float aspect = (float) viewportWidth / viewportHeight;
        float u = ndcX * aspect;
        float v = ndcY;

        // 2. Базис камеры
        float yawRad = (float) Math.toRadians(camera.getYawPitch().x);
        float pitchRad = (float) Math.toRadians(camera.getYawPitch().y);
        Vector3f forward = new Vector3f(
                (float)(Math.cos(pitchRad) * Math.sin(yawRad)),
                (float) Math.sin(pitchRad),
                (float)(Math.cos(pitchRad) * Math.cos(yawRad))
        ).normalize();


        Vector3f right = new Vector3f(forward);
        right.cross(new Vector3f(0, 1, 0));
        right.normalize();
        Vector3f up = new Vector3f(right);
        up.cross(forward);
        up.normalize();

        // 3. Направление луча в мировом пространстве
        Vector3f rayDir = new Vector3f(forward);
                rayDir.add(right.mul(u))
                .add(up.mul(v));
                rayDir.normalize(); // на всякий случай
        Vector3f rayOrigin = camera.getPosition();

        float minDist = Float.POSITIVE_INFINITY;
        int hitModelIdx = -1;
        float[] distOut = new float[1];

        // Для каждой модели преобразуем луч в локальное пространство
        for (int m = 0; m < models.size(); m++) {
            ModelMetadata model = models.get(m);
            Matrix4f invModelMatrix = model.getInverseModelMatrix();

            // Преобразуем начало луча в локальное пространство
            Vector3f localOrigin = new Vector3f(rayOrigin);
            localOrigin.mulPosition(invModelMatrix);
            // Преобразуем направление (важно использовать только поворот/масштаб, без переноса)
            Vector3f localDir = new Vector3f(rayDir);
            localDir.mulDirection(invModelMatrix);

            int startTri = model.getStartTriangleIndex();
            int endTri = startTri + model.getTriangleCount();
            for (int i = startTri; i < endTri; i++) {
                int i0 = indices.get(i*3);
                int i1 = indices.get(i*3+1);
                int i2 = indices.get(i*3+2);
                Vector3f v0 = new Vector3f(vertices.get(i0*3), vertices.get(i0*3+1), vertices.get(i0*3+2));
                Vector3f v1 = new Vector3f(vertices.get(i1*3), vertices.get(i1*3+1), vertices.get(i1*3+2));
                Vector3f v2 = new Vector3f(vertices.get(i2*3), vertices.get(i2*3+1), vertices.get(i2*3+2));
                if (rayTriangleIntersect(localOrigin, localDir, v0, v1, v2, distOut)) {
                    // Расстояние в локальном пространстве – прямое, так как масштаб искажает, но для сравнения подойдёт
                    if (distOut[0] < minDist && distOut[0] > 0) {
                        minDist = distOut[0];
                        hitModelIdx = m;
                    }
                }
            }
        }
        return hitModelIdx;
    }
    public void addMaterial(String albedoTexPath, String normalTexPath, String RMTTexPath) {
        materials.add(TextureMaterial.create(albedoTexPath, normalTexPath, RMTTexPath));
    }

    public void addMaterial(TextureMaterial mat) {
        materials.add(mat);
    }

    public void removeMaterial(int index) {
        materials.remove(index);
    }

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

    private int getModelIndexByTriangle(int triIdx) {
        for (int m = 0; m < models.size(); m++) {
            ModelMetadata model = models.get(m);
            int start = model.getStartTriangleIndex();
            int end = start + model.getTriangleCount();
            if (triIdx >= start && triIdx < end) return m;
        }
        return -1;
    }

    public void packScene(SSBO geometryBuffer, SSBO indexBuffer, SSBO materialIndicesBuffer,
                          SSBO materialHandlesBuffer, SSBO triangleModelIndicesBuffer) {
        int vertexCount = vertices.size() / 3;
        float[] geometryData = new float[vertexCount * 20];
        for (int i = 0; i < vertexCount; i++) {
            int base = i * 20;
            geometryData[base + 0] = vertices.get(i * 3);
            geometryData[base + 1] = vertices.get(i * 3 + 1);
            geometryData[base + 2] = vertices.get(i * 3 + 2);
            geometryData[base + 3] = 1.0f;                           // pad
            geometryData[base + 4] = normals.get(i * 3);
            geometryData[base + 5] = normals.get(i * 3 + 1);
            geometryData[base + 6] = normals.get(i * 3 + 2);
            geometryData[base + 7] = 0.0f;                           // pad
            geometryData[base + 8] = uvs.get(i * 2);
            geometryData[base + 9] = uvs.get(i * 2 + 1);
            geometryData[base +10] = 0.0f;                           // pad
            geometryData[base +11] = 0.0f;                           // pad
            geometryData[base +12] = tangents.get(i * 3);
            geometryData[base +13] = tangents.get(i * 3 + 1);
            geometryData[base +14] = tangents.get(i * 3 + 2);
            geometryData[base +15] = 0.0f;                           // pad
            geometryData[base +16] = bitangents.get(i * 3);
            geometryData[base +17] = bitangents.get(i * 3 + 1);
            geometryData[base +18] = bitangents.get(i * 3 + 2);
            geometryData[base +19] = 0.0f;                           // pad
        }
        geometryBuffer.fillBuffer(geometryData);

        // --- Индексы ---
        int[] indexData = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) indexData[i] = indices.get(i);
        indexBuffer.fillBuffer(indexData);

        // --- Материалы для каждого треугольника ---
        int[] materialIndicesData = new int[triangleCount];
        for (int i = 0; i < triangleCount; i++) materialIndicesData[i] = materialIndicesPerTriangle.get(i);
        materialIndicesBuffer.fillBuffer(materialIndicesData);

        // --- Массив стартовых индексов моделей (длина = modelCount + 1) ---
        int[] startIndices = new int[models.size() + 1];
        for (int i = 0; i < models.size(); i++) {
            startIndices[i] = models.get(i).getStartTriangleIndex();
        }
        startIndices[models.size()] = triangleCount;   // последний элемент = общее количество треугольников
        triangleModelIndicesBuffer.fillBuffer(startIndices);

        // --- Bindless handles материалов ---
        packMaterials(materialHandlesBuffer);

        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT | GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT);
    }

    public void updateModelMatricesOnGPU(SSBO modelMatricesBuffer) {
        // Каждая модель хранит две матрицы (forward и inverse) – 32 float
        float[] matricesData = new float[models.size() * 32];
        for (int i = 0; i < models.size(); i++) {
            Matrix4f forward = models.get(i).getModelMatrix();
            Matrix4f inverse = models.get(i).getInverseModelMatrix();
            forward.get(matricesData, i * 32);
            inverse.get(matricesData, i * 32 + 16);
        }
        modelMatricesBuffer.fillBuffer(matricesData);
    }
}