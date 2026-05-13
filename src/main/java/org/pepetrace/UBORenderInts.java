package org.pepetrace;

import org.pepetrace.Buffers.UBO;
import org.pepetrace.Util.ViewportRenderMode;

public class UBORenderInts extends UBO {

    public UBORenderInts(int binding) {
        super(24, binding); // 20 + 4 = 24 байта
    }

    public void updateBuffer(int tick, int samples, int reflections, boolean doAO,
                             ViewportRenderMode renderMode, int triangleCount) {
        buffer.clear();
        putInt(0, tick);
        putInt(4, samples);
        putInt(8, reflections);
        putBoolean(12, doAO);
        putInt(16, renderMode.ordinal());
        putInt(20, triangleCount); // новое поле
        uploadToGPU();
    }
}