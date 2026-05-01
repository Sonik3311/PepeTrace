package org.pepetrace;

import static org.lwjgl.glfw.GLFW.*;
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

import org.pepetrace.Buffers.SSBO;
import org.pepetrace.Buffers.Texture;
import org.pepetrace.GUI.*;
import org.pepetrace.Scene.Scene;
import org.pepetrace.Shader.ComputeProgram;
import org.pepetrace.Shader.Program;
import org.pepetrace.Util.ViewportRenderMode;

public class Drawer implements Window.ResizeListener, AutoCloseable {

    private ImGuiImplGlfw imGuiGlfw;
    private ImGuiImplGl3 imGuiGl3;
    private Window window;
    private ComputeProgram pathTracingProgram;
    private Program windowTextureDrawerProgram;
    private final Program outlineProgram = new Program("./src/main/glsl/util/outliner/outliner");
    private int drawVAO;
    private Camera camera;
    private boolean isLayoutInitialized = false;
    private boolean guiWantsMouse = false;
    private boolean isViewportHovered = false;

    public boolean isViewportHovered() {
        return isViewportHovered;
    }

    public void setViewportHovered(boolean hovered) {
        this.isViewportHovered = hovered;
    }
    public boolean isGuiWantsMouse() { return guiWantsMouse; }
    public SSBO getIndexBuffer() { return indexBuffer; }
    public SSBO getGeometryBuffer() { return geometryBuffer; }
    public SSBO getMaterialIndicesBuffer() {return materialIndicesBuffer;}
    public SSBO getMaterialHandlesBuffer() {return materialHandlesBuffer;}
    public SSBO getTriangleModelIndicesBuffer() { return triangleModelIndicesBuffer; }
    public SSBO getModelMatricesBuffer() { return modelMatricesBuffer; }

    private final SSBO geometryBuffer = new SSBO(GL_STATIC_DRAW, 6);
    private final SSBO indexBuffer = new SSBO(GL_STATIC_DRAW, 7);
    private final SSBO materialIndicesBuffer = new SSBO(GL_STATIC_DRAW, 8);
    private final SSBO materialHandlesBuffer = new SSBO(GL_STATIC_DRAW, 9);
    private final SSBO modelMatricesBuffer = new SSBO(GL_DYNAMIC_DRAW, 10);
    private final SSBO triangleModelIndicesBuffer = new SSBO(GL_STATIC_DRAW, 11);
    public ImInt renderMode = new ImInt(ViewportRenderMode.SHADED.ordinal());
    private UBORenderInts ubo;
    public int frame = 0;
    public ImInt samples = new ImInt(5);
    public ImInt reflections = new ImInt(2);
    public ImBoolean accumulating = new ImBoolean(false);
    public ImFloat roughness = new ImFloat(1.0f);
    private Texture pathTracingTexture;
    private Texture modelStencilTexture;
    private Texture outputTexture;
    private int fbo;
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
    private final OutlinerWindow outlinerWindow = new OutlinerWindow();
    private final ModelDataWindow modelDataWindow = new ModelDataWindow();

    public Drawer(Window window) throws FileNotFoundException {
        this.window = window;
        window.setResizeListener(this);
        this.currentWidth = window.getWidth();
        this.currentHeight = window.getHeight();

        this.initImGUI();
        this.initGL();

        window.setCursorMode(Window.CURSOR_DISABLED);
    }

    public void refreshSceneBuffers() {
        Scene scene = programState.getScene();
        scene.packScene(geometryBuffer, indexBuffer, materialIndicesBuffer, materialHandlesBuffer, triangleModelIndicesBuffer);
        scene.updateModelMatricesOnGPU(modelMatricesBuffer);
        resetRender();
    }

    public boolean sizeChanged(int newWidth, int newHeight) {
        return (newWidth != currentWidth || newHeight != currentHeight);
    }

