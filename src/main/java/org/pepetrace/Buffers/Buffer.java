package org.pepetrace.Buffers;

import static org.lwjgl.opengl.GL46.*;

/**
 * Класс для создания и управления буферами OpenGL.
 */
public abstract class Buffer {

    protected final int id = glGenBuffers();
    protected int binding;
    protected final int bufferType;

    /**
     * Конструктор для создания буфера OpenGL.
     *
     * @param bufferType Тип буфера (GL_ARRAY_BUFFER, GL_UNIFORM_BUFFER, и т.д.).
     * @param binding    Биндинг (slot) для связывания с шейдером. Используется в layout(binding = <Ваш binding>) в шейдере.
     */
    public Buffer(int bufferType, int binding) {
        this.binding = binding;
        this.bufferType = bufferType;
    }

    /**
     * Устанавливает новый биндинг (slot) для связывания с шейдером.
     * <p>
     * Не рекомендуется, так как при изменении биндинга шейдер всё ещё будет думать, что он привязан к старому биндингу
     *  и будет читать рандомные данные из памяти видеокарты (плохо).
     * @param binding Новый биндинг (slot).
     */
    protected void setShaderBinding(int binding) {
        this.binding = binding;
        bind();
        glBindBufferBase(bufferType, binding, id);
        unbind();
    }

    /**
     * Привязывает буфер к текущему контексту OpenGL.
     */
    protected void bind() {
        glBindBuffer(bufferType, id);
    }

    /**
     * Отвязывает буфер от текущего контекста OpenGL.
     */
    protected void unbind() {
        glBindBuffer(bufferType, 0);
    }

    /**
     * Возвращает идентификатор буфера.
     * @return Идентификатор буфера.
     */
    public int getId() {
        return id;
    }

    /**
     * Возвращает текущий биндинг (slot) буфера.
     * @return Текущий биндинг (slot) буфера.
     */
    public int getBinding() {
        return binding;
    }
}
