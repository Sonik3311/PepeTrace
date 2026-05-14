package org.pepetrace;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

public class Window implements AutoCloseable {

    private static boolean glfwInitialized = false;
    private long id;
    private int width;
    private int height;
    private double lastMouseX, lastMouseY;
    private boolean firstMouse = true;
    private double scrollY = 0.0;
    private ResizeListener resizeListener;
    private boolean isShown = false;

    public static final int CURSOR_NORMAL = GLFW_CURSOR_NORMAL;
    public static final int CURSOR_DISABLED = GLFW_CURSOR_DISABLED;
    public static final int MOUSE_BUTTON_LEFT = GLFW_MOUSE_BUTTON_LEFT;

    @Override
    public void close() throws Exception {
        glfwDestroyWindow(id);
    }

    public interface ResizeListener {
        void onResize(int newWidth, int newHeight, boolean isFromGlfw);
    }

    public void setResizeListener(ResizeListener listener) {
        this.resizeListener = listener;
    }

    public void addScrollDelta(double delta) {
        this.scrollY += delta;
    }

    public Window() {
        initGLFW();
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_FALSE);

        this.width = 1600;
        this.height = 900;
        this.id = glfwCreateWindow(width, height, "PepeTrace", NULL, NULL);
        if (this.id == NULL) throw new RuntimeException("Failed to create window");

        // Коллбек на изменение размера фреймбуфера
        glfwSetFramebufferSizeCallback(id, (window, w, h) -> {
            this.width = w;
            this.height = h;
            if (resizeListener != null) {
                resizeListener.onResize(w, h, true);
            }
        });

        glfwSetScrollCallback(id, (window, xoffset, yoffset) -> {
            scrollY += yoffset;
        });

        // Коррекция DPI
        float[] xscale = {100};
        float[] yscale = {100};
        glfwGetWindowContentScale(id, xscale, yscale);
        glfwSetWindowSize(id,
                (int) (width / xscale[0]),
                (int) (height / yscale[0]));
    }

    public Window(int width, int height, boolean resizable, String title) {
        initGLFW();
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, resizable ? GLFW_TRUE : GLFW_FALSE);
        glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_FALSE);

        this.width = width;
        this.height = height;
        this.id = glfwCreateWindow(width, height, title, NULL, NULL);
        if (this.id == NULL) throw new RuntimeException("Failed to create window");

        glfwSetFramebufferSizeCallback(id, (window, w, h) -> {
            this.width = w;
            this.height = h;
            if (resizeListener != null) {
                resizeListener.onResize(w, h, true);
            }
        });

        // Коррекция DPI
        float[] xscale = {0};
        float[] yscale = {0};
        glfwGetWindowContentScale(id, xscale, yscale);
        glfwSetWindowSize(id,
                (int) (width / xscale[0]),
                (int) (height / yscale[0]));
    }

    public void show() {
        glfwShowWindow(id);
        isShown = true;
    }

    public void hide() {
        glfwHideWindow(id);
        isShown = false;
    }

    public boolean isVisible() {
        return isShown;
    }

    public Window(int width, int height, boolean resizable, String title, Window parentWindow) {
        initGLFW();
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, resizable ? GLFW_TRUE : GLFW_FALSE);
        glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_FALSE);

        this.width = width;
        this.height = height;
        this.id = glfwCreateWindow(width, height, title, NULL, parentWindow.getId());
        if (this.id == NULL) throw new RuntimeException("Failed to create window");

        glfwSetFramebufferSizeCallback(id, (window, w, h) -> {
            this.width = w;
            this.height = h;
            if (resizeListener != null) {
                resizeListener.onResize(w, h, true);
            }
        });

        // Коррекция DPI
        float[] xscale = {0};
        float[] yscale = {0};
        glfwGetWindowContentScale(id, xscale, yscale);
        glfwSetWindowSize(id,
                (int) (width / xscale[0]),
                (int) (height / yscale[0]));
    }

    private void initGLFW() {
        if (glfwInitialized) return;
        GLFWErrorCallback errorCallback;
        glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        glfwInitialized = true;
    }

    public void setActive() {
        glfwMakeContextCurrent(this.id);
        GL.createCapabilities();
        glfwSwapInterval(1);
        //if (!isShown) {
        //    glfwShowWindow(this.id);
        //    isShown = true;
        //}
    }

    public void resetMouse() {
        double[] xpos = new double[1];
        double[] ypos = new double[1];
        glfwGetCursorPos(id, xpos, ypos);
        lastMouseX = xpos[0];
        lastMouseY = ypos[0];
        firstMouse = false;
    }

    public double getScrollDelta() {
        double value = scrollY;
        scrollY = 0;
        return value;
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
            dy = (float)(lastMouseY - ypos[0]);
            lastMouseX = xpos[0];
            lastMouseY = ypos[0];
        }

        return new float[]{dx, dy};
    }

    public boolean isKeyPressed(int key) {
        return glfwGetKey(id, key) == GLFW_PRESS;
    }

    public boolean isMouseButtonPressed(int button) {
        return glfwGetMouseButton(id, button) == GLFW_PRESS;
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