package org.pepetrace.Scene.Material;

public class Material implements AutoCloseable {
    private long id;
    private static long nextMaterialID = 0;

    public Material() {
        this.id = nextMaterialID;
        nextMaterialID++;
    }

    public void destroy() {}

    public long getID() {
        return id;
    }

    @Override
    public void close() throws Exception {}
}