    public Texture getRenderTexture() { return pathTracingTexture; }
    public Texture getModelStencilTexture() { return modelStencilTexture; }
    public Texture getOutputTexture() { return outputTexture; }

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
                GL_READ_WRITE,
                GL_NEAREST
        );
        modelStencilTexture = new Texture(
                currentWidth,
                currentHeight,
                false,
                12,
                GL_R32UI,
                GL_READ_WRITE,
                GL_NEAREST
        );
        outputTexture = new Texture(
                currentWidth, currentHeight, false, 13,
                GL_RGBA32F, GL_READ_WRITE, GL_LINEAR
        );
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                GL_TEXTURE_2D, outputTexture.id, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        resetRender();

        ImGuiIO io = ImGui.getIO();
        //io.setDisplaySize(currentWidth, currentHeight);
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



    public void renderFrame() {
        // 1. Запуск compute шейдера
        //glBindImageTexture(5, skybox.id, 9, false, 0, skybox.getAccess(), skybox.getImageFormat());
        ubo.updateBuffer(
            frame,
            samples.get(),
            reflections.get(),
            roughness.get(),
            ViewportRenderMode.values()[renderMode.get()]
        );

        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT | GL_TEXTURE_FETCH_BARRIER_BIT);

        pathTracingProgram.use();
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, skybox.id);
        glUniform1i(glGetUniformLocation(pathTracingProgram.id, "blurrySkybox"), 1);
        int groupsX = (currentWidth + 15) / 16;
        int groupsY = (currentHeight + 15) / 16;
        glDispatchCompute(groupsX, groupsY, 1);

        // 2. Барьер памяти - важно для синхронизации
        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL_TEXTURE_FETCH_BARRIER_BIT);

        // 3. Рендеринг квада
        glViewport(0, 0, currentWidth, currentHeight);
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        outlineProgram.use();

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // чистим прошлый фрэймбуффер (опционально)
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, modelStencilTexture.id);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, pathTracingTexture.id);

        outlineProgram.setInt("u_stencilTexture",    0);
        outlineProgram.setInt("u_colorTexture", 1);
        outlineProgram.setInt("u_selectedID", programState.getSelectedModelIndex());

        glBindVertexArray(drawVAO);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        // Start a new ImGui frame
        renderImGUI();

        boolean blockInput = guiWantsMouse && !isViewportHovered;
        if (camera.updateCamera(window, blockInput)) {
            resetRender();
        }
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
        outlinerWindow.render(ImGuiWindowFlags.NoCollapse);
        modelDataWindow.render(ImGuiWindowFlags.NoCollapse);

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
        guiWantsMouse = ImGui.getIO().getWantCaptureMouse();

        if (!guiWantsMouse && ImGui.isKeyPressed(ImGuiKey.Delete)) {
            int selected = programState.getSelectedModelIndex();
            scene = programState.getScene();
            if (selected >= 0 && selected < scene.getModels().size()) {
                scene.removeModel(selected);
                refreshSceneBuffers();
                programState.setSelectedModelIndex(-1);
                resetRender();
            }
        }
    }



    private void initGL() throws FileNotFoundException {
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        pathTracingTexture = new Texture(
                currentWidth, currentHeight, false, 1,
                GL_RGBA32F, GL_READ_WRITE, GL_NEAREST
        );
        modelStencilTexture = new Texture(
                currentWidth, currentHeight, false, 12,
                GL_R32UI, GL_READ_WRITE, GL_NEAREST
        );
        outputTexture = new Texture(
                currentWidth, currentHeight, false, 13,
                GL_RGBA32F, GL_READ_WRITE, GL_NEAREST
        );

        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                GL_TEXTURE_2D, outputTexture.id, 0);
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("FBO not complete: " + status);
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        pathTracingProgram = new ComputeProgram("./src/main/glsl/renderers/viewport/program");
        windowTextureDrawerProgram = new Program("./src/main/glsl/screenQuad");
        drawVAO = glGenVertexArrays();
        ubo = new UBORenderInts(3);
        modelMatricesBuffer.fillBuffer(new float[0]); // временно пустой
        triangleModelIndicesBuffer.fillBuffer(new int[0]); // временно пустой
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
        triangleModelIndicesBuffer.close();
    }
}
