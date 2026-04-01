package org.pepetrace;

import static org.lwjgl.opengl.GL46.*;
import org.pepetrace.Scene.TestTriangleScene;

import imgui.*;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import java.io.FileNotFoundException;

import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.pepetrace.Buffers.SSBO;
import org.pepetrace.Buffers.Texture;
import org.pepetrace.Shader.ComputeProgram;
import org.pepetrace.Shader.Program;
import org.pepetrace.Util.Passport;

public class Drawer implements Window.ResizeListener {

    private ImGuiImplGlfw imGuiGlfw;
    private ImGuiImplGl3 imGuiGl3;
    private Window window;
    private ComputeProgram pathTracingProgram;
    private Program windowTextureDrawerProgram;
    private int drawVAO;
    private Camera camera;

    public SSBO getTriangleBuffer() {
        return triangleBuffer;
    }

    private SSBO triangleBuffer;
    private UBORenderInts ubo;
    private int frame = 0;
    private ImInt samples = new ImInt(5);
    private ImInt reflections = new ImInt(2);
    private ImBoolean accumulating = new ImBoolean(false);
    private ImFloat roughness = new ImFloat(1.0f);
    private Texture pathTracingTexture;
    private final Texture skybox = Texture.createFromFile(6, GL_READ_ONLY,"./src/main/java/org/pepetrace/skybox.jpg");
    private int currentWidth;
    private int currentHeight;

    public Drawer(Window window) throws FileNotFoundException {
        this.window = window;
        window.setResizeListener(this);
        this.currentWidth = window.getWidth();
        this.currentHeight = window.getHeight();

        this.initImGUI();
        this.initGL();

        window.setCursorMode(Window.CURSOR_DISABLED);
    }

    @Override
    public void onResize(int newWidth, int newHeight) {
        if (newWidth == currentWidth && newHeight == currentHeight) return;
        currentWidth = newWidth;
        currentHeight = newHeight;

        // Пересоздаём текстуру
        if (pathTracingTexture != null) {
            glDeleteTextures(pathTracingTexture.id);
        }
        pathTracingTexture = new Texture(currentWidth, currentHeight, 0, GL_RGBA32F, GL_RGBA, GL_FLOAT, GL_RGBA32F, GL_READ_WRITE);
        resetRender();
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
        // 1. Создаём контекст ImGUI
        ImGui.createContext();

        // 2. Базовая конфигурация
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null); // Выключаем .ini файл, чтобы избежать сохранения состояния окон ImGUI
        io.setDisplaySize(currentWidth, currentHeight); // Изначальный размер окна GLFW (Не нужно судя по всему, так как наследует от GLFW автоматически под капотом)
        io.getFonts().addFontDefault(); // Загрузить стандартный шрифт текста.

