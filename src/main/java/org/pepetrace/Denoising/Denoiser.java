package org.pepetrace.Denoising;

public interface Denoiser extends AutoCloseable {

    void denoise(int colorTexId, int outputTexId, int width, int height, int sampleCount);

    void denoise(
        int colorTexId, int albedoTexId, int normalTexId,
        int outputTexId, int width, int height, int sampleCount
    );
}
