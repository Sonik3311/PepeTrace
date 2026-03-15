package org.pepetrace.Buffers;

import static org.lwjgl.opengl.GL46.*;

public class SSBO extends Buffer {

    protected int sizeBytes;
    protected int length;
    public int usage;

    public SSBO(int GL_DrawTarget, int binding) {
        super(GL_SHADER_STORAGE_BUFFER, binding);
        setShaderBinding(binding);
        this.usage = GL_DrawTarget;
    }

    public void fillBuffer(Object data) {
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
            case null, default -> throw new IllegalArgumentException("Unsupported type");
        }

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, this.binding, id);
        unbind();
    }

    public int getSizeBytes() {
        return sizeBytes;
    }

    public int getLength() {
        return length;
    }
}
