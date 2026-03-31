package org.pepetrace;

import org.pepetrace.Buffers.UBO;

public class UBORenderInts extends UBO {

    public UBORenderInts(int binding) {
        super(16, binding);
    }

    public void updateBuffer(int tick, int samples, int reflections, float roughness) {
        buffer.clear();
        putInt(0, tick);
        putInt(4, samples);
        putInt(8, reflections);
        putFloat(12, roughness);
        uploadToGPU();
    }
}
