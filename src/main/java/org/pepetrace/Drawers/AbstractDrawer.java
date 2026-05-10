package org.pepetrace.Drawers;

import imgui.ImFontConfig;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.pepetrace.Camera;
import org.pepetrace.GlobalState;
import org.pepetrace.Window;

import static org.lwjgl.glfw.GLFW.*;

public abstract class AbstractDrawer implements Window.ResizeListener {
    protected final GlobalState programState = GlobalState.getInstance();

    protected final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    protected final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    protected final Window window;
    protected int frameId;
    protected Camera camera;

    protected int currentWidth;
    protected int currentHeight;

    public int getCurrentWidth() {return currentWidth;}
    public int getCurrentHeight() {return currentHeight;}

    public int getFrameId() { return frameId; }



    public AbstractDrawer(Window window, String guiLayoutFile) {
        this.window = window;
        window.setResizeListener(this);
        init(guiLayoutFile);
    }


    public void onResize(int newWidth, int newHeight, boolean isFromGlfw) {
        currentWidth = newWidth;
        currentHeight = newHeight;
    };


    public void renderFrame() {frameId++;}

    public void setCamera(Camera camera) {
        this.camera = camera;
    }
    
    protected void init(String imGuiLayoutFile) {
        ImGui.createContext();

        ImGuiIO io = ImGui.getIO();
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable); // Разрешает перетаскивание окон друг в друга

        io.setIniFilename(imGuiLayoutFile);
        io.setDisplaySize(currentWidth, currentHeight); // Изначальный размер окна GLFW (Не нужно судя по всему, так как наследует от GLFW автоматически под капотом)

        float[] scalex_factor = {0};
        float[] scaley_factor = {0};
        glfwGetMonitorContentScale(glfwGetPrimaryMonitor(), scalex_factor, scaley_factor);
        ImFontConfig config = new ImFontConfig();
        config.setPixelSnapH(true);
        config.setRasterizerDensity(scalex_factor[0]);
        io.getFonts().addFontFromFileTTF("./src/main/resources/Fonts/GoogleSansCode-VariableFont_wght.ttf", 14, config);

        // 3. Инициализировать байндинги GLFW и OpenGL 4.6
        imGuiGlfw.init(window.getId(), true); // The boolean is for integrating the callbacks
        imGuiGl3.init("#version 460"); // Your GLSL version

        // После инициализации ImGui ПЕРЕУСТАНАВЛИВАЕМ свой колбэк прокрутки,
        glfwSetScrollCallback(window.getId(), (win, xoff, yoff) -> {
            ImGui.getIO().setMouseWheel((float) yoff);
            // Накапливаем для камеры
            window.addScrollDelta(yoff);
        });
    }
}
