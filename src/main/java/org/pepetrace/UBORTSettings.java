package org.pepetrace;

import org.pepetrace.Buffers.UBO;

public class UBORTSettings extends UBO {

    public UBORTSettings(int binding) {
        super(16, binding);
    }

    public void updateBuffer(
        int frameId,
        int samplesPerFrame,
        int maxBounces,
        int triangleCount
    ) {
        buffer.clear();
        putInt(0, frameId);
        putInt(4, samplesPerFrame);
        putInt(8, maxBounces);
        putInt(12, triangleCount);
        uploadToGPU();
    }
}
