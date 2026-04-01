package org.pepetrace.Scene.OptimizationStructure;

import org.joml.Vector3f;

/**
 *  Класс, хранящий данные об Оси-направленной коробке (Axis-Aligned Bounding Box)
 */
public class AABB {
    private final Vector3f startPoint;
    private final Vector3f endPoint;

    private final Vector3f size;
    private final float volume;

    public AABB(Vector3f startPoint, Vector3f endPoint) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;

        this.size = (endPoint.sub(startPoint)).absolute();
        this.volume = size.x * size.y * size.z;
    }

    /**
     * @return Начальная точка (нижний левый угол)
     */
    public Vector3f getStartPoint() {
        return startPoint;
    }

    /**
     * @return Конечная точка (верхний правый угол)
     */
    public Vector3f getEndPoint() {
        return endPoint;
    }

    /**
     * @return Размер коробки
     */
    public Vector3f getSize() {
        return size;
    }

    /**
     * @return Объём коробки
     */
    public float getVolume() {
        return volume;
    }
}
