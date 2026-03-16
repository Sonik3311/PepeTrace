package org.pepetrace;

import static org.lwjgl.opengl.GL46.*;

import imgui.*;
import imgui.flag.ImGuiCond;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import java.io.FileNotFoundException;

import org.joml.Vector3f;
import org.pepetrace.Buffers.SSBO;
import org.pepetrace.Shader.ComputeProgram;
import org.pepetrace.Shader.Program;
import org.pepetrace.Buffers.Texture;
import org.pepetrace.Util.Passport;

public class Drawer {

    private ImGuiImplGlfw imGuiGlfw;
    private ImGuiImplGl3 imGuiGl3;
    private Window window;

    private ComputeProgram pathTracingProgram;
    private Program windowTextureDrawerProgram;
    private int drawVAO;

    private SSBO TEST_SSBO;
    private UBORenderInts ubo;

    private Vector3f cameraPosition;
    private float[] cameraRotation = new float[2]; // [yaw, pitch]
    private float mouseSensitivity = 0.1f;
    private float moveSpeed = 0.05f;
    private boolean cursorLocked = true;
    private boolean wasEscapePressed = false;
    private SSBO cameraSSBO;

    private int frame = 0;


    private Texture pathTracingTexture;

    public Drawer(Window window, Vector3f cameraPos, float[] cameraRot) throws FileNotFoundException {
        this.window = window;
        this.cameraPosition = cameraPos;
        this.cameraRotation = cameraRot;

        this.initImGUI();
        this.initGL();

        window.setCursorMode(Window.CURSOR_DISABLED);
    }

    private void updateCameraSSBO() {
        if (cameraSSBO == null) {
            cameraSSBO = new SSBO(GL_DYNAMIC_DRAW, 3); // Используем binding 3
        }

        float[] cameraData = new float[]{
                cameraPosition.x, cameraPosition.y, cameraPosition.z,
                cameraRotation[0], cameraRotation[1], 0.0f
        };
        cameraSSBO.fillBuffer(cameraData);
    }

    public void handleCameraInput() {

        if (cursorLocked) {
            float[] mouseDelta = window.getMouseDelta();
            cameraRotation[0] += mouseDelta[0] * mouseSensitivity;
            cameraRotation[1] += mouseDelta[1] * mouseSensitivity;
            cameraRotation[1] = Math.max(-89.0f, Math.min(89.0f, cameraRotation[1]));
        }

        boolean escapePressed = window.isKeyPressed(Window.KEY_ESCAPE);
        if (escapePressed && !wasEscapePressed) {
            cursorLocked = !cursorLocked;
            window.setCursorMode(cursorLocked ? Window.CURSOR_DISABLED : Window.CURSOR_NORMAL);
            window.resetMouse();
        }
        wasEscapePressed = escapePressed;

        if (cursorLocked) {
            float yawRad = (float)Math.toRadians(cameraRotation[0]);

            if (window.isKeyPressed(Window.KEY_W)) {
                cameraPosition.x += (float)Math.sin(yawRad) * moveSpeed;
                cameraPosition.z += (float)Math.cos(yawRad) * moveSpeed;
            }
            if (window.isKeyPressed(Window.KEY_S)) {
                cameraPosition.x -= (float)Math.sin(yawRad) * moveSpeed;
                cameraPosition.z -= (float)Math.cos(yawRad) * moveSpeed;
            }
            if (window.isKeyPressed(Window.KEY_A)) {
                cameraPosition.x += (float)Math.sin(yawRad + Math.PI/2) * moveSpeed;
                cameraPosition.z += (float)Math.cos(yawRad + Math.PI/2) * moveSpeed;
            }
            if (window.isKeyPressed(Window.KEY_D)) {
                cameraPosition.x += (float)Math.sin(yawRad - Math.PI/2) * moveSpeed;
                cameraPosition.z += (float)Math.cos(yawRad - Math.PI/2) * moveSpeed;
            }
            if (window.isKeyPressed(Window.KEY_E)) {
                cameraPosition.y += moveSpeed;
            }
            if (window.isKeyPressed(Window.KEY_Q)) {
                cameraPosition.y -= moveSpeed;
            }
        }

        updateCameraSSBO();
    }

