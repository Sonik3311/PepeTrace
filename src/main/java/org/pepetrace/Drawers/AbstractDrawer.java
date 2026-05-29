package org.pepetrace.Drawers;

import imgui.ImFontConfig;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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



    public AbstractDrawer(Window window) {
        this.window = window;
        window.setResizeListener(this);
        init(getWritableLayoutPath().toString());
    }

    private static Path getConfigDir() {
        String home = System.getProperty("user.home");
        return Paths.get(home, ".config", "pepetrace");
    }

    private static Path getWritableLayoutPath() {
        Path configDir = getConfigDir();
        Path layoutFile = configDir.resolve("guilayout.ini");
        if (Files.exists(layoutFile)) {
            return layoutFile;
        }
        try {
            Files.createDirectories(configDir);
            try (InputStream in = AbstractDrawer.class.getResourceAsStream("/guilayout.ini")) {
                if (in != null) {
                    try (OutputStream out = Files.newOutputStream(layoutFile)) {
                        in.transferTo(out);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to extract guilayout.ini: " + e.getMessage());
        }
        return layoutFile;
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
        try (InputStream fontIs = getClass().getResourceAsStream("/Fonts/GoogleSansCode-VariableFont_wght.ttf")) {
            if (fontIs != null) {
                byte[] fontBytes = fontIs.readAllBytes();
                io.getFonts().addFontFromMemoryTTF(fontBytes, 14, config);
            }
        } catch (IOException e) {
            System.err.println("Failed to load font resource: " + e.getMessage());
        }

        // 3. Инициализировать байндинги GLFW и OpenGL 4.6
        imGuiGlfw.init(window.getId(), true); // The boolean is for integrating the callbacks
        imGuiGl3.init("#version 460"); // Your GLSL version

        // После инициализации ImGui ПЕРЕУСТАНАВЛИВАЕМ свой колбэк прокрутки,
        glfwSetScrollCallback(window.getId(), (win, xoff, yoff) -> {
            ImGui.getIO().setMouseWheel((float) yoff);
            // Накапливаем для камеры
            window.addScrollDelta(yoff);
        });

        currentHeight = window.getHeight();
        currentWidth = window.getWidth();
    }
}
