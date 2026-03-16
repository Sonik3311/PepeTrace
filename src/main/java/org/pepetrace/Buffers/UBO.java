package org.pepetrace.Buffers;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL46.*;

public abstract class UBO extends Buffer {
    protected ByteBuffer buffer;
    protected int sizeBytes;

    public UBO(int sizeBytes, int binding) {
        super(GL_UNIFORM_BUFFER, binding);
        this.sizeBytes = sizeBytes;
        buffer = ByteBuffer.allocateDirect(sizeBytes).order(ByteOrder.nativeOrder());

        bind();
        glBufferData(GL_UNIFORM_BUFFER, sizeBytes, GL_DYNAMIC_DRAW);
        unbind();
        setShaderBinding(binding);

    }

    protected void putFloat(int offsetBytes, float value) {
        buffer.putFloat(offsetBytes, value);
    }

    protected void putInt(int offsetBytes, int value) {
        buffer.putInt(offsetBytes, value);
    }

    protected void putBoolean(int offsetBytes, boolean value) {
        buffer.putInt(offsetBytes, value ? 1 : 0);
    }

    protected void putVec2(int offsetBytes, Vector2f vec2) {
        buffer.putFloat(offsetBytes, vec2.x);
        buffer.putFloat(offsetBytes + 4, vec2.y);
    }

    protected void putVec3(int offsetBytes, Vector3f vec3) {
        buffer.putFloat(offsetBytes, vec3.x);
        buffer.putFloat(offsetBytes + 4, vec3.y);
        buffer.putFloat(offsetBytes + 8, vec3.z);
    }

    protected void putVec4(int offsetBytes, Vector4f vec4) {
        buffer.putFloat(offsetBytes, vec4.x);
        buffer.putFloat(offsetBytes + 4, vec4.y);
        buffer.putFloat(offsetBytes + 8, vec4.z);
        buffer.putFloat(offsetBytes + 12, vec4.w);
    }

    protected void uploadToGPU() {
        bind();
        glBufferSubData(GL_UNIFORM_BUFFER, 0, buffer);
        unbind();
    }

    public int getSizeBytes() {
        return sizeBytes;
    }
}
