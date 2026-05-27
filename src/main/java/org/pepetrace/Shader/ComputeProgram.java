package org.pepetrace.Shader;

import static org.lwjgl.opengl.GL46.*;

import java.io.FileNotFoundException;
import java.util.Map;

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
        this(filepath, null);
    }

    /**
     * Создает программу-компьютерный шейдер (compute shader) из файла
     * с дополнительными #define директивами.
     *
     * @param filepath Путь к файлу с кодом шейдера.
     * @param defines  Карта имен define-директив к их значениям (может быть null).
     * @throws FileNotFoundException Если файл не найден.
     */
    public ComputeProgram(String filepath, Map<String, String> defines) throws FileNotFoundException {
        ShaderSourceReader sourceReader = new ShaderSourceReader();

        CharSequence shader_source = sourceReader.readFile(
            filepath + ".comp",
            false
        );

        // Inject #defines after the first line (#version)
        StringBuilder fullSource = new StringBuilder();
        String source = shader_source.toString();
        String[] lines = source.split("\n", 2);
        fullSource.append(lines[0]).append('\n');
        if (defines != null) {
            for (Map.Entry<String, String> entry : defines.entrySet()) {
                fullSource.append("#define ").append(entry.getKey()).append(' ').append(entry.getValue()).append('\n');
            }
        }
        fullSource.append(lines.length > 1 ? lines[1] : "");

        int compute = glCreateShader(GL_COMPUTE_SHADER);
        glShaderSource(compute, fullSource);
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
