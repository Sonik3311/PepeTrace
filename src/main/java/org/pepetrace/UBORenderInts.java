package org.pepetrace;

import org.pepetrace.Buffers.UBO;
import org.pepetrace.Util.ViewportRenderMode;

public class UBORenderInts extends UBO {

    public UBORenderInts(int binding) {
        super(20, binding);
    }

    public void updateBuffer(int tick, int samples, int reflections, float roughness, ViewportRenderMode renderMode) {
        buffer.clear();
        putInt(0, tick);
        putInt(4, samples);
        putInt(8, reflections);
        putFloat(12, roughness);
        putInt(16, renderMode.ordinal());
        uploadToGPU();
    }
}
