package org.pepetrace;

import org.pepetrace.Scene.Scene;

import java.util.HashMap;

public class GlobalState {
    private static GlobalState instance;

    // References to your main classes
    private Scene scene;
    private Drawer viewportDrawer;
    private Camera camera;
    private final HashMap<String, Object> arbitraryData = new HashMap<>();

    private GlobalState() {}

    public static GlobalState getInstance() {
        if (instance == null) {
            instance = new GlobalState();
        }
        return instance;
    }

    public void initializeArbitraryData(String K, Object Value) { if (arbitraryData.get(K) == null) { setArbitraryData(K, Value); } }
    public void setArbitraryData(String K, Object Value) { arbitraryData.put(K, Value); }
    public Object getArbitraryData(String K) { return arbitraryData.get(K); }

    public void setViewportDrawer(Drawer drawer) { this.viewportDrawer = drawer; }
    public void setScene(Scene scene) { this.scene = scene; }
    public void setCamera(Camera camera) { this.camera = camera; }

    public Drawer getViewportDrawer() { return this.viewportDrawer; }
    public Scene getScene() { return this.scene; }
    public Camera getCamera() { return this.camera; }


}
