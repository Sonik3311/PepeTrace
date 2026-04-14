package org.pepetrace;

import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_messageBox;
import static org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog;

import imgui.*;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.pepetrace.Buffers.SSBO;
import org.pepetrace.Buffers.Texture;
import org.pepetrace.GUI.*;
import org.pepetrace.Scene.Material.TextureMaterial;
import org.pepetrace.Scene.Scene;
import org.pepetrace.Shader.ComputeProgram;
import org.pepetrace.Shader.Program;
import org.pepetrace.Util.Passport;
import org.lwjgl.util.tinyfd.TinyFileDialogs.*;

public class Drawer implements Window.ResizeListener, AutoCloseable {

    private ImGuiImplGlfw imGuiGlfw;
    private ImGuiImplGl3 imGuiGl3;
    private Window window;
    private ComputeProgram pathTracingProgram;
    private Program windowTextureDrawerProgram;
    private int drawVAO;
    private Camera camera;
    private boolean isLayoutInitialized = false;


    public SSBO getIndexBuffer() { return indexBuffer; }
    public SSBO getGeometryBuffer() { return geometryBuffer; }
    public SSBO getMaterialIndicesBuffer() {return materialIndicesBuffer;}
    public SSBO getMaterialHandlesBuffer() {return materialHandlesBuffer;}

    private SSBO geometryBuffer = new SSBO(GL_STATIC_DRAW, 6);
    private SSBO indexBuffer = new SSBO(GL_STATIC_DRAW, 7);
    private SSBO materialIndicesBuffer = new SSBO(GL_STATIC_DRAW, 8);
    private SSBO materialHandlesBuffer = new SSBO(GL_STATIC_DRAW, 9);
    public ImInt renderMode = new ImInt(ViewportRenderMode.SHADED.ordinal());
    private UBORenderInts ubo;
    public int frame = 0;
    public ImInt samples = new ImInt(5);
    public ImInt reflections = new ImInt(2);
    public ImBoolean accumulating = new ImBoolean(false);
    public ImFloat roughness = new ImFloat(1.0f);
    private Texture pathTracingTexture;
    private final Texture skybox = Texture.createFromFile(
        4,
        true,
        GL_READ_ONLY,
        "./src/main/resources/Textures/grey_background.png"
    );
    private int currentWidth;
    private int currentHeight;

    private final GlobalState programState = GlobalState.getInstance();

    private final BuildInfoWindow buildInfoWindow = new BuildInfoWindow();
    private final MainMenuBar mainMenuBar = new MainMenuBar();
    private final MaterialManagerWindow materialManagerWindow = new MaterialManagerWindow();
    private final CameraInfoWindow cameraInfoWindow = new CameraInfoWindow();
    private final ViewportWindow viewportWindow = new ViewportWindow();
    private final ViewportRenderSettingsWindow viewportRenderSettingsWindow = new ViewportRenderSettingsWindow();

    public Drawer(Window window) throws FileNotFoundException {
        this.window = window;
        window.setResizeListener(this);
        this.currentWidth = window.getWidth();
        this.currentHeight = window.getHeight();

        this.initImGUI();
        this.initGL();

        window.setCursorMode(Window.CURSOR_DISABLED);
    }

    public boolean sizeChanged(int newWidth, int newHeight) {
        return (newWidth != currentWidth || newHeight != currentHeight);
    }

    public Texture getRenderTexture() { return pathTracingTexture; }

