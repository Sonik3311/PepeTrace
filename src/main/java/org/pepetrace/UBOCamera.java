package org.pepetrace;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.pepetrace.Buffers.UBO;

public class UBOCamera extends UBO {
    public UBOCamera(int binding) {
        // --vec3 position-- (offset = 0)
        // float            | 4 Bytes
        // float            | 4 Bytes
        // float            | 4 Bytes
        // std140 alignemnt | 4 Bytes
        // --vec2 yawPitch-- (offset = 16)
        // float            | 4 Bytes
        // float            | 4 Bytes
        super(24, binding);
    }

    public void updateBuffer(Vector3f position, Vector2f yawPitch) {
        buffer.clear();
        putVec3(0, position);
        putVec2(16, yawPitch);
        uploadToGPU();
    }
}
