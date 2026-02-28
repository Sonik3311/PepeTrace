package org.pepetrace.Shader;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import static org.lwjgl.opengl.GL46.*;

public class Program {
    public int id;


    // TODO: Использовать вместо типа String для filepath что-то иное?
    //  Вдруг при разных типах упаковки (.jar, .class, ...) пути поломаются?
    public Program(String filepath) throws FileNotFoundException {
        CharSequence frag_source = readFile(filepath + ".frag");
        CharSequence vert_source = readFile(filepath + ".vert");

        int vertex, fragment;

        vertex = glCreateShader(GL_VERTEX_SHADER);
        fragment = glCreateShader(GL_FRAGMENT_SHADER);

        glShaderSource(vertex, vert_source);
        glShaderSource(fragment, frag_source);

        glCompileShader(vertex);
        glCompileShader(fragment);

        checkCompilationStatus(vertex);
        checkCompilationStatus(fragment);

        id = glCreateProgram();
        glAttachShader(id, vertex);
        glAttachShader(id, fragment);
        glLinkProgram(id);
        checkLinkStatus(id);

        // Шейдеры уже скопированы в program, так что удаляем
        glDeleteShader(vertex);
        glDeleteShader(fragment);
    }


    // Затычка
    public Program() {
    }

    public String getShaderTypeString(int shaderId) {
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

    // TODO: Использовать StringBuilder вместо конкатенации в цикле?
    protected CharSequence readFile(String filepath) throws FileNotFoundException {
        CharSequence chars = "";
        File file = new File(filepath);
        Scanner scanner = new Scanner(file);

        chars = scanner.nextLine();
        while (scanner.hasNextLine()) {
            chars = chars + "\n" + scanner.nextLine();
        }

        return chars;
    }

    protected void checkCompilationStatus(int shader) {
        int success = glGetShaderi(shader, GL_COMPILE_STATUS);

        if (success != GL_TRUE) { // compile failure
            String infoLog = glGetShaderInfoLog(shader, 512);
            System.err.println("\u001B[31m[" + getShaderTypeString(shader) + "] GLSL COMPILE ERROR: " + infoLog + "\u001B[0m");
        }
    }

    protected void checkLinkStatus(int program) {
        int success = glGetProgrami(program, GL_LINK_STATUS);

        if (success != GL_TRUE) { // linking failure
            String infoLog = glGetProgramInfoLog(program, 512);
            System.err.println("\u001B[31m[Program] GLSL LINKING ERROR: " + infoLog + "\u001B[0m");
        }
    }

    public void use() {
        glUseProgram(id);
    }

    public void setInt(final String name, int value) {
        glUniform1i(glGetUniformLocation(id, name), value);
    }

    public void setBool(final String name, boolean value) {
        glUniform1i(glGetUniformLocation(id, name), value ? 1 : 0);
    }

    public void setFloat(final String name, float value) {
        glUniform1f(glGetUniformLocation(id, name), value);
    }
}