    @Override
    public void onResize(int newWidth, int newHeight) {
        if (!sizeChanged(newWidth, newHeight)) return;
        currentWidth = newWidth;
        currentHeight = newHeight;

        // Пересоздаём текстуру
        if (pathTracingTexture != null) {
            glDeleteTextures(pathTracingTexture.id);
        }
        pathTracingTexture = new Texture(
            currentWidth,
            currentHeight,
            false,
            1,
            GL_RGBA32F,
            GL_RGBA,
            GL_FLOAT,
            GL_RGBA32F,
            GL_READ_WRITE
        );
        resetRender();

        ImGuiIO io = ImGui.getIO();
        io.setDisplaySize(currentWidth, currentHeight);
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public void resetRender() {
        frame = 0;
    }

    private void initImGUI() {
        imGuiGlfw = new ImGuiImplGlfw();
        imGuiGl3 = new ImGuiImplGl3();
        ImGui.createContext();

        ImGuiIO io = ImGui.getIO();
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable); // Разрешает перетаскивание окон друг в друга

        io.setIniFilename("guilayout.ini"); // Выключаем .ini файл, чтобы избежать сохранения состояния окон ImGUI
        io.setDisplaySize(currentWidth, currentHeight); // Изначальный размер окна GLFW (Не нужно судя по всему, так как наследует от GLFW автоматически под капотом)
        io.getFonts().addFontDefault(); // Загрузить стандартный шрифт текста.

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



    public void renderFrame() {
        // 1. Запуск compute шейдера
        glViewport(0, 0, currentWidth, currentHeight);
        //glBindImageTexture(5, skybox.id, 9, false, 0, skybox.getAccess(), skybox.getImageFormat());
        ubo.updateBuffer(
            frame,
            samples.get(),
            reflections.get(),
            roughness.get(),
            ViewportRenderMode.values()[renderMode.get()]
        );
        pathTracingProgram.use();
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, skybox.id);
        glUniform1i(glGetUniformLocation(pathTracingProgram.id, "blurrySkybox"), 1);
        int groupsX = (currentWidth + 15) / 16;
        int groupsY = (currentHeight + 15) / 16;
        glDispatchCompute(groupsX, groupsY, 1);

        // 2. Барьер памяти - важно для синхронизации
        glMemoryBarrier(
            GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL_TEXTURE_FETCH_BARRIER_BIT
        );

        // 3. Рендеринг квада
        //glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // чистим прошлый фрэймбуффер (опционально)
        //glActiveTexture(GL_TEXTURE0);
        //glBindTexture(GL_TEXTURE_2D, pathTracingTexture.id);
        //windowTextureDrawerProgram.use();
        //windowTextureDrawerProgram.setInt("tex", 0);

        // Убедимся, что текстура привязана (опционально)
        //glBindVertexArray(drawVAO);
        //glDrawArrays(GL_TRIANGLES, 0, 3);

        // Start a new ImGui frame
        renderImGUI();

        if (accumulating.get()) {
            frame++;
        }
    }

    private void renderImGUI() {
        Scene scene = (Scene) programState.getArbitraryData("Scene");
        imGuiGl3.newFrame();
        imGuiGlfw.newFrame();
        ImGui.newFrame();
        // Меню сверху
        mainMenuBar.render(0);
        // Основное пространство


        ImGui.dockSpaceOverViewport();
        buildInfoWindow.render(ImGuiWindowFlags.NoCollapse);
        materialManagerWindow.render(ImGuiWindowFlags.NoCollapse);
        cameraInfoWindow.render(ImGuiWindowFlags.NoCollapse);
        viewportWindow.render(0);
        viewportRenderSettingsWindow.render(ImGuiWindowFlags.NoCollapse);

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }



    private void initGL() throws FileNotFoundException {
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        pathTracingTexture = new Texture(
                currentWidth, currentHeight, false, 1,
                GL_RGBA32F, GL_RGBA, GL_FLOAT, GL_RGBA32F, GL_READ_WRITE
        );
        pathTracingProgram = new ComputeProgram("./src/main/glsl/renderers/viewport/program");
        windowTextureDrawerProgram = new Program("./src/main/glsl/screenQuad");
        drawVAO = glGenVertexArrays();
        ubo = new UBORenderInts(3);
    }

    @Override
    public void close() throws Exception {
        pathTracingTexture.close();
        skybox.close();
        geometryBuffer.close();
        indexBuffer.close();
        materialHandlesBuffer.close();
        materialIndicesBuffer.close();
        ubo.close();
        pathTracingProgram.close();
        imGuiGl3.shutdown();      // Удаляет шейдеры и буферы ImGui из видеопамяти
        imGuiGlfw.shutdown();     // Отключает обработчики событий GLFW
        ImGui.destroyContext();
    }
}