    private void initImGUI() {
        imGuiGlfw = new ImGuiImplGlfw();
        imGuiGl3 = new ImGuiImplGl3();
        // 1. Создаём контекст ImGUI
        ImGui.createContext();

        // 2. Базовая конфигурация optional)
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null); // Выключаем .ini файл, чтобы избежать сохранения состояния окон ImGUI
        io.setDisplaySize(1600, 900); // Изначальный размер окна GLFW (Не нужно судя по всему, так как наследует от GLFW автоматически под капотом)
        io.getFonts().addFontDefault(); // Загрузить стандартный шрифт текста.

        // 3. Инициализировать байндинги GLFW и OpenGL 4.6
        imGuiGlfw.init(window.getId(), true); // The boolean is for integrating the callbacks
        imGuiGl3.init("#version 460"); // Your GLSL version
    }

    public void renderFrame() {
        // 1. Запуск compute шейдера
        ubo.updateBuffer(frame);
        pathTracingProgram.use();
        glDispatchCompute(window.getWidth() / 16, window.getHeight() / 16 + 1, 1);

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

        frame++;
    }

    private void renderImGUI() {
        imGuiGl3.newFrame();
        imGuiGlfw.newFrame();
        ImGui.newFrame();

        if (cursorLocked) {
            ImGui.getIO().setMousePos(-Float.MAX_VALUE, -Float.MAX_VALUE);
        }

        // Здесь строится UI
        ImGui.showDemoWindow(); // Встроенное демо окно

        //ImGui.setNextWindowSize(300, 150, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowPos(0, 0, ImGuiCond.FirstUseEver);
        ImGui.begin("Build info");
        ImGui.text(String.format("Camera: (%.2f, %.2f, %.2f)", cameraPosition.x, cameraPosition.y, cameraPosition.z));
        ImGui.text(String.format("Yaw: %.2f Pitch: %.2f", cameraRotation[0], cameraRotation[1]));
        ImGui.text(String.format("Build No. %s", Passport.INSTANCE.getBuildNumber()));
        ImGui.text(String.format("OS: %s", Passport.INSTANCE.getBuildOS()));
        ImGui.text(String.format("Build timestamp: %s", Passport.INSTANCE.getBuildTime()));
        ImGui.text(String.format("Java: %s", Passport.INSTANCE.getJavaVersion()));
        ImGui.text(String.format("Git branch: %s", Passport.INSTANCE.getGitBranchHash()));
        if (ImGui.button("Set frame to 0")) {
            frame = 0;
        }
        ImGui.end();

        // Render ImGui
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }

    private void initGL() throws FileNotFoundException {
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        pathTracingTexture = new Texture(
                1600,
                900,
                GL_RGBA,
                GL_FLOAT,
                GL_RGBA32F
        );


        pathTracingProgram = new ComputeProgram(
                "./src/main/glsl/program"
        );


        windowTextureDrawerProgram = new Program("./src/main/glsl/screenQuad");


        drawVAO = glGenVertexArrays();


        //ТЕСТ
        //TODO: Убрать и сделать нормально
        TEST_SSBO = new SSBO(GL_STATIC_DRAW, 1);
        float[] sphereVertices = {
                0.500000f,
                -0.866025f,
                0.000000f,
                0.000000f,
                -1.000000f,
                0.000000f,
                0.000000f,
                -1.000000f,
                0.000000f,
                0.250000f,
                -0.866025f,
                0.433013f,
                0.500000f,
                -0.866025f,
                0.000000f,
                0.000000f,
                -1.000000f,
                0.000000f,
                0.250000f,
                -0.866025f,
                0.433013f,
                0.000000f,
                -1.000000f,
                0.000000f,
                -0.000000f,
                -1.000000f,
                0.000000f,
                -0.250000f,
                -0.866025f,
                0.433013f,
                0.250000f,
                -0.866025f,
                0.433013f,
                -0.000000f,
                -1.000000f,
                0.000000f,
                -0.250000f,
                -0.866025f,
                0.433013f,
                -0.000000f,
                -1.000000f,
                0.000000f,
                -0.000000f,
                -1.000000f,
                0.000000f,
                -0.500000f,
                -0.866025f,
                0.000000f,
                -0.250000f,
                -0.866025f,
                0.433013f,
                -0.000000f,
                -1.000000f,
                0.000000f,
                -0.500000f,
                -0.866025f,
                0.000000f,
                -0.000000f,
                -1.000000f,
                0.000000f,
                -0.000000f,
                -1.000000f,
                -0.000000f,
                -0.250000f,
                -0.866025f,
                -0.433013f,
                -0.500000f,
                -0.866025f,
                0.000000f,
                -0.000000f,
                -1.000000f,
                -0.000000f,
                -0.250000f,
                -0.866025f,
                -0.433013f,
                -0.000000f,
                -1.000000f,
                -0.000000f,
                0.000000f,
                -1.000000f,
                -0.000000f,
                0.250000f,
                -0.866025f,
                -0.433013f,
                -0.250000f,
                -0.866025f,
                -0.433013f,
                0.000000f,
                -1.000000f,
                -0.000000f,
                0.250000f,
                -0.866025f,
                -0.433013f,
                0.000000f,
                -1.000000f,
                -0.000000f,
                0.000000f,
                -1.000000f,
                -0.000000f,
                0.500000f,
                -0.866025f,
                -0.000000f,
                0.250000f,
                -0.866025f,
                -0.433013f,
                0.000000f,
                -1.000000f,
                -0.000000f,
                0.866025f,
                -0.500000f,
                0.000000f,
                0.500000f,
                -0.866025f,
                0.000000f,
                0.250000f,
                -0.866025f,
                0.433013f,
                0.433013f,
                -0.500000f,
                0.750000f,
                0.866025f,
                -0.500000f,
                0.000000f,
                0.250000f,
                -0.866025f,
                0.433013f,
                0.433013f,
                -0.500000f,
                0.750000f,
                0.250000f,
                -0.866025f,
                0.433013f,
                -0.250000f,
                -0.866025f,
                0.433013f,
                -0.433013f,
                -0.500000f,
                0.750000f,
                0.433013f,
                -0.500000f,
                0.750000f,
                -0.250000f,
                -0.866025f,
                0.433013f,
                -0.433013f,
                -0.500000f,
                0.750000f,
                -0.250000f,
                -0.866025f,
                0.433013f,
                -0.500000f,
                -0.866025f,
                0.000000f,
                -0.866025f,
                -0.500000f,
                0.000000f,
                -0.433013f,
                -0.500000f,
                0.750000f,
                -0.500000f,
                -0.866025f,
                0.000000f,
                -0.866025f,
                -0.500000f,
                0.000000f,
                -0.500000f,
                -0.866025f,
                0.000000f,
                -0.250000f,
                -0.866025f,
                -0.433013f,
                -0.433013f,
                -0.500000f,
                -0.750000f,
                -0.866025f,
                -0.500000f,
                0.000000f,
                -0.250000f,
                -0.866025f,
                -0.433013f,
                -0.433013f,
                -0.500000f,
                -0.750000f,
                -0.250000f,
                -0.866025f,
                -0.433013f,
                0.250000f,
                -0.866025f,
                -0.433013f,
                0.433013f,
                -0.500000f,
                -0.750000f,
                -0.433013f,
                -0.500000f,
                -0.750000f,
                0.250000f,
                -0.866025f,
                -0.433013f,
                0.433013f,
                -0.500000f,
                -0.750000f,
                0.250000f,
                -0.866025f,
                -0.433013f,
                0.500000f,
                -0.866025f,
                -0.000000f,
                0.866025f,
                -0.500000f,
                -0.000000f,
                0.433013f,
                -0.500000f,
                -0.750000f,
                0.500000f,
                -0.866025f,
                -0.000000f,
                1.000000f,
                0.000000f,
                0.000000f,
                0.866025f,
                -0.500000f,
                0.000000f,
                0.433013f,
                -0.500000f,
                0.750000f,
                0.500000f,
                0.000000f,
                0.866025f,
                1.000000f,
                0.000000f,
                0.000000f,
                0.433013f,
                -0.500000f,
                0.750000f,
                0.500000f,
                0.000000f,
                0.866025f,
                0.433013f,
                -0.500000f,
                0.750000f,
                -0.433013f,
                -0.500000f,
                0.750000f,
                -0.500000f,
                0.000000f,
                0.866025f,
                0.500000f,
                0.000000f,
                0.866025f,
                -0.433013f,
                -0.500000f,
                0.750000f,
                -0.500000f,
                0.000000f,
                0.866025f,
                -0.433013f,
                -0.500000f,
                0.750000f,
                -0.866025f,
                -0.500000f,
                0.000000f,
                -1.000000f,
                0.000000f,
                0.000000f,
                -0.500000f,
                0.000000f,
                0.866025f,
                -0.866025f,
                -0.500000f,
                0.000000f,
                -1.000000f,
                0.000000f,
                0.000000f,
                -0.866025f,
                -0.500000f,
                0.000000f,
                -0.433013f,
                -0.500000f,
                -0.750000f,
                -0.500000f,
                0.000000f,
                -0.866025f,
                -1.000000f,
                0.000000f,
                0.000000f,
                -0.433013f,
                -0.500000f,
                -0.750000f,
                -0.500000f,
                0.000000f,
                -0.866025f,
                -0.433013f,
                -0.500000f,
                -0.750000f,
                0.433013f,
                -0.500000f,
                -0.750000f,
                0.500000f,
                0.000000f,
                -0.866025f,
                -0.500000f,
                0.000000f,
                -0.866025f,
                0.433013f,
                -0.500000f,
                -0.750000f,
                0.500000f,
                0.000000f,
                -0.866025f,
                0.433013f,
                -0.500000f,
                -0.750000f,
                0.866025f,
                -0.500000f,
                -0.000000f,
                1.000000f,
                0.000000f,
                -0.000000f,
                0.500000f,
                0.000000f,
                -0.866025f,
                0.866025f,
                -0.500000f,
                -0.000000f,
                0.866025f,
                0.500000f,
                0.000000f,
                1.000000f,
                0.000000f,
                0.000000f,
                0.500000f,
                0.000000f,
                0.866025f,
                0.433013f,
                0.500000f,
                0.750000f,
                0.866025f,
                0.500000f,
                0.000000f,
                0.500000f,
                0.000000f,
                0.866025f,
                0.433013f,
                0.500000f,
                0.750000f,
                0.500000f,
                0.000000f,
                0.866025f,
                -0.500000f,
                0.000000f,
                0.866025f,
                -0.433013f,
                0.500000f,
                0.750000f,
                0.433013f,
                0.500000f,
                0.750000f,
                -0.500000f,
                0.000000f,
                0.866025f,
                -0.433013f,
                0.500000f,
                0.750000f,
                -0.500000f,
                0.000000f,
                0.866025f,
                -1.000000f,
                0.000000f,
                0.000000f,
                -0.866025f,
                0.500000f,
                0.000000f,
                -0.433013f,
                0.500000f,
                0.750000f,
                -1.000000f,
                0.000000f,
                0.000000f,
                -0.866025f,
                0.500000f,
                0.000000f,
                -1.000000f,
                0.000000f,
                0.000000f,
                -0.500000f,
                0.000000f,
                -0.866025f,
                -0.433013f,
                0.500000f,
                -0.750000f,
                -0.866025f,
                0.500000f,
                0.000000f,
                -0.500000f,
                0.000000f,
                -0.866025f,
                -0.433013f,
                0.500000f,
                -0.750000f,
                -0.500000f,
                0.000000f,
                -0.866025f,
                0.500000f,
                0.000000f,
                -0.866025f,
                0.433013f,
                0.500000f,
                -0.750000f,
                -0.433013f,
                0.500000f,
                -0.750000f,
                0.500000f,
                0.000000f,
                -0.866025f,
                0.433013f,
                0.500000f,
                -0.750000f,
                0.500000f,
                0.000000f,
                -0.866025f,
                1.000000f,
                0.000000f,
                -0.000000f,
                0.866025f,
                0.500000f,
                -0.000000f,
                0.433013f,
                0.500000f,
                -0.750000f,
                1.000000f,
                0.000000f,
                -0.000000f,
                0.500000f,
                0.866025f,
                0.000000f,
                0.866025f,
                0.500000f,
                0.000000f,
                0.433013f,
                0.500000f,
                0.750000f,
                0.250000f,
                0.866025f,
                0.433013f,
                0.500000f,
                0.866025f,
                0.000000f,
                0.433013f,
                0.500000f,
                0.750000f,
                0.250000f,
                0.866025f,
                0.433013f,
                0.433013f,
                0.500000f,
                0.750000f,
                -0.433013f,
                0.500000f,
                0.750000f,
                -0.250000f,
                0.866025f,
                0.433013f,
                0.250000f,
                0.866025f,
                0.433013f,
                -0.433013f,
                0.500000f,
                0.750000f,
                -0.250000f,
                0.866025f,
                0.433013f,
                -0.433013f,
                0.500000f,
                0.750000f,
                -0.866025f,
                0.500000f,
                0.000000f,
                -0.500000f,
                0.866025f,
                0.000000f,
                -0.250000f,
                0.866025f,
                0.433013f,
                -0.866025f,
                0.500000f,
                0.000000f,
                -0.500000f,
                0.866025f,
                0.000000f,
                -0.866025f,
                0.500000f,
                0.000000f,
                -0.433013f,
                0.500000f,
                -0.750000f,
                -0.250000f,
                0.866025f,
                -0.433013f,
                -0.500000f,
                0.866025f,
                0.000000f,
                -0.433013f,
                0.500000f,
                -0.750000f,
                -0.250000f,
                0.866025f,
                -0.433013f,
                -0.433013f,
                0.500000f,
                -0.750000f,
                0.433013f,
                0.500000f,
                -0.750000f,
                0.250000f,
                0.866025f,
                -0.433013f,
                -0.250000f,
                0.866025f,
                -0.433013f,
                0.433013f,
                0.500000f,
                -0.750000f,
                0.250000f,
                0.866025f,
                -0.433013f,
                0.433013f,
                0.500000f,
                -0.750000f,
                0.866025f,
                0.500000f,
                -0.000000f,
                0.500000f,
                0.866025f,
                -0.000000f,
                0.250000f,
                0.866025f,
                -0.433013f,
                0.866025f,
                0.500000f,
                -0.000000f,
                0.000000f,
                1.000000f,
                0.000000f,
                0.500000f,
                0.866025f,
                0.000000f,
                0.250000f,
                0.866025f,
                0.433013f,
                0.000000f,
                1.000000f,
                0.000000f,
                0.000000f,
                1.000000f,
                0.000000f,
                0.250000f,
                0.866025f,
                0.433013f,
                0.000000f,
                1.000000f,
                0.000000f,
                0.250000f,
                0.866025f,
                0.433013f,
                -0.250000f,
                0.866025f,
                0.433013f,
                -0.000000f,
                1.000000f,
                0.000000f,
                0.000000f,
                1.000000f,
                0.000000f,
                -0.250000f,
                0.866025f,
                0.433013f,
                -0.000000f,
                1.000000f,
                0.000000f,
                -0.250000f,
                0.866025f,
                0.433013f,
                -0.500000f,
                0.866025f,
                0.000000f,
                -0.000000f,
                1.000000f,
                0.000000f,
                -0.000000f,
                1.000000f,
                0.000000f,
                -0.500000f,
                0.866025f,
                0.000000f,
                -0.000000f,
                1.000000f,
                0.000000f,
                -0.500000f,
                0.866025f,
                0.000000f,
                -0.250000f,
                0.866025f,
                -0.433013f,
                -0.000000f,
                1.000000f,
                -0.000000f,
                -0.000000f,
                1.000000f,
                0.000000f,
                -0.250000f,
                0.866025f,
                -0.433013f,
                -0.000000f,
                1.000000f,
                -0.000000f,
                -0.250000f,
                0.866025f,
                -0.433013f,
                0.250000f,
                0.866025f,
                -0.433013f,
                0.000000f,
                1.000000f,
                -0.000000f,
                -0.000000f,
                1.000000f,
                -0.000000f,
                0.250000f,
                0.866025f,
                -0.433013f,
                0.000000f,
                1.000000f,
                -0.000000f,
                0.250000f,
                0.866025f,
                -0.433013f,
                0.500000f,
                0.866025f,
                -0.000000f,
                0.000000f,
                1.000000f,
                -0.000000f,
                0.000000f,
                1.000000f,
                -0.000000f,
                0.500000f,
                0.866025f,
                -0.000000f
        };


        TEST_SSBO.fillBuffer(sphereVertices
        );


        ubo = new UBORenderInts(2);
    }
}

