package org.pepetrace.Shader;

import java.io.FileNotFoundException;

import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.system.MemoryUtil.NULL;


public class ComputeProgram extends Program {
    // TODO: Использовать вместо типа String для filepath что-то иное?
    //  Вдруг при разных типах упаковки (.jar, .class, ...) пути поломаются?

    public ComputeProgram(String filepath) throws FileNotFoundException {
        CharSequence shader_source = readFile(filepath + ".comp");

        int compute = glCreateShader(GL_COMPUTE_SHADER);
        glShaderSource(compute, shader_source);
        glCompileShader(compute);
        checkCompilationStatus(compute);

        id = glCreateProgram();
        glAttachShader(id, compute);
        glLinkProgram(id);
        checkLinkStatus(id);
    }

    static public int[] getMaxWorkGroupCount() {
        int[] workGroupCount = {-1, -1, -1};

        glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0, workGroupCount);
        glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 1, workGroupCount);
        glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 2, workGroupCount);
        return workGroupCount;
    }

    @Override
    public void setInt(final String name, int value) {
        System.err.println("ComputeProgram не поддерживает uniform-ы");
    }

    @Override
    public void setBool(final String name, boolean value) {
        System.err.println("ComputeProgram не поддерживает uniform-ы");
    }

    @Override
    public void setFloat(final String name, float value) {
        System.err.println("ComputeProgram не поддерживает uniform-ы");
    }
}
