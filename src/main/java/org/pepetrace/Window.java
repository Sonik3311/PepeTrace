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
    private boolean glCreated = false;

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
        this(1600, 900, true, "PepeTrace", NULL);
    }

    public Window(int width, int height, boolean resizable, String title) {
        this(width, height, resizable, title, NULL);
    }

    public void show() {
        glfwShowWindow(id);
        isShown = true;
    }

    public void hide() {
        glfwHideWindow(id);
        isShown = false;
    }

    public void pacedHide() {
        glfwSwapBuffers(id);
        glfwPollEvents();
        try {
            Thread.sleep(16);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        glfwHideWindow(id);
        isShown = false;
    }

    public boolean isVisible() {
        return isShown;
    }

    public Window(int width, int height, boolean resizable, String title, long shareContext) {
        this(width, height, resizable, title, shareContext, true);
    }

    public Window(int width, int height, boolean resizable, String title, long shareContext, boolean visible) {
        initGLFW();
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_VISIBLE, visible ? GLFW_TRUE : GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, resizable ? GLFW_TRUE : GLFW_FALSE);
        glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_FALSE);

        this.width = width;
        this.height = height;
        this.id = glfwCreateWindow(width, height, title, NULL, shareContext);
        if (this.id == NULL) throw new RuntimeException("Failed to create window");
        this.isShown = visible;

        glfwSetFramebufferSizeCallback(id, (window, w, h) -> {
            this.width = w;
            this.height = h;
            if (resizeListener != null) {
                resizeListener.onResize(w, h, true);
            }
        });

        float[] xscale = {0};
        float[] yscale = {0};
        glfwGetWindowContentScale(id, xscale, yscale);
        glfwSetWindowSize(id,
                (int) (width / xscale[0]),
                (int) (height / yscale[0]));
    }

    public Window(int width, int height, boolean resizable, String title, Window parentWindow) {
        this(width, height, resizable, title, parentWindow.getId());
    }

    public Window(int width, int height, boolean resizable, String title, Window parentWindow, boolean visible) {
        this(width, height, resizable, title, parentWindow.getId(), visible);
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
        makeCurrent();
        glfwSwapInterval(1);
    }

    public void makeCurrent() {
        glfwMakeContextCurrent(this.id);
        if (!glCreated) {
            GL.createCapabilities();
            glCreated = true;
        }
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

    public void resetCloseFlag() {
        glfwSetWindowShouldClose(id, false);
    }
}