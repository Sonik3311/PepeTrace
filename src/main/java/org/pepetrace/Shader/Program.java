package org.pepetrace.Shader;

import static org.lwjgl.opengl.GL46.*;

import java.io.FileNotFoundException;

/**
 * Класс для создания и управления программой-шейдером.
 */
public class Program {

    public int id;

    // TODO: Использовать вместо типа String для filepath что-то иное?
    //  Вдруг при разных типах упаковки (.jar, .class, ...) пути поломаются?
    /**
     * Создаёт программу OpenGL из файлов-шейдеров <filepath>.vert и <filepath>.frag.
     *
     * @param filepath путь к шейдеру. Обязан НЕ иметь расширение файла (например, ".vert" или ".frag")
     * Пример: "shaders/myShader", при этом файлы-шейдеры должны быть "shaders/myShader.vert" и "shaders/myShader.frag"
     */
    public Program(String filepath) throws FileNotFoundException {
        id = glCreateProgram();

        ShaderSourceReader sourceReader = new ShaderSourceReader();

        CharSequence frag_source = sourceReader.readFile(
            filepath + ".frag",
            false
        );
        CharSequence vert_source = sourceReader.readFile(
            filepath + ".vert",
            false
        );
        //System.out.println(frag_source + "\n" + vert_source);

        int vertex, fragment;

        vertex = glCreateShader(GL_VERTEX_SHADER);
        fragment = glCreateShader(GL_FRAGMENT_SHADER);

        glShaderSource(vertex, vert_source);
        glShaderSource(fragment, frag_source);

        glCompileShader(vertex);
        glCompileShader(fragment);

        checkCompilationStatus(vertex);
        checkCompilationStatus(fragment);

        glAttachShader(id, vertex);
        glAttachShader(id, fragment);
        glLinkProgram(id);
        checkLinkStatus(id);

        // Шейдеры уже скопированы в program, так что удаляем
        glDeleteShader(vertex);
        glDeleteShader(fragment);
    }

    /**
     * Если ты это используешь, то ты идёшь по неправильному пути. Наверное.
     * Пустая затычка чтобы линтер не ругался.
     */
    public Program() {}

    /**
     * Возвращает строку, описывающую тип шейдера по его ID.
     *
     * @param shaderId ID шейдера
     * @return строка, описывающая тип шейдера
     */
    public static String getShaderTypeString(int shaderId) {
        // 1. Получаем тип шейдера (как int)
        int type = glGetShaderi(shaderId, GL_SHADER_TYPE);

        // 2. Преобразуем в текст
        return switch (type) {
            case GL_VERTEX_SHADER -> "Vertex Shader";
            case GL_FRAGMENT_SHADER -> "Fragment Shader";
            case GL_GEOMETRY_SHADER -> "Geometry Shader";
            case GL_COMPUTE_SHADER -> "Compute Shader";
            default -> "Unknown (" + type + ")";
        };
    }

    /**
     * Проверяет статус компиляции шейдера и выводит сообщение об ошибке, если компиляция не удалась.
     *
     * @param shader ID шейдера
     */
    protected void checkCompilationStatus(int shader) {
        int success = glGetShaderi(shader, GL_COMPILE_STATUS);

        if (success != GL_TRUE) {
            // compile failure
            String infoLog = glGetShaderInfoLog(shader, 512);
            System.err.println(
                "\u001B[31m[" +
                    getShaderTypeString(shader) +
                    "] GLSL COMPILE ERROR: " +
                    infoLog +
                    "\u001B[0m"
            );
        }
    }

    /**
     * Проверяет статус линковки программы и выводит сообщение об ошибке, если линковка не удалась.
     * Линковка - процесс после компиляции шейдеров, связывающих их в единую программу, готовую к запуске на ГП.
     *
     * @param program ID программы
     */
    protected void checkLinkStatus(int program) {
        int success = glGetProgrami(program, GL_LINK_STATUS);

        if (success != GL_TRUE) {
            // linking failure
            String infoLog = glGetProgramInfoLog(program, 512);
            System.err.println(
                "\u001B[31m[Program] GLSL LINKING ERROR: " +
                    infoLog +
                    "\u001B[0m"
            );
        }
    }

    /**
     * Говорит OpenGL использовать данную программу для рендеринга.
     */
    public void use() {
        glUseProgram(id);
    }

    /**
     * Устанавливает целочисленное значение для униформной переменной (uniform).
     *
     * @param name  Имя униформной переменной. Должно совпадать с тем, что прописано в шейдере.
     * @param value Значение
     */
    public void setInt(final String name, int value) {
        glUniform1i(glGetUniformLocation(id, name), value);
    }

    /**
     * Устанавливает булево значение для униформной переменной (uniform).
     *
     * @param name  Имя униформной переменной. Должно совпадать с тем, что прописано в шейдере.
     * @param value Значение
     */
    public void setBool(final String name, boolean value) {
        glUniform1i(glGetUniformLocation(id, name), value ? 1 : 0);
    }

    /**
     * Устанавливает значение типа float для униформной переменной (uniform).
     *
     * @param name  Имя униформной переменной. Должно совпадать с тем, что прописано в шейдере.
     * @param value Значение
     */
    public void setFloat(final String name, float value) {
        glUniform1f(glGetUniformLocation(id, name), value);
    }
}
