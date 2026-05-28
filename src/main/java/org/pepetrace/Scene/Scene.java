package org.pepetrace.Scene;

import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BARRIER_BIT;

import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.pepetrace.Buffers.SSBO;
import org.pepetrace.Camera;
import org.pepetrace.GlobalState;
import org.pepetrace.Scene.Loader.AssimpLoader;
import org.pepetrace.Scene.Loader.MeshData;
import org.pepetrace.Scene.Loader.MeshLoader;
import org.pepetrace.Scene.Material.Material;
import org.pepetrace.Scene.Material.TextureMaterial;
import org.pepetrace.Scene.OptimizationStructure.AABB;

public class Scene implements AutoCloseable {

    private final ArrayList<Float> vertices = new ArrayList<>();
    private final ArrayList<Float> normals = new ArrayList<>();
    private final ArrayList<Float> uvs = new ArrayList<>();
    private final ArrayList<Float> tangents = new ArrayList<>();
    private final ArrayList<Float> bitangents = new ArrayList<>();
    private final ArrayList<Integer> indices = new ArrayList<>();
    private final ArrayList<TextureMaterial> materials = new ArrayList<>();
    private final ArrayList<Integer> materialIndicesPerTriangle =
        new ArrayList<>();
    private final Map<TextureMaterial, Integer> materialRefCount =
        new HashMap<>();
    private int triangleCount = 0;
    private int modelCount = 0;
    private final MeshLoader loader = new AssimpLoader();
    private final ArrayList<ModelMetadata> models = new ArrayList<>();
    private final ArrayList<Integer> modelTriangleStartIndices =
        new ArrayList<>();
    private AABB tlasAABB; // общий bounding box всей сцены

    public Scene() {
        TextureMaterial defaultMat = TextureMaterial.create(
            "./src/main/resources/Textures/defaulta.png",
            "./src/main/resources/Textures/defaultn.png",
            "./src/main/resources/Textures/defaultrmt.png"
        );
        materials.add(defaultMat);
        materialRefCount.put(defaultMat, 1); // одна модель (дефолтный материал используется в сцене)
    }

    public void updateTLAS() {
        if (models.isEmpty()) {
            tlasAABB = null;
            return;
        }
        Vector3f globalMin = new Vector3f(Float.POSITIVE_INFINITY);
        Vector3f globalMax = new Vector3f(Float.NEGATIVE_INFINITY);
        for (ModelMetadata model : models) {
            AABB worldAABB = model.getWorldAABB();
            globalMin.min(worldAABB.getStartPoint());
            globalMax.max(worldAABB.getEndPoint());
        }
        tlasAABB = new AABB(globalMin, globalMax);
    }

    public AABB getTLAS() {
        return tlasAABB;
    }

    @Override
    public void close() throws Exception {
        for (Material m : materials) {
            m.close();
        }
    }

    public void loadModel(String path, int materialIndex, String modelName) {
        if (materials.isEmpty()) {
            // Добавляем дефолтный материал, если список пуст
            TextureMaterial defaultMat = TextureMaterial.create(
                "./src/main/resources/Textures/defaulta.png",
                "./src/main/resources/Textures/defaultn.png",
                "./src/main/resources/sunny_rose_garden_2k.hdr"
            );
            materials.add(defaultMat);
            materialRefCount.put(defaultMat, 0);
        }

        // Загружаем геометрию через Assimp
        MeshData data = loader.load(path);

        // --- Вычисляем локальный AABB модели на основе вершин ---
        Vector3f localMin = new Vector3f(Float.POSITIVE_INFINITY);
        Vector3f localMax = new Vector3f(Float.NEGATIVE_INFINITY);
        List<Float> verticesData = data.getVertices();
        for (int i = 0; i < verticesData.size(); i += 3) {
            float x = verticesData.get(i);
            float y = verticesData.get(i + 1);
            float z = verticesData.get(i + 2);
            localMin.min(new Vector3f(x, y, z));
            localMax.max(new Vector3f(x, y, z));
        }

        // Добавляем вершины в общие буферы сцены
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
        TextureMaterial mat = materials.get(materialIndex);
        for (int i = 0; i < newTriangles; i++) {
            materialIndicesPerTriangle.add(materialIndex);
        }

        int startTriangle = triangleCount;
        // Создаём метаданные модели с вычисленным AABB
        ModelMetadata meta = new ModelMetadata(
            modelName,
            startTriangle,
            newTriangles,
            localMin,
            localMax,
            materialIndex
        );
        models.add(meta);
        modelTriangleStartIndices.add(startTriangle);

        triangleCount += newTriangles;
        modelCount++;

        // Увеличиваем счётчик использования материала
        materialRefCount.merge(mat, 1, Integer::sum);

        // Пересчитываем TLAS (общий bounding box сцены)
        updateTLAS();
    }

