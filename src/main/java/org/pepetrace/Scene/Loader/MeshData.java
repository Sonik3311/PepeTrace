package org.pepetrace.Scene.Loader;

import java.util.ArrayList;
import java.util.List;

public class MeshData {
    private final List<Float> vertices;
    private final List<Float> normals;
    private final List<Float> uvs;
    private final List<Float> tangents;
    private final List<Float> bitangents;
    private final List<Integer> indices;

    public MeshData() {
        vertices = new ArrayList<>();
        normals = new ArrayList<>();
        uvs = new ArrayList<>();
        tangents = new ArrayList<>();
        bitangents = new ArrayList<>();
        indices = new ArrayList<>();
    }

    public void addVertex(float x, float y, float z) {
        vertices.add(x); vertices.add(y); vertices.add(z);
    }
    public void addNormal(float nx, float ny, float nz) {
        normals.add(nx); normals.add(ny); normals.add(nz);
    }
    public void addUV(float u, float v) {
        uvs.add(u); uvs.add(v);
    }
    public void addTangent(float tx, float ty, float tz) {
        tangents.add(tx); tangents.add(ty); tangents.add(tz);
    }
    public void addBitangent(float bx, float by, float bz) {
        bitangents.add(bx); bitangents.add(by); bitangents.add(bz);
    }
    public void addIndex(int idx) {
        indices.add(idx);
    }

    public List<Float> getVertices() { return vertices; }
    public List<Float> getNormals() { return normals; }
    public List<Float> getUVs() { return uvs; }
    public List<Float> getTangents() { return tangents; }
    public List<Float> getBitangents() { return bitangents; }
    public List<Integer> getIndices() { return indices; }
    public int getVertexCount() { return vertices.size() / 3; }
    public int getTriangleCount() { return indices.size() / 3; }

    // ----- Генерация отсутствующих данных -----

    /**
     * Вычисляет вершинные нормали усреднением нормалей инцидентных треугольников.
     * Заменяет существующие нормали.
     */
    public void computeNormals() {
        int vertexCount = getVertexCount();
        float[] sumX = new float[vertexCount];
        float[] sumY = new float[vertexCount];
        float[] sumZ = new float[vertexCount];
        int[] count = new int[vertexCount];

        for (int i = 0; i < indices.size(); i += 3) {
            int i0 = indices.get(i);
            int i1 = indices.get(i+1);
            int i2 = indices.get(i+2);

            if (i0 >= vertexCount || i1 >= vertexCount || i2 >= vertexCount ||
                    i0 < 0 || i1 < 0 || i2 < 0) {
                System.err.println("computeNormals: skipping invalid triangle indices: "
                        + i0 + ", " + i1 + ", " + i2);
                continue;
            }

            float x0 = vertices.get(i0*3);
            float y0 = vertices.get(i0*3+1);
            float z0 = vertices.get(i0*3+2);
            float x1 = vertices.get(i1*3);
            float y1 = vertices.get(i1*3+1);
            float z1 = vertices.get(i1*3+2);
            float x2 = vertices.get(i2*3);
            float y2 = vertices.get(i2*3+1);
            float z2 = vertices.get(i2*3+2);

            // Нормаль треугольника (без нормализации, для усреднения)
            float nx = (y1 - y0) * (z2 - z0) - (z1 - z0) * (y2 - y0);
            float ny = (z1 - z0) * (x2 - x0) - (x1 - x0) * (z2 - z0);
            float nz = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0);

            sumX[i0] += nx; sumY[i0] += ny; sumZ[i0] += nz;
            sumX[i1] += nx; sumY[i1] += ny; sumZ[i1] += nz;
            sumX[i2] += nx; sumY[i2] += ny; sumZ[i2] += nz;
            count[i0]++; count[i1]++; count[i2]++;
        }

