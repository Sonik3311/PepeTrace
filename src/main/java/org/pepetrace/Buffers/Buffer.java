package org.pepetrace.Buffers;

import static org.lwjgl.opengl.GL46.*;

public abstract class Buffer {
    final protected int id = glGenBuffers();
    protected int binding;
    final protected int bufferType;

    public Buffer(int bufferType, int binding) {
        this.binding = binding;
        this.bufferType = bufferType;
    }

    protected void setShaderBinding(int binding) {
        this.binding = binding;
        bind();
        glBindBufferBase(bufferType, binding, id);
        unbind();
    };

    protected void bind() {
        glBindBuffer(bufferType, id);
    }
    protected void unbind() {
        glBindBuffer(bufferType, 0);
    }

    public int getId() {
        return id;
    }

    public int getBinding() {
        return binding;
    }
}
