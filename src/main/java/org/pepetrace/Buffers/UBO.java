package org.pepetrace.Buffers;

import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL46.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

/**
 * Абстрактный класс для универсальных буферов (UBO).
 * <p>
 * Лучше использовать тогда, когда данные малы, их размер известен заранее и не требуют частых обновлений (кэшируются на видеокарте)
 */
public abstract class UBO extends Buffer {

    protected ByteBuffer buffer;
    protected int sizeBytes;

    /**
     * Конструктор для создания UBO.
     *
     * @param sizeBytes размер буфера в байтах.
     * @param binding   привязка (slot) к шейдеру. Используется в шейдере как `layout (binding = <binding>) uniform`.
     */
    public UBO(int sizeBytes, int binding) {
        super(GL_UNIFORM_BUFFER, binding);
        this.sizeBytes = sizeBytes;
        buffer = ByteBuffer.allocateDirect(sizeBytes).order(
            ByteOrder.nativeOrder()
        );

        bind();
        glBufferData(GL_UNIFORM_BUFFER, sizeBytes, GL_DYNAMIC_DRAW);
        unbind();
        setShaderBinding(binding);
    }

    /**
     * Помещает float-значение в буфер по указанному смещению.
     * <p>
     * Занимает 4 байта.
     *
     * @param offsetBytes смещение в байтах.
     * @param value       значение для помещения.
     */
    protected void putFloat(int offsetBytes, float value) {
        buffer.putFloat(offsetBytes, value);
    }

    /**
     * Помещает int-значение в буфер по указанному смещению.
     * <p>
     * Занимает 4 байта.
     *
     * @param offsetBytes смещение в байтах.
     * @param value       значение для помещения.
     */
    protected void putInt(int offsetBytes, int value) {
        buffer.putInt(offsetBytes, value);
    }

    /**
     * Помещает boolean-значение в буфер по указанному смещению.
     * <p>
     * Занимает 4 байта.
     *
     * @param offsetBytes смещение в байтах.
     * @param value       значение для помещения.
     */
    protected void putBoolean(int offsetBytes, boolean value) {
        buffer.putInt(offsetBytes, value ? 1 : 0);
    }

    /**
     * Помещает Vector2f в буфер по указанному смещению.
     * <p>
     * Занимает 8 байт.
     *
     * @param offsetBytes смещение в байтах.
     * @param vec2        значение для помещения.
     */
    protected void putVec2(int offsetBytes, Vector2f vec2) {
        buffer.putFloat(offsetBytes, vec2.x);
        buffer.putFloat(offsetBytes + 4, vec2.y);
    }

    /**
     * Помещает Vector3f в буфер по указанному смещению.
     * <p>
     * Занимает 12 байт + 4 байта (выравнивание до 16 байт по std140).
     *
     * @param offsetBytes смещение в байтах.
     * @param vec3        значение для помещения.
     */
    protected void putVec3(int offsetBytes, Vector3f vec3) {
        buffer.putFloat(offsetBytes, vec3.x);
        buffer.putFloat(offsetBytes + 4, vec3.y);
        buffer.putFloat(offsetBytes + 8, vec3.z);
    }

    /**
     * Помещает Vector4f в буфер по указанному смещению.
     * <p>
     * Занимает 16 байт.
     *
     * @param offsetBytes смещение в байтах.
     * @param vec4        значение для помещения.
     */
    protected void putVec4(int offsetBytes, Vector4f vec4) {
        buffer.putFloat(offsetBytes, vec4.x);
        buffer.putFloat(offsetBytes + 4, vec4.y);
        buffer.putFloat(offsetBytes + 8, vec4.z);
        buffer.putFloat(offsetBytes + 12, vec4.w);
    }

    /**
     * Загружает данные из буфера на ГП.
     */
    protected void uploadToGPU() {
        bind();
        glBufferSubData(GL_UNIFORM_BUFFER, 0, buffer);
        unbind();
    }

    /**
     * Возвращает размер буфера в байтах.
     *
     * @return размер буфера в байтах.
     */
    public int getSizeBytes() {
        return sizeBytes;
    }
}
