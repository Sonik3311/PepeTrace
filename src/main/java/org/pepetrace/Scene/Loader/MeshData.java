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
}