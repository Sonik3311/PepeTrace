package org.pepetrace.Shader;

import static org.lwjgl.opengl.GL46.*;

import java.io.FileNotFoundException;

/**
 * Класс для создания и управления программой-компьютерным шейдером (compute shader).
 */
public class ComputeProgram extends Program {

    /**
     * Создает программу-компьютерный шейдер (compute shader) из файла.
     *
     * @param filepath Путь к файлу с кодом шейдера.
     * @throws FileNotFoundException Если файл не найден.
     */
    public ComputeProgram(String filepath) throws FileNotFoundException {
        ShaderSourceReader sourceReader = new ShaderSourceReader();

        CharSequence shader_source = sourceReader.readFile(
            filepath + ".comp",
            false
        );

        int compute = glCreateShader(GL_COMPUTE_SHADER);
        glShaderSource(compute, shader_source);
        glCompileShader(compute);
        checkCompilationStatus(compute);

        id = glCreateProgram();
        glAttachShader(id, compute);
        glLinkProgram(id);
        checkLinkStatus(id);
        glDeleteShader(compute);
    }

    /**
     * Возвращает максимальный размер рабочей группы для compute шейдера.
     *
     * @return Массив из трех элементов: максимальный размер рабочей группы ГП по оси X, Y и Z.
     */
    public static int[] getMaxWorkGroupCount() {
        int[] workGroupCount = {-1, -1, -1};

        glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0, workGroupCount);
        glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 1, workGroupCount);
        glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 2, workGroupCount);
        return workGroupCount;
    }
}