        // 3. Инициализировать байндинги GLFW и OpenGL 4.6
        imGuiGlfw.init(window.getId(), true); // The boolean is for integrating the callbacks
        imGuiGl3.init("#version 460"); // Your GLSL version
    }

    public void renderFrame() {
        // 1. Запуск compute шейдера
        glViewport(0, 0, currentWidth, currentHeight);
        ubo.updateBuffer(frame, samples.get(), reflections.get(), roughness.get());
        pathTracingProgram.use();
        int groupsX = (currentWidth + 15) / 16;
        int groupsY = (currentHeight + 15) / 16;
        glDispatchCompute(groupsX, groupsY, 1);

        // 2. Барьер памяти - важно для синхронизации
        glMemoryBarrier(
            GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL_TEXTURE_FETCH_BARRIER_BIT
        );

        // 3. Рендеринг квада
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // чистим прошлый фрэймбуффер (опционально)
        windowTextureDrawerProgram.use();
        windowTextureDrawerProgram.setInt("tex", 0);

        // Убедимся, что текстура привязана (опционально)
        glBindVertexArray(drawVAO);
        glDrawArrays(GL_TRIANGLES, 0, 3);

        // Start a new ImGui frame
        renderImGUI();

        if (accumulating.get()) {frame++;}
    }

    private void renderImGUI() {
        imGuiGl3.newFrame();
        imGuiGlfw.newFrame();
        ImGui.newFrame();
        int windowFlags = ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse;

        //if (cursorLocked) {
        //    ImGui.getIO().setMousePos(-Float.MAX_VALUE, -Float.MAX_VALUE);
        //}

        // Здесь строится UI
        //ImGui.showDemoWindow(); // Встроенное демо окно

        //ImGui.setNextWindowSize(300, 150, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowPos(0, 0, ImGuiCond.FirstUseEver);
        ImGui.begin("Build info", windowFlags);
        if (camera != null) {
            ImGui.text(
                String.format(
                    "Mode: %s",
                    camera.getCameraMode() == 0 ? "Free" : "Orbit"
                )
            );
            Vector3f pos = camera.getPosition();
            Vector2f rot = camera.getYawPitch();
            ImGui.text(
                String.format("Pos: (%.2f, %.2f, %.2f)", pos.x, pos.y, pos.z)
            );
            ImGui.text(String.format("Yaw: %.2f Pitch: %.2f", rot.x, rot.y));
        }
        //ImGui.text(String.format("Camera: (%.2f, %.2f, %.2f)", cameraPosition.x, cameraPosition.y, cameraPosition.z));
        //ImGui.text(String.format("Yaw: %.2f Pitch: %.2f", cameraRotation[0], cameraRotation[1]));
        ImGui.text(
            String.format("Build No. %s", Passport.INSTANCE.getBuildNumber())
        );
        ImGui.text(String.format("OS: %s", Passport.INSTANCE.getBuildOS()));
        ImGui.text(
            String.format(
                "Build timestamp: %s",
                Passport.INSTANCE.getBuildTime()
            )
        );
        ImGui.text(
            String.format("Java: %s", Passport.INSTANCE.getJavaVersion())
        );
        ImGui.text(
            String.format(
                "Git branch: %s",
                Passport.INSTANCE.getGitBranchHash()
            )
        );
        ImGui.end();

        ImGui.setNextWindowPos(0, 190, ImGuiCond.FirstUseEver);
        ImGui.begin("Render Settings", windowFlags);
        if (ImGui.inputInt("Samples", samples)) {
            int min = 1, max = 16384;
            int clamped = Math.clamp(samples.get(), min, max);
            samples.set(clamped);
            frame = 0;
        }
        if (ImGui.inputInt("Reflections", reflections)) {
            int min = 1, max = 16384;
            int clamped = Math.clamp(reflections.get(), min, max);
            reflections.set(clamped);
            frame = 0;
        }
        if (ImGui.checkbox("Accumulate frames", accumulating)) {
            // Optional: Add code here to execute immediately when the state changes.
            //System.out.println("Checkbox state changed to: " + accumulating.get());
            if (!accumulating.get()) {
                frame = 0;
            }
        }

        if (ImGui.inputFloat("roughness", roughness)) {
            int min = 0, max = 1;
            float clamped = Math.clamp(roughness.get(), min, max);
            roughness.set(clamped);
            frame = 0;
        }
        if (ImGui.button("Reset Accumulation")) {
            frame = 0;
        }
        ImGui.end();

        // Render ImGui
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }

    private void initGL() throws FileNotFoundException {
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        pathTracingTexture = new Texture(currentWidth, currentHeight, 0, GL_RGBA32F, GL_RGBA, GL_FLOAT, GL_RGBA32F, GL_READ_WRITE);
        pathTracingProgram = new ComputeProgram("./src/main/glsl/program");

        windowTextureDrawerProgram = new Program("./src/main/glsl/screenQuad");

        drawVAO = glGenVertexArrays();

        //ТЕСТ
        //TODO: Убрать и сделать нормально
        triangleBuffer = new SSBO(GL_STATIC_DRAW, 1);

        //triangleBuffer.fillBuffer(TestTriangleScene.vertices);

        ubo = new UBORenderInts(2);
    }
}
