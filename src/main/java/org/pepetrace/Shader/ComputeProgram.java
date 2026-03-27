package org.pepetrace.Shader;

import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.io.FileNotFoundException;

/**
 * Класс для создания и управления программой-компьютерным шейдером (compute shader).
 */
public class ComputeProgram extends Program {

    // TODO: Использовать вместо типа String для filepath что-то иное?
    //  Вдруг при разных типах упаковки (.jar, .class, ...) пути поломаются?

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

        //System.out.println(shader_source);

        int compute = glCreateShader(GL_COMPUTE_SHADER);
        glShaderSource(compute, shader_source);
        glCompileShader(compute);
        checkCompilationStatus(compute);

        id = glCreateProgram();
        glAttachShader(id, compute);
        glLinkProgram(id);
        checkLinkStatus(id);
    }

    /**
     * Возвращает максимальный размер рабочей группы для compute шейдера.
     *
     * @return Массив из трех элементов: максимальный размер рабочей группы ГП по оси X, Y и Z.
     */
    public static int[] getMaxWorkGroupCount() {
        int[] workGroupCount = { -1, -1, -1 };

        glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0, workGroupCount);
        glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 1, workGroupCount);
        glGetIntegeri_v(GL_MAX_COMPUTE_WORK_GROUP_COUNT, 2, workGroupCount);
        return workGroupCount;
    }

    /**
     * Устанавливает целочисленное значение для uniform-переменной.
     * <p>
     * !!! НЕ РАБОТАЕТ В Compute ШЕЙДЕРАХ! !!!
     * <p>
     * * Исходит из документации OpenGL. Используй буфферы SSBO и UBO.
     *
     * @param name  Имя uniform-переменной.
     * @param value Значение для установки.
     */
    @Override
    public void setInt(final String name, int value) {
        System.err.println("ComputeProgram не поддерживает uniform-ы");
    }

    /**
     * Устанавливает булевое значение для uniform-переменной.
     * <p>
     * !!! НЕ РАБОТАЕТ В Compute ШЕЙДЕРАХ! !!!
     * <p>
     * * Исходит из документации OpenGL. Используй буфферы SSBO и UBO.
     *
     * @param name  Имя uniform-переменной.
     * @param value Значение для установки.
     */
    @Override
    public void setBool(final String name, boolean value) {
        System.err.println("ComputeProgram не поддерживает uniform-ы");
    }

    /**
     * Устанавливает значение типа float для uniform-переменной.
     * <p>
     * !!! НЕ РАБОТАЕТ В Compute ШЕЙДЕРАХ! !!!
     * <p>
     * * Исходит из документации OpenGL. Используй буфферы SSBO и UBO.
     *
     * @param name  Имя uniform-переменной.
     * @param value Значение для установки.
     */
    @Override
    public void setFloat(final String name, float value) {
        System.err.println("ComputeProgram не поддерживает uniform-ы");
    }
}