    public ModelMetadata getModelByTriangleIndex(int triangleIdx) {
        int pos = Collections.binarySearch(
            modelTriangleStartIndices,
            triangleIdx
        );
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
            int i0 = indices.get(i * 3);
            int i1 = indices.get(i * 3 + 1);
            int i2 = indices.get(i * 3 + 2);
            Vector3f v0 = new Vector3f(
                vertices.get(i0 * 3),
                vertices.get(i0 * 3 + 1),
                vertices.get(i0 * 3 + 2)
            );
            Vector3f v1 = new Vector3f(
                vertices.get(i1 * 3),
                vertices.get(i1 * 3 + 1),
                vertices.get(i1 * 3 + 2)
            );
            Vector3f v2 = new Vector3f(
                vertices.get(i2 * 3),
                vertices.get(i2 * 3 + 1),
                vertices.get(i2 * 3 + 2)
            );
            min.min(v0).min(v1).min(v2);
            max.max(v0).max(v1).max(v2);
        }
        return new Vector3f(min).add(max).mul(0.5f);
    }

    private void rebuildGeometryData() {
        // Собираем все вершины, которые используются в оставшихся треугольниках
        Set<Integer> usedVertices = new HashSet<>();
        for (int triIdx = 0; triIdx < triangleCount; triIdx++) {
            int i0 = indices.get(triIdx * 3);
            int i1 = indices.get(triIdx * 3 + 1);
            int i2 = indices.get(triIdx * 3 + 2);
            usedVertices.add(i0);
            usedVertices.add(i1);
            usedVertices.add(i2);
        }

        // Создаём отображение старый индекс -> новый индекс
        Map<Integer, Integer> indexMap = new HashMap<>();
        List<Float> newVertices = new ArrayList<>();
        List<Float> newNormals = new ArrayList<>();
        List<Float> newUVs = new ArrayList<>();
        List<Float> newTangents = new ArrayList<>();
        List<Float> newBitangents = new ArrayList<>();

        int newIdx = 0;
        for (int oldIdx : usedVertices) {
            indexMap.put(oldIdx, newIdx);
            newVertices.add(vertices.get(oldIdx * 3));
            newVertices.add(vertices.get(oldIdx * 3 + 1));
            newVertices.add(vertices.get(oldIdx * 3 + 2));
            newNormals.add(normals.get(oldIdx * 3));
            newNormals.add(normals.get(oldIdx * 3 + 1));
            newNormals.add(normals.get(oldIdx * 3 + 2));
            newUVs.add(uvs.get(oldIdx * 2));
            newUVs.add(uvs.get(oldIdx * 2 + 1));
            newTangents.add(tangents.get(oldIdx * 3));
            newTangents.add(tangents.get(oldIdx * 3 + 1));
            newTangents.add(tangents.get(oldIdx * 3 + 2));
            newBitangents.add(bitangents.get(oldIdx * 3));
            newBitangents.add(bitangents.get(oldIdx * 3 + 1));
            newBitangents.add(bitangents.get(oldIdx * 3 + 2));
            newIdx++;
        }

        // Перестраиваем индексы
        List<Integer> newIndices = new ArrayList<>();
        for (int triIdx = 0; triIdx < triangleCount; triIdx++) {
            int oldI0 = indices.get(triIdx * 3);
            int oldI1 = indices.get(triIdx * 3 + 1);
            int oldI2 = indices.get(triIdx * 3 + 2);
            newIndices.add(indexMap.get(oldI0));
            newIndices.add(indexMap.get(oldI1));
            newIndices.add(indexMap.get(oldI2));
        }

        // Заменяем старые списки новыми
        vertices.clear();
        vertices.addAll(newVertices);
        normals.clear();
        normals.addAll(newNormals);
        uvs.clear();
        uvs.addAll(newUVs);
        tangents.clear();
        tangents.addAll(newTangents);
        bitangents.clear();
        bitangents.addAll(newBitangents);
        indices.clear();
        indices.addAll(newIndices);
    }

    private void remapMaterialIndicesAfterRemoval(int removedMatIdx) {
        for (int i = 0; i < materialIndicesPerTriangle.size(); i++) {
            int oldIdx = materialIndicesPerTriangle.get(i);
            if (oldIdx > removedMatIdx) {
                materialIndicesPerTriangle.set(i, oldIdx - 1);
            }
        }
    }

    public void removeModel(int index) {
        if (index < 0 || index >= models.size()) return;

        ModelMetadata model = models.get(index);
        int startTri = model.getStartTriangleIndex();
        int triCount = model.getTriangleCount();

        // Сохраняем индексы материалов, используемых удаляемой моделью
        Set<Integer> materialsToRelease = new HashSet<>();
        for (int i = startTri; i < startTri + triCount; i++) {
            materialsToRelease.add(materialIndicesPerTriangle.get(i));
        }

        // 1. Удаляем индексы треугольников
        int idxPos = startTri * 3;
        for (int i = 0; i < triCount; i++) {
            indices.remove(idxPos);
            indices.remove(idxPos);
            indices.remove(idxPos);
        }

        // 2. Удаляем материалы треугольников
        for (int i = 0; i < triCount; i++) {
            materialIndicesPerTriangle.remove(startTri);
        }

        // 3. Корректируем startTriangleIndex у последующих моделей
        for (int i = index + 1; i < models.size(); i++) {
            ModelMetadata next = models.get(i);
            next.setStartTriangleIndex(next.getStartTriangleIndex() - triCount);
        }

        // 4. Удаляем модель из списков
        models.remove(index);
        triangleCount -= triCount;
        modelCount--;

        // 5. Если моделей не осталось – полная очистка
        if (models.isEmpty()) {
            vertices.clear();
            normals.clear();
            uvs.clear();
            tangents.clear();
            bitangents.clear();
            indices.clear();
            materialIndicesPerTriangle.clear();
            triangleCount = 0;
            modelCount = 0;
            modelTriangleStartIndices.clear();

            // // Закрываем все материалы
            // for (TextureMaterial mat : materials) {
            //     try {
            //         mat.close();
            //     } catch (Exception e) {
            //         e.printStackTrace();
            //     }
            // }
            // materials.clear();
            // materialRefCount.clear();

            return;
        }
        // 6. Перестраиваем вершины (удаляем мёртвые)
        rebuildGeometryData();

        // 7. Перестраиваем modelTriangleStartIndices
        modelTriangleStartIndices.clear();
        for (ModelMetadata m : models) {
            modelTriangleStartIndices.add(m.getStartTriangleIndex());
        }

        // 8. Уменьшаем счётчики материалов и удаляем неиспользуемые
        for (int matIdx : materialsToRelease) {
            TextureMaterial mat = materials.get(matIdx);
            int newCount = materialRefCount.merge(
                mat,
                -1,
                (old, delta) -> old + delta
            );
            if (newCount == 0) {
                try {
                    mat.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                materials.remove(matIdx);
                materialRefCount.remove(mat);
                // После удаления материала сдвигаем индексы в materialIndicesPerTriangle
                remapMaterialIndicesAfterRemoval(matIdx);
            }
        }
    }

    private boolean rayTriangleIntersect(
        Vector3fc ro,
        Vector3fc rd,
        Vector3f v0,
        Vector3f v1,
        Vector3f v2,
        float[] outDist
    ) {
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

    private Vector3f getRayDirection(
        float screenX,
        float screenY,
        Camera camera,
        int viewportWidth,
        int viewportHeight
    ) {
        float ndcX = (2.0f * screenX) / viewportWidth - 1.0f;
        float ndcY = 1.0f - (2.0f * screenY) / viewportHeight;
        return new Vector3f(ndcX, ndcY, 1.0f).normalize();
    }

    public int pickModel(
        float screenX,
        float screenY,
        Camera camera,
        int viewportWidth,
        int viewportHeight
    ) {
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
            (float) (Math.cos(pitchRad) * Math.sin(yawRad)),
            (float) Math.sin(pitchRad),
            (float) (Math.cos(pitchRad) * Math.cos(yawRad))
        ).normalize();

        Vector3f right = new Vector3f(forward);
        right.cross(new Vector3f(0, 1, 0));
        right.normalize();
        Vector3f up = new Vector3f(right);
        up.cross(forward);
        up.normalize();

        // 3. Направление луча в мировом пространстве
        Vector3f rayDir = new Vector3f(forward);
        rayDir.add(right.mul(u)).add(up.mul(v));
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
                int i0 = indices.get(i * 3);
                int i1 = indices.get(i * 3 + 1);
                int i2 = indices.get(i * 3 + 2);
                Vector3f v0 = new Vector3f(
                    vertices.get(i0 * 3),
                    vertices.get(i0 * 3 + 1),
                    vertices.get(i0 * 3 + 2)
                );
                Vector3f v1 = new Vector3f(
                    vertices.get(i1 * 3),
                    vertices.get(i1 * 3 + 1),
                    vertices.get(i1 * 3 + 2)
                );
                Vector3f v2 = new Vector3f(
                    vertices.get(i2 * 3),
                    vertices.get(i2 * 3 + 1),
                    vertices.get(i2 * 3 + 2)
                );
                if (
                    rayTriangleIntersect(
                        localOrigin,
                        localDir,
                        v0,
                        v1,
                        v2,
                        distOut
                    )
                ) {
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

    public void addMaterial(
        String albedoTexPath,
        String normalTexPath,
        String RMTTexPath
    ) {
        if (materials.size() >= GlobalState.getInstance().getMaxMaterials()) {
            System.err.println(
                "Cannot add material: limit of " +
                    GlobalState.getInstance().getMaxMaterials() +
                    " reached"
            );
            return;
        }
        materials.add(
            TextureMaterial.create(albedoTexPath, normalTexPath, RMTTexPath)
        );
    }

    public void addMaterial(TextureMaterial mat) {
        if (materials.size() >= GlobalState.getInstance().getMaxMaterials()) {
            System.err.println(
                "Cannot add material: limit of " +
                    GlobalState.getInstance().getMaxMaterials() +
                    " reached"
            );
            return;
        }
        materials.add(mat);
    }

    public void removeMaterial(int index) {
        materials.remove(index);
    }

    public void setModelMaterial(int modelIndex, int newMaterialIndex) {
        if (modelIndex < 0 || modelIndex >= models.size()) return;
        if (
            newMaterialIndex < 0 || newMaterialIndex >= materials.size()
        ) return;

        ModelMetadata model = models.get(modelIndex);
        int oldMaterialIndex = model.getMaterialIndex();
        if (oldMaterialIndex == newMaterialIndex) return;

        // Update material indices for all triangles of this model
        int startTri = model.getStartTriangleIndex();
        int triCount = model.getTriangleCount();
        for (int i = startTri; i < startTri + triCount; i++) {
            materialIndicesPerTriangle.set(i, newMaterialIndex);
        }

        // Update ref counts
        TextureMaterial oldMat = materials.get(oldMaterialIndex);
        TextureMaterial newMat = materials.get(newMaterialIndex);
        int newCount = materialRefCount.merge(
            oldMat,
            -1,
            (old, delta) -> old + delta
        );
        if (newCount <= 0) {
            materialRefCount.remove(oldMat);
        }
        materialRefCount.merge(newMat, 1, Integer::sum);

        model.setMaterialIndex(newMaterialIndex);
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

    public void packScene(
        SSBO geometryBuffer,
        SSBO indexBuffer,
        SSBO materialIndicesBuffer,
        SSBO materialHandlesBuffer,
        SSBO triangleModelIndicesBuffer
    ) {
        if (models.isEmpty()) {
            geometryBuffer.clear();
            indexBuffer.clear();
            materialIndicesBuffer.clear();
            materialHandlesBuffer.clear();
            triangleModelIndicesBuffer.clear();
            // Также очищаем буфер матриц моделей (он не передаётся в этот метод, но очистим отдельно)
            // Он будет очищен в updateModelMatricesOnGPU при вызове с пустым списком
            return;
        }

        int vertexCount = vertices.size() / 3;
        float[] geometryData = new float[vertexCount * 20];
        for (int i = 0; i < vertexCount; i++) {
            int base = i * 20;
            geometryData[base + 0] = vertices.get(i * 3);
            geometryData[base + 1] = vertices.get(i * 3 + 1);
            geometryData[base + 2] = vertices.get(i * 3 + 2);
            geometryData[base + 3] = 1.0f; // pad
            geometryData[base + 4] = normals.get(i * 3);
            geometryData[base + 5] = normals.get(i * 3 + 1);
            geometryData[base + 6] = normals.get(i * 3 + 2);
            geometryData[base + 7] = 0.0f; // pad
            geometryData[base + 8] = uvs.get(i * 2);
            geometryData[base + 9] = uvs.get(i * 2 + 1);
            geometryData[base + 10] = 0.0f; // pad
            geometryData[base + 11] = 0.0f; // pad
            geometryData[base + 12] = tangents.get(i * 3);
            geometryData[base + 13] = tangents.get(i * 3 + 1);
            geometryData[base + 14] = tangents.get(i * 3 + 2);
            geometryData[base + 15] = 0.0f; // pad
            geometryData[base + 16] = bitangents.get(i * 3);
            geometryData[base + 17] = bitangents.get(i * 3 + 1);
            geometryData[base + 18] = bitangents.get(i * 3 + 2);
            geometryData[base + 19] = 0.0f; // pad
        }
        geometryBuffer.fillBuffer(geometryData);

        // --- Индексы ---
        int[] indexData = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) indexData[i] = indices.get(i);
        indexBuffer.fillBuffer(indexData);

        // --- Материалы для каждого треугольника ---
        int[] materialIndicesData = new int[triangleCount];
        for (int i = 0; i < triangleCount; i++) materialIndicesData[i] =
            materialIndicesPerTriangle.get(i);
        materialIndicesBuffer.fillBuffer(materialIndicesData);

        // --- Массив стартовых индексов моделей (длина = modelCount + 1) ---
        int[] startIndices = new int[models.size() + 1];
        for (int i = 0; i < models.size(); i++) {
            startIndices[i] = models.get(i).getStartTriangleIndex();
        }
        startIndices[models.size()] = triangleCount; // последний элемент = общее количество треугольников
        triangleModelIndicesBuffer.fillBuffer(startIndices);

        // --- Bindless handles материалов ---
        packMaterials(materialHandlesBuffer);

        glMemoryBarrier(
            GL_SHADER_STORAGE_BARRIER_BIT | GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT
        );
    }

    public void updateModelMatricesOnGPU(SSBO modelMatricesBuffer) {
        if (models.isEmpty()) {
            modelMatricesBuffer.clear();
            return;
        }
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
