package org.pepetrace.Denoising;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import org.pepetrace.Denoising.Denoiser;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glReadPixels;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexSubImage2D;
import static org.lwjgl.opengl.GL15.GL_STREAM_DRAW;
import static org.lwjgl.opengl.GL15.GL_STREAM_READ;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL15.glMapBuffer;
import static org.lwjgl.opengl.GL15.glUnmapBuffer;
import static org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgramiv;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderiv;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.GL_HALF_FLOAT;
import static org.lwjgl.opengl.GL30.GL_READ_ONLY;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import static org.lwjgl.opengl.GL30.GL_WRITE_ONLY;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL42.GL_BUFFER_UPDATE_BARRIER_BIT;
import static org.lwjgl.opengl.GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
import static org.lwjgl.opengl.GL42.GL_TEXTURE_UPDATE_BARRIER_BIT;
import static org.lwjgl.opengl.GL42.glBindImageTexture;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BARRIER_BIT;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL43.glDispatchCompute;

public class OIDNDenoiser implements Denoiser {

    private final Pointer device;
    private final OIDN oidn;
    private static final String copyShaderSource =
        "#version 430\n" +
        "layout(local_size_x=16,local_size_y=16) in;\n" +
        "layout(rgba32f,binding=0) readonly uniform image2D src;\n" +
        "layout(rgba32f,binding=1) writeonly uniform image2D dst;\n" +
        "void main(){\n" +
        "  ivec2 c=ivec2(gl_GlobalInvocationID.xy);\n" +
        "  if(c.x>=imageSize(src).x||c.y>=imageSize(src).y)return;\n" +
        "  imageStore(dst,c,imageLoad(src,c));\n" +
        "}";

    public OIDNDenoiser() {
        this(OIDN.OIDN_DEVICE_TYPE_CPU);
    }

    public OIDNDenoiser(int deviceType) {
        this.oidn = OIDN.get();
        device = oidn.oidnNewDevice(deviceType);
        if (device == null) throw new RuntimeException("Failed to create OIDN device");
        oidn.oidnCommitDevice(device);
        checkError("oidnCommitDevice");
    }

    @Override
    public void denoise(int colorTexId, int outputTexId, int width, int height, int sampleCount) {
        denoise(colorTexId, 0, 0, outputTexId, width, height, sampleCount);
    }

