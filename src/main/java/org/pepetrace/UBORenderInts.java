package org.pepetrace;

import org.pepetrace.Buffers.UBO;
import org.pepetrace.Util.ViewportRenderMode;

public class UBORenderInts extends UBO {

    public UBORenderInts(int binding) {
        super(20, binding);
    }

    public void updateBuffer(int tick, int samples, int reflections, boolean doAO, ViewportRenderMode renderMode) {
        buffer.clear();
        putInt(0, tick);
        putInt(4, samples);
        putInt(8, reflections);
        putBoolean(12, doAO);
        putInt(16, renderMode.ordinal());
        uploadToGPU();
    }
}
