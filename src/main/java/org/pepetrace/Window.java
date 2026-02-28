package org.pepetrace;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {

    private static boolean glfw_initialized = false;
    public long id;

    public Window() {
        initGLFW();
        glfwDefaultWindowHints(); // Загружаем настройки окна GLFW по умолчания
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE); // Делаем окно видимым
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // Делает окно меняемым по размеру

        this.id = glfwCreateWindow(1600, 900, "PepeTrace", NULL, NULL);
        if (this.id == NULL) throw new RuntimeException("Failed to create window");
    }

    public Window(int width, int height, boolean resizable, String title) {
        initGLFW();
        glfwDefaultWindowHints(); // Загружаем настройки окна GLFW по умолчания
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE); // Делаем окно видимым
        glfwWindowHint(GLFW_RESIZABLE, resizable ? GLFW_TRUE : GLFW_FALSE); // Делает окно меняемым по размеру

        this.id = glfwCreateWindow(width, height, title, NULL, NULL);
        if (this.id == NULL) throw new RuntimeException("Failed to create window");
    }

    // Инициализирует библиотеку GLFW
    private void initGLFW() {
        if (glfw_initialized) return;

        GLFWErrorCallback errorCallback;
        glfwSetErrorCallback(
                errorCallback = GLFWErrorCallback.createPrint(System.err)
        );

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfw_initialized = true;
    }

    // Делает данное окно "основным"
    // команды GLFW и GL будут работать по отношению к данному
    public void setActive() {
        glfwMakeContextCurrent(this.id); // некоторым функциям (таким как glfwSwapInterval) необходимо знать, к какому окну (контексту) мы обращаемся, иначе ошибка NO_CURRENT_CONTEXT
        GL.createCapabilities(); // Даём lwjgl понять что мы хотим использовать данный контекст для отрисовки
        glfwSwapInterval(1); // Vertical Sync
        glfwShowWindow(this.id); // Показывает окно на экране
    }
}
