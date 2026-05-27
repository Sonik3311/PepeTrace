package org.pepetrace;

import org.pepetrace.Drawers.ViewportDrawer;
import org.pepetrace.Scene.Scene;
import java.util.*;

public class GlobalState {
    private static GlobalState instance;
    private Scene scene;
    private ViewportDrawer viewportDrawer;
    private Camera camera;
    private final HashMap<String, Object> arbitraryData = new HashMap<>();
    private final Set<Integer> selectedModelIndices = new HashSet<>();  // множество выбранных моделей
    private int maxMaterials = Integer.MAX_VALUE;

    private GlobalState() {}

    public static GlobalState getInstance() {
        if (instance == null) instance = new GlobalState();
        return instance;
    }

    public void initializeArbitraryData(String K, Object Value) {
        if (arbitraryData.get(K) == null) setArbitraryData(K, Value);
    }
    public void setArbitraryData(String K, Object Value) { arbitraryData.put(K, Value); }
    public Object getArbitraryData(String K) { return arbitraryData.get(K); }

    public void setViewportDrawer(ViewportDrawer drawer) { this.viewportDrawer = drawer; }
    public void setScene(Scene scene) { this.scene = scene; }
    public void setCamera(Camera camera) { this.camera = camera; }

    public ViewportDrawer getViewportDrawer() { return this.viewportDrawer; }
    public Scene getScene() { return this.scene; }
    public Camera getCamera() { return this.camera; }

    public void removeSelectedModel(int removedIndex) {
        Set<Integer> newSet = new HashSet<>();
        for (int idx : selectedModelIndices) {
            if (idx == removedIndex) continue;
            if (idx > removedIndex) newSet.add(idx - 1);
            else newSet.add(idx);
        }
        selectedModelIndices.clear();
        selectedModelIndices.addAll(newSet);
        notifySelectionChanged();
    }

    // ----- Новые методы для работы с выделением -----
    public Set<Integer> getSelectedModelIndices() {
        return Collections.unmodifiableSet(selectedModelIndices);
    }

    public int getMaxMaterials() { return maxMaterials; }
    public void setMaxMaterials(int maxMaterials) { this.maxMaterials = maxMaterials; }

    public void setSelectedModels(int singleIndex) {
        selectedModelIndices.clear();
        if (singleIndex >= 0) {
            selectedModelIndices.add(singleIndex);
        }
        notifySelectionChanged();
    }

    public void addSelectedModel(int index) {
        if (index >= 0) {
            selectedModelIndices.add(index);
            notifySelectionChanged();
        }
    }

    public void toggleModelSelection(int index) {
        if (index < 0) return;
        if (selectedModelIndices.contains(index)) {
            selectedModelIndices.remove(index);
        } else {
            selectedModelIndices.add(index);
        }
        notifySelectionChanged();
    }

    public void clearSelectedModels() {
        selectedModelIndices.clear();
        notifySelectionChanged();
    }

    private void notifySelectionChanged() {
        // TODO: при необходимости обновить UI (например, outliner)
        // Пока просто печатаем для отладки
        System.out.println("Selected models: " + selectedModelIndices);
    }

    // Устаревший метод для обратной совместимости (если где-то используется)
    @Deprecated
    public int getSelectedModelIndex() {
        return selectedModelIndices.isEmpty() ? -1 : selectedModelIndices.iterator().next();
    }

    @Deprecated
    public void setSelectedModelIndex(int index) {
        setSelectedModels(index);
    }
}