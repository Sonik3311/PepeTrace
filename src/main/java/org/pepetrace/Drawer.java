package org.pepetrace;

import static org.lwjgl.opengl.GL46.*;

import imgui.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import java.io.FileNotFoundException;

import org.pepetrace.Buffers.SSBO;
import org.pepetrace.Shader.ComputeProgram;
import org.pepetrace.Shader.Program;
import org.pepetrace.Buffers.Texture;

public class Drawer {

    private ImGuiImplGlfw imGuiGlfw;
    private ImGuiImplGl3 imGuiGl3;
    private Window window;

    private ComputeProgram pathTracingProgram;
    private Program windowTextureDrawerProgram;
    private int drawVAO;

    private SSBO TEST_SSBO;
    private UBORenderInts ubo;

    private int frame = 0;


    private Texture pathTracingTexture;

    public Drawer(Window window) throws FileNotFoundException {
        this.window = window;
        this.initImGUI();
        this.initGL();
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
        TEST_SSBO.fillBuffer(new float[]
                {1, 1, 0,
                 -1, -1, 0,
                 -1,1, 0,

                        -1, -1, 0,
                        1, 1, 0,
                        1, -1, 0

                  //1.1f, 1.0f, 1,
                  //1.1f,-1.0f, 0,
                  //-0.9f,-1.0f,-1,


                }
        );


        ubo = new UBORenderInts(2);
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
        //glBindBufferBase(GL_UNIFORM_BUFFER, 2, ubo.getId());
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
        //System.out.println(frame);
    }

    private void renderImGUI() {
        imGuiGl3.newFrame();
        imGuiGlfw.newFrame();
        ImGui.newFrame();

        // Здесь строится UI
        ImGui.showDemoWindow(); // Встроенное демо окно
        if (ImGui.button("Hello, World!")) {
            System.out.println("Button clicked!");
        }

        // Render ImGui
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }
}
