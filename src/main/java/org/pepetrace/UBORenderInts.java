package org.pepetrace;

import org.pepetrace.Buffers.UBO;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.lwjgl.opengl.ARBUniformBufferObject.glGetActiveUniformBlockiv;
import static org.lwjgl.opengl.GL46.*;

public class UBORenderInts extends UBO {

    public UBORenderInts(int binding) {
        super(4, binding);
    }

    public void updateBuffer(int tick) {
        buffer.clear();
        putInt(0, tick);
        uploadToGPU();
    }
}
