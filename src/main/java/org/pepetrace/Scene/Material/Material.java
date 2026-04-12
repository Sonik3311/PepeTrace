package org.pepetrace.Scene.Material;

public class Material implements AutoCloseable {
    private long id;
    private static long nextMaterialID = 0;

    public Material() {
        this.id = nextMaterialID;
        nextMaterialID++;
    }

    public void destroy() {}

    @Override
    public void close() throws Exception {}
}