        normals.clear();
        for (int i = 0; i < vertexCount; i++) {
            if (count[i] > 0) {
                float nx = sumX[i] / count[i];
                float ny = sumY[i] / count[i];
                float nz = sumZ[i] / count[i];
                float len = (float) Math.sqrt(nx*nx + ny*ny + nz*nz);
                if (len > 1e-6f) {
                    nx /= len; ny /= len; nz /= len;
                }
                normals.add(nx); normals.add(ny); normals.add(nz);
            } else {
                normals.add(0f); normals.add(1f); normals.add(0f);
            }
        }
    }

    /**
     * Вычисляет тангенты и битангенты на основе существующих нормалей и UV.
     * Алгоритм Миккельсена (усреднение по треугольникам).
     * Требует наличия UV и нормалей.
     */
    public void computeTangentsAndBitangents() {
        int vertexCount = getVertexCount();
        int triangleCount = indices.size() / 3;

        float[] tan1X = new float[vertexCount];
        float[] tan1Y = new float[vertexCount];
        float[] tan1Z = new float[vertexCount];
        float[] tan2X = new float[vertexCount];
        float[] tan2Y = new float[vertexCount];
        float[] tan2Z = new float[vertexCount];

        for (int i = 0; i < triangleCount; i++) {
            int i0 = indices.get(i*3);
            int i1 = indices.get(i*3+1);
            int i2 = indices.get(i*3+2);

            float x0 = vertices.get(i0*3);
            float y0 = vertices.get(i0*3+1);
            float z0 = vertices.get(i0*3+2);
            float x1 = vertices.get(i1*3);
            float y1 = vertices.get(i1*3+1);
            float z1 = vertices.get(i1*3+2);
            float x2 = vertices.get(i2*3);
            float y2 = vertices.get(i2*3+1);
            float z2 = vertices.get(i2*3+2);

            float u0 = uvs.get(i0*2);
            float v0 = uvs.get(i0*2+1);
            float u1 = uvs.get(i1*2);
            float v1 = uvs.get(i1*2+1);
            float u2 = uvs.get(i2*2);
            float v2 = uvs.get(i2*2+1);

            float du1 = u1 - u0;
            float dv1 = v1 - v0;
            float du2 = u2 - u0;
            float dv2 = v2 - v0;

            float ex1 = x1 - x0;
            float ey1 = y1 - y0;
            float ez1 = z1 - z0;
            float ex2 = x2 - x0;
            float ey2 = y2 - y0;
            float ez2 = z2 - z0;

            float det = du1 * dv2 - du2 * dv1;
            float r = (Math.abs(det) < 1e-6f) ? 0.0f : 1.0f / det;

            float tx = (ex1 * dv2 - ex2 * dv1) * r;
            float ty = (ey1 * dv2 - ey2 * dv1) * r;
            float tz = (ez1 * dv2 - ez2 * dv1) * r;

            float bx = (ex2 * du1 - ex1 * du2) * r;
            float by = (ey2 * du1 - ey1 * du2) * r;
            float bz = (ez2 * du1 - ez1 * du2) * r;

            tan1X[i0] += tx; tan1Y[i0] += ty; tan1Z[i0] += tz;
            tan1X[i1] += tx; tan1Y[i1] += ty; tan1Z[i1] += tz;
            tan1X[i2] += tx; tan1Y[i2] += ty; tan1Z[i2] += tz;

            tan2X[i0] += bx; tan2Y[i0] += by; tan2Z[i0] += bz;
            tan2X[i1] += bx; tan2Y[i1] += by; tan2Z[i1] += bz;
            tan2X[i2] += bx; tan2Y[i2] += by; tan2Z[i2] += bz;
        }

        tangents.clear();
        bitangents.clear();
        for (int i = 0; i < vertexCount; i++) {
            float nx = normals.get(i*3);
            float ny = normals.get(i*3+1);
            float nz = normals.get(i*3+2);

            float tx = tan1X[i];
            float ty = tan1Y[i];
            float tz = tan1Z[i];

            float bx = tan2X[i];
            float by = tan2Y[i];
            float bz = tan2Z[i];

            // Ортогонализация (Грамм-Шмидт)
            float dot = tx*nx + ty*ny + tz*nz;
            tx -= dot * nx;
            ty -= dot * ny;
            tz -= dot * nz;
            float lenTan = (float) Math.sqrt(tx*tx + ty*ty + tz*tz);
            if (lenTan > 1e-6f) {
                tx /= lenTan; ty /= lenTan; tz /= lenTan;
            } else {
                tx = 1; ty = 0; tz = 0;
            }

            // Битангент = cross(normal, tangent) * sign определителя
            float cx = ny*tz - nz*ty;
            float cy = nz*tx - nx*tz;
            float cz = nx*ty - ny*tx;
            float sign = (bx*cx + by*cy + bz*cz) >= 0 ? 1.0f : -1.0f;

            tangents.add(tx); tangents.add(ty); tangents.add(tz);
            bitangents.add(cx * sign); bitangents.add(cy * sign); bitangents.add(cz * sign);
        }
    }
}