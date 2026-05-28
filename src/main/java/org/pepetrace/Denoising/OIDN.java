package org.pepetrace.Denoising;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

public interface OIDN extends Library {
    int OIDN_DEVICE_TYPE_DEFAULT = 0;
    int OIDN_DEVICE_TYPE_CPU = 1;
    int OIDN_DEVICE_TYPE_SYCL = 2;
    int OIDN_DEVICE_TYPE_CUDA = 3;
    int OIDN_DEVICE_TYPE_HIP = 4;
    int OIDN_DEVICE_TYPE_METAL = 5;

    int OIDN_ERROR_NONE = 0;
    int OIDN_ERROR_UNKNOWN = 1;
    int OIDN_ERROR_INVALID_ARGUMENT = 2;
    int OIDN_ERROR_INVALID_OPERATION = 3;
    int OIDN_ERROR_OUT_OF_MEMORY = 4;
    int OIDN_ERROR_UNSUPPORTED_HARDWARE = 5;
    int OIDN_ERROR_CANCELLED = 6;

    int OIDN_FORMAT_UNDEFINED = 0;
    int OIDN_FORMAT_FLOAT = 1;
    int OIDN_FORMAT_FLOAT2 = 2;
    int OIDN_FORMAT_FLOAT3 = 3;
    int OIDN_FORMAT_FLOAT4 = 4;

    Pointer oidnNewDevice(int type);
    void oidnRetainDevice(Pointer device);
    void oidnReleaseDevice(Pointer device);
    void oidnSetDeviceBool(Pointer device, String name, boolean value);
    void oidnSetDeviceInt(Pointer device, String name, int value);
    void oidnCommitDevice(Pointer device);
    int oidnGetDeviceError(Pointer device, PointerByReference outMessage);

    Pointer oidnNewFilter(Pointer device, String type);
    void oidnRetainFilter(Pointer filter);
    void oidnReleaseFilter(Pointer filter);
    void oidnSetFilterBool(Pointer filter, String name, boolean value);
    void oidnSetFilterInt(Pointer filter, String name, int value);
    void oidnSetFilterFloat(Pointer filter, String name, float value);
    void oidnSetFilterImage(
        Pointer filter,
        String name,
        Pointer buffer,
        int format,
        long width,
        long height,
        long byteOffset,
        long bytePixelStride,
        long byteRowStride
    );
    void oidnSetSharedFilterImage(
        Pointer filter,
        String name,
        Pointer data,
        int format,
        long width,
        long height,
        long byteOffset,
        long bytePixelStride,
        long byteRowStride
    );
    void oidnCommitFilter(Pointer filter);
    void oidnExecuteFilter(Pointer filter);

    Pointer oidnNewBuffer(Pointer device, long byteSize);
    void oidnWriteBuffer(Pointer buffer, long byteOffset, long byteSize, Pointer data);
    void oidnReadBuffer(Pointer buffer, long byteOffset, long byteSize, Pointer data);
    void oidnRetainBuffer(Pointer buffer);
    void oidnReleaseBuffer(Pointer buffer);

    static OIDN get() {
        return InstanceHolder.INSTANCE;
    }

    class InstanceHolder {

        static {
            OIDNLoader.load();
        }

        static final OIDN INSTANCE = Native.load(
            "OpenImageDenoise",
            OIDN.class
        );
    }
}
