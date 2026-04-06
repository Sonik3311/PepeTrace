package org.pepetrace.Scene.Loader;

import java.util.ArrayList;

public interface TriangleLoader {
    /**
     * Загружает треугольники
     * @param path Путь до файла
     * @return
     */
    ArrayList<Float> loadTriangles(String path);
}
