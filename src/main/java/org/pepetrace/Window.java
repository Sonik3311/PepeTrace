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
        glfwDefaultWindowHints(); // Loads GLFW's default window settings
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE); // Sets window to be visible
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // Sets whether the window is resizable

        this.id = glfwCreateWindow(1600, 900, "PepeTrace", NULL, NULL); // Does the actual window creation
        if (this.id == NULL) throw new RuntimeException("Failed to create window");
        setActive();
    }

    public Window(int width, int height, boolean resizable, String title) {
        initGLFW();
        glfwDefaultWindowHints(); // Loads GLFW's default window settings
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE); // Sets window to be visible
        glfwWindowHint(GLFW_RESIZABLE, resizable ? GLFW_TRUE : GLFW_FALSE); // Sets whether the window is resizable

        this.id = glfwCreateWindow(width, height, title, NULL, NULL); // Does the actual window creation
        if (this.id == NULL) throw new RuntimeException("Failed to create window");
        setActive();
    }

    private void initGLFW() {
        GLFWErrorCallback errorCallback;
        glfwSetErrorCallback(
                errorCallback = GLFWErrorCallback.createPrint(System.err)
        );

        if (glfw_initialized) return;

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfw_initialized = true;

    }

    // Делает данное окно "основным"
    // комманды GLFW и GL будут работать по отношению к данному
    private void setActive() {
        glfwMakeContextCurrent(this.id); // glfwSwapInterval needs a context on the calling thread, otherwise will cause NO_CURRENT_CONTEXT error
        GL.createCapabilities(); // Will let lwjgl know we want to use this context as the context to draw with
        glfwSwapInterval(1); // How many draws to swap the buffer
        glfwShowWindow(this.id); // Shows the window
    }
}