    @Override
    public void denoise(
        int colorTexId, int albedoTexId, int normalTexId,
        int outputTexId, int width, int height, int sampleCount
    ) {
        int pixelCount = width * height;
        int byteSize = pixelCount * 4 * 4;
        int rgbByteSize = pixelCount * 3 * 4;
        // Clear any pending GL error from the render loop
        while (glGetError() != GL_NO_ERROR) {}
        ByteBuffer colorBuf = readTexture(colorTexId, width, height, byteSize);
        glFinish();

        // Clear any stale OIDN errors
        PointerByReference staleMsg = new PointerByReference();
        while (oidn.oidnGetDeviceError(device, staleMsg) != OIDN.OIDN_ERROR_NONE) {}

        // Sanitize input: clamp NaN/Inf to 0, restore alpha to sample count
        FloatBuffer floatBuf = colorBuf.rewind().asFloatBuffer();
        for (int i = 0; i < pixelCount * 4; i++) {
            float v = floatBuf.get(i);
            if (Float.isNaN(v) || Float.isInfinite(v)) floatBuf.put(i, 0.0f);
        }
        float sampleCountF = (float) sampleCount;
        for (int i = 0; i < pixelCount; i++) floatBuf.put(i * 4 + 3, sampleCountF);

        // Build packed RGB buffer for OIDN with native byte order (LITTLE_ENDIAN on x86)
        ByteBuffer colorRgbOnly = ByteBuffer.allocateDirect(rgbByteSize).order(ByteOrder.nativeOrder());
        FloatBuffer rgbFb = colorRgbOnly.asFloatBuffer();
        for (int i = 0; i < pixelCount; i++) {
            rgbFb.put(i * 3 + 0, floatBuf.get(i * 4 + 0));
            rgbFb.put(i * 3 + 1, floatBuf.get(i * 4 + 1));
            rgbFb.put(i * 3 + 2, floatBuf.get(i * 4 + 2));
        }

        // OIDN output buffer — also native byte order
        ByteBuffer outRgbOnly = ByteBuffer.allocateDirect(rgbByteSize).order(ByteOrder.nativeOrder());
        ByteBuffer writeBuf = ByteBuffer.allocateDirect(byteSize).order(ByteOrder.nativeOrder());

        // Read auxiliary channels (albedo, normal) if provided
        ByteBuffer albedoRgbOnly = null, normalRgbOnly = null;
        if (albedoTexId != 0) {
            albedoRgbOnly = readAuxFloat3(albedoTexId, width, height, pixelCount, rgbByteSize, false);
        }
        if (normalTexId != 0) {
            normalRgbOnly = readAuxFloat3(normalTexId, width, height, pixelCount, rgbByteSize, true);
        }

        // Build per-pixel sample count buffer (uniform across image after accumulation)
        ByteBuffer sampleCountBuf = ByteBuffer.allocateDirect(pixelCount * 4).order(ByteOrder.nativeOrder());
        FloatBuffer sampleCountFb = sampleCountBuf.asFloatBuffer();
        for (int i = 0; i < pixelCount; i++) sampleCountFb.put(i, (float) sampleCount);

        // OIDN RT filter — use shared memory
        Pointer filter = oidn.oidnNewFilter(device, "RT");
        checkError("oidnNewFilter");
        if (filter == null) throw new RuntimeException("Failed to create OIDN filter");

        try {
            oidn.oidnSetSharedFilterImage(filter, "color",
                Native.getDirectBufferPointer(colorRgbOnly),
                OIDN.OIDN_FORMAT_FLOAT3, width, height, 0, 0, 0);
            if (albedoRgbOnly != null) {
                oidn.oidnSetSharedFilterImage(filter, "albedo",
                    Native.getDirectBufferPointer(albedoRgbOnly),
                    OIDN.OIDN_FORMAT_FLOAT3, width, height, 0, 0, 0);
            }
            if (normalRgbOnly != null) {
                oidn.oidnSetSharedFilterImage(filter, "normal",
                    Native.getDirectBufferPointer(normalRgbOnly),
                    OIDN.OIDN_FORMAT_FLOAT3, width, height, 0, 0, 0);
            }
            oidn.oidnSetSharedFilterImage(filter, "output",
                Native.getDirectBufferPointer(outRgbOnly),
                OIDN.OIDN_FORMAT_FLOAT3, width, height, 0, 0, 0);
            oidn.oidnSetSharedFilterImage(filter, "sampleCount",
                Native.getDirectBufferPointer(sampleCountBuf),
                OIDN.OIDN_FORMAT_FLOAT, width, height, 0, 0, 0);
            oidn.oidnSetFilterBool(filter, "hdr", true);
            oidn.oidnSetFilterBool(filter, "cleanAux", false);

            oidn.oidnCommitFilter(filter);
            oidn.oidnExecuteFilter(filter);
        } finally {
            oidn.oidnReleaseFilter(filter);
        }

        // Merge OIDN output (RGB) + alpha into native-order RGBA buffer
        FloatBuffer writeFb = writeBuf.asFloatBuffer();
        FloatBuffer outRgbFb = outRgbOnly.rewind().asFloatBuffer();
        for (int i = 0; i < pixelCount; i++) {
            writeFb.put(i * 4 + 0, outRgbFb.get(i * 3 + 0));
            writeFb.put(i * 4 + 1, outRgbFb.get(i * 3 + 1));
            writeFb.put(i * 4 + 2, outRgbFb.get(i * 3 + 2));
            writeFb.put(i * 4 + 3, sampleCountF);
        }

        // Write back: PBO upload → compute shader copy to immutable texture
        // (glTexSubImage2D and glCopyImageSubData both fail on immutable textures with this driver)
        while (glGetError() != GL_NO_ERROR) {}
        int pbo = glGenBuffers();
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pbo);
        glBufferData(GL_PIXEL_UNPACK_BUFFER, writeBuf, GL_STREAM_DRAW);
        int tempTex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tempTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, width, height, 0,
            GL_RGBA, GL_FLOAT, 0);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height,
            GL_RGBA, GL_FLOAT, 0);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

        // Bind textures for compute shader
        glBindImageTexture(0, tempTex, 0, false, 0, GL_READ_ONLY, GL_RGBA32F);
        glBindImageTexture(1, outputTexId, 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);

        // Simple copy compute shader
        int cs = glCreateShader(GL_COMPUTE_SHADER);
        glShaderSource(cs, copyShaderSource);
        glCompileShader(cs);
        int prog = glCreateProgram();
        glAttachShader(prog, cs);
        glLinkProgram(prog);
        glUseProgram(prog);
        int groupsX = (width + 15) / 16, groupsY = (height + 15) / 16;
        glDispatchCompute(groupsX, groupsY, 1);
        glMemoryBarrier(GL_TEXTURE_UPDATE_BARRIER_BIT | GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        glUseProgram(0);
        glDeleteProgram(prog);
        glDeleteShader(cs);

        glBindTexture(GL_TEXTURE_2D, 0);
        glDeleteTextures(tempTex);
        glDeleteBuffers(pbo);
        glFinish();
    }

    private ByteBuffer readTexture(int texId, int width, int height, int byteSize) {
        return readTextureFbo(texId, width, height, byteSize);
    }

    private ByteBuffer readTextureFbo(int texId, int width, int height, int byteSize) {
        glMemoryBarrier(GL_TEXTURE_UPDATE_BARRIER_BIT | GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        glFinish();

        // Unbind from image unit so FBO attachment is valid
        glBindImageTexture(0, 0, 0, false, 0, GL_READ_ONLY, GL_RGBA32F);

        int fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texId, 0);

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            glDeleteFramebuffers(fbo);
            throw new RuntimeException("Readback FBO not complete: " + Integer.toHexString(status));
        }

        ByteBuffer rawHalf = ByteBuffer.allocateDirect(byteSize).order(ByteOrder.nativeOrder());
        glReadPixels(0, 0, width, height, GL_RGBA, GL_HALF_FLOAT, rawHalf);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteFramebuffers(fbo);

        ByteBuffer result = ByteBuffer.allocateDirect(byteSize);
        FloatBuffer fBuf = result.asFloatBuffer();
        rawHalf.rewind();
        for (int i = 0; i < width * height * 4; i++) {
            fBuf.put(i, halfToFloat(rawHalf.getShort(i * 2)));
        }

        result.rewind();
        return result;
    }

    /** Read an accumulated auxiliary texture, divide by count, and pack as Float3 for OIDN.
     *  If {@code isNormal} is true, the data is un-biased from [0,1] to [-1,1] and renormalized. */
    private ByteBuffer readAuxFloat3(int texId, int width, int height,
                                     int pixelCount, int rgbByteSize, boolean isNormal) {
        ByteBuffer buf = readTexture(texId, width, height, pixelCount * 4 * 4);
        FloatBuffer fb = buf.rewind().asFloatBuffer();
        // Divide by alpha (accumulated sample count) to get the average
        for (int i = 0; i < pixelCount; i++) {
            float cnt = fb.get(i * 4 + 3);
            if (cnt > 1e-6f) {
                float r = fb.get(i * 4 + 0) / cnt;
                float g = fb.get(i * 4 + 1) / cnt;
                float b = fb.get(i * 4 + 2) / cnt;
                if (isNormal) {
                    // Unbias from [0,1] to [-1,1] and renormalize
                    r = r * 2.0f - 1.0f;
                    g = g * 2.0f - 1.0f;
                    b = b * 2.0f - 1.0f;
                    float len = (float) Math.sqrt(r * r + g * g + b * b);
                    if (len > 1e-6f) { r /= len; g /= len; b /= len; }
                }
                fb.put(i * 4 + 0, r);
                fb.put(i * 4 + 1, g);
                fb.put(i * 4 + 2, b);
            }
        }
        // Pack to Float3 with native byte order
        ByteBuffer out = ByteBuffer.allocateDirect(rgbByteSize).order(ByteOrder.nativeOrder());
        FloatBuffer outFb = out.asFloatBuffer();
        for (int i = 0; i < pixelCount; i++) {
            outFb.put(i * 3 + 0, fb.get(i * 4 + 0));
            outFb.put(i * 3 + 1, fb.get(i * 4 + 1));
            outFb.put(i * 3 + 2, fb.get(i * 4 + 2));
        }
        return out;
    }

    private static float halfToFloat(short hf) {
        int h = hf & 0xffff;
        int sign = (h >>> 15) & 1;
        int exp = (h >>> 10) & 0x1f;
        int mant = h & 0x3ff;
        int f;
        if (exp == 0) {
            if (mant == 0) f = sign << 31;
            else {
                int e = -1, m = mant;
                do { e++; m <<= 1; } while ((m & 0x400) == 0);
                f = (sign << 31) | ((127 - 15 - e) << 23) | ((m & 0x3ff) << 13);
            }
        } else if (exp == 31) {
            f = (sign << 31) | (0xff << 23) | (mant << 13);
        } else {
            f = (sign << 31) | ((exp - 15 + 127) << 23) | (mant << 13);
        }
        return Float.intBitsToFloat(f);
    }

    @Override
    public void close() {
        if (device != null) {
            oidn.oidnReleaseDevice(device);
        }
    }

    private void checkError(String stage) {
        PointerByReference msgPtr = new PointerByReference();
        int err = oidn.oidnGetDeviceError(device, msgPtr);
        if (err != OIDN.OIDN_ERROR_NONE) {
            String msg =
                msgPtr.getValue() != null
                    ? msgPtr.getValue().getString(0)
                    : "unknown";
            throw new RuntimeException(
                "OIDN error [" + stage + "] code=" + err + ": " + msg
            );
        }
    }
}
