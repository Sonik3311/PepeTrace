package org.pepetrace.Buffers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.lwjgl.opengl.GL46.*;

public abstract class UBO extends Buffer {
    protected ByteBuffer buffer;
    protected int sizeBytes;

    public UBO(int sizeBytes, int binding) {
        super(GL_UNIFORM_BUFFER, binding);
        this.sizeBytes = sizeBytes;
        buffer = ByteBuffer.allocateDirect(sizeBytes)
                .order(ByteOrder.nativeOrder());
    }

    public abstract void updateBuffer();

    protected void putFloat(int offsetBytes, float value) {
        buffer.putFloat(offsetBytes, value);
    }

    protected void putInt(int offsetBytes, int value) {
        buffer.putInt(offsetBytes, value);
    }

    protected void putBoolean(int offsetBytes, boolean value) {
        buffer.putInt(offsetBytes, value ? 1 : 0);
    }

    protected void putVec2(int offsetBytes, float x, float y) {
        buffer.putFloat(offsetBytes, x);
        buffer.putFloat(offsetBytes + 4, y);
    }

    protected void putVec3(int offsetBytes, float x, float y, float z) {
        buffer.putFloat(offsetBytes, x);
        buffer.putFloat(offsetBytes + 4, y);
        buffer.putFloat(offsetBytes + 8, z);
    }

    protected void putVec4(int offsetBytes, float x, float y, float z, float w) {
        buffer.putFloat(offsetBytes, x);
        buffer.putFloat(offsetBytes + 4, y);
        buffer.putFloat(offsetBytes + 8, z);
        buffer.putFloat(offsetBytes + 12, w);
    }

    protected void uploadToGPU() {
        bind();
        glBufferSubData(GL_UNIFORM_BUFFER, 0, buffer);
        unbind();
    }
}
