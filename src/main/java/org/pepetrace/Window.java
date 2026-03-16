package org.pepetrace;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

public class Window {

    private static boolean glfw_initialized = false;
    private long id;
    private int width;
    private int height;
    private double lastMouseX, lastMouseY;
    private boolean firstMouse = true;

    public static final int CURSOR_NORMAL = GLFW_CURSOR_NORMAL;
    public static final int CURSOR_DISABLED = GLFW_CURSOR_DISABLED;

    public Window() {
        initGLFW();
        glfwDefaultWindowHints(); // Загружаем настройки окна GLFW по умолчания
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4); // OpenGL 4.6
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6); // OpenGL 4.6
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE); // Делаем окно видимым
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // Делает окно меняемым по размеру

        // Первая попытка ИГНОРИТЬ скейлинг системы (DPI)
        glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_FALSE);

        this.id = glfwCreateWindow(1600, 900, "PepeTrace", NULL, NULL);
        this.width = 1600;
        this.height = 900;
        if (this.id == NULL) throw new RuntimeException(
            "Failed to create window"
        );

        //Вторая попытка
        float[] xscale = {0};
        float[] yscale = {0};
        glfwGetWindowContentScale(id, xscale, yscale);
        glfwSetWindowSize(id,
                (int) (width / xscale[0]),
                (int) (height / yscale[0]));

    }

    public Window(int width, int height, boolean resizable, String title) {
        initGLFW();
        glfwDefaultWindowHints(); // Загружаем настройки окна GLFW по умолчания
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4); // OpenGL 4.6
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6); // OpenGL 4.6
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE); // Делаем окно видимым
        glfwWindowHint(GLFW_RESIZABLE, resizable ? GLFW_TRUE : GLFW_FALSE); // Делает окно меняемым по размеру

        this.id = glfwCreateWindow(width, height, title, NULL, NULL);
        this.width = width;
        this.height = height;
        if (this.id == NULL) throw new RuntimeException(
            "Failed to create window"
        );
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

    public void resetMouse() {
        double[] xpos = new double[1];
        double[] ypos = new double[1];
        glfwGetCursorPos(id, xpos, ypos);
        lastMouseX = xpos[0];
        lastMouseY = ypos[0];
        firstMouse = false; // гарантируем, что следующий getMouseDelta не будет сбрасывать
    }

    public float[] getMouseDelta() {
        double[] xpos = new double[1];
        double[] ypos = new double[1];
        glfwGetCursorPos(id, xpos, ypos);

        float dx = 0, dy = 0;

        if (firstMouse) {
            lastMouseX = xpos[0];
            lastMouseY = ypos[0];
            firstMouse = false;
        } else {
            dx = (float)(lastMouseX - xpos[0]);
            dy = (float)(lastMouseY - ypos[0]); // Инвертируем Y для OpenGL
            lastMouseX = xpos[0];
            lastMouseY = ypos[0];
        }

        return new float[]{dx, dy};
    }

    public boolean isKeyPressed(int key) {
        return glfwGetKey(id, key) == GLFW_PRESS;
    }

    public void setCursorMode(int mode) {
        glfwSetInputMode(id, GLFW_CURSOR, mode);
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public long getId() {
        return this.id;
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(id);
    }
}
