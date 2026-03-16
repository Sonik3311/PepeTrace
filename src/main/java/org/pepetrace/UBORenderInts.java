package org.pepetrace;

import org.pepetrace.Buffers.UBO;

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
