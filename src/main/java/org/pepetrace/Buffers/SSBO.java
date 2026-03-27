package org.pepetrace.Buffers;

import static org.lwjgl.opengl.GL46.*;

/**
 * Класс SSBO (Shader Storage Buffer Object) представляет собой буфер данных, используемый для передачи данных между шейдерами.
 * <p>
 * SSBO лучше использовать для больших объемов данных, так как он хранит данные в виде массива и может эффективно обновляться.
 */
public class SSBO extends Buffer {

    protected int sizeBytes;
    protected int length;
    public int usage;

    /**
     * Конструктор SSBO.
     *
     * @param GL_DrawTarget режим использования буфера (например, GL_DRAW_INDIRECT)
     * @param binding       привязка (slot) к шейдеру. Используется в шейдере как `layout(std140, binding = <ваш binding>`
     */
    public SSBO(int GL_DrawTarget, int binding) {
        super(GL_SHADER_STORAGE_BUFFER, binding);
        setShaderBinding(binding);
        this.usage = GL_DrawTarget;
    }

    /**
     * Заполняет буфер данными, перезаписывая все старые данные.
     *
     * @param data данные для заполнения буфера. Может быть float[], int[] или double[].
     * @throws IllegalArgumentException если данные не являются float[], int[] или double[].
     */
    public void fillBuffer(Object data) throws IllegalArgumentException {
        bind();

        switch (data) {
            case float[] floatData -> {
                this.length = floatData.length;
                this.sizeBytes = floatData.length * Float.BYTES;
                glBufferData(GL_SHADER_STORAGE_BUFFER, floatData, usage);
            }
            case int[] intData -> {
                this.length = intData.length;
                this.sizeBytes = intData.length * Integer.BYTES;
                glBufferData(GL_SHADER_STORAGE_BUFFER, intData, usage);
            }
            case double[] doubleData -> {
                this.length = doubleData.length;
                this.sizeBytes = doubleData.length * Double.BYTES;
                glBufferData(GL_SHADER_STORAGE_BUFFER, doubleData, usage);
            }
            case null, default -> throw new IllegalArgumentException(
                "Unsupported type"
            );
        }

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, this.binding, id);
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

    /**
     * Возвращает длину буфера (количество элементов).
     *
     * @return длина буфера.
     */
    public int getLength() {
        return length;
    }
}
