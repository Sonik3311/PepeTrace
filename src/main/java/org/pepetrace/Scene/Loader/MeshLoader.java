package org.pepetrace.Scene.Loader;

import java.util.ArrayList;
import java.util.List;

public interface MeshLoader {
    /**
     * Загружает точки треугольников, нормали и UV координаты модели
     * @param path Путь до файла
     * @return List<ArrayList<Точки>, ArrayList<Нормали>, ArrayList<UV>>
     */
    List<ArrayList<Float>> load(String path);
}
