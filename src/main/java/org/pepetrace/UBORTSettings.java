package org.pepetrace;

import org.pepetrace.Buffers.UBO;

public class UBORTSettings extends UBO {

    public UBORTSettings(int binding) {
        super(16, binding);
    }

    public void updateBuffer(
        int frame,
        int samples,
        int bounces,
        int triangleCount
    ) {
        buffer.clear();
        putInt(0, samples);
        putInt(4, bounces);
        putInt(8, triangleCount);
        putInt(12, frame);
        uploadToGPU();
    }
}
