package org.pepetrace;

import static org.lwjgl.opengl.GL46.*;

public class FloatSSBO {
    final private int id;
    private int binding;
    private int length;
    private int size;

    public FloatSSBO(int binding) {
        id = glGenBuffers();
        System.out.println(id);
        this.binding = binding;
    }

    public void fillBuffer(float[] data) {
        this.length = data.length;
        this.size = data.length * 4; //4 Байт на float
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, id);
        glBufferData(GL_SHADER_STORAGE_BUFFER, data, GL_STATIC_DRAW);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, this.binding, id);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    public int getId() {
        return id;
    }

    public int getLength() {
        return length;
    }

    public int getSize() {
        return size;
    }
}
