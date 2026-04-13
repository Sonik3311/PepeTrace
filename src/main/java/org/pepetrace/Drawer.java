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
import java.util.Arrays;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.pepetrace.Buffers.SSBO;
import org.pepetrace.Buffers.Texture;
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
    private ImInt renderMode = new ImInt(ViewportRenderMode.SHADED.ordinal());
    private UBORenderInts ubo;
    private int frame = 0;
    private ImInt samples = new ImInt(5);
    private ImInt reflections = new ImInt(2);
    private ImBoolean accumulating = new ImBoolean(false);
    private ImFloat roughness = new ImFloat(1.0f);
    private Texture pathTracingTexture;
    private final Texture skybox = Texture.createFromFile(
        4,
        true,
        GL_READ_ONLY,
        "./src/main/resources/Textures/grey_background.png"
    );
    private int currentWidth;
    private int currentHeight;

    private GlobalState programState = GlobalState.getInstance();

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
        imGuiGl3.newFrame();
        imGuiGlfw.newFrame();
        ImGui.newFrame();
        // Меню сверху
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("Save", "Ctrl+S")) {
                    // handle save
                }

                ImGui.endMenu();
            }if (ImGui.beginMenu("Edit")) {
                if (ImGui.menuItem("Open Model", "Ctrl+K+O")) {
                    Scene scene = (Scene) programState.getArbitraryData("Scene");

                    try (MemoryStack stack = MemoryStack.stackPush()) {
                        // 1. Окно сообщения (title, message, dialogType, iconType, defaultButton)
                        tinyfd_messageBox("Внимание", "Продолжить выполнение?", "yesno", "question", 1);
                        // 1. Создаем буфер указателей на строки расширений
                        PointerBuffer filters = stack.mallocPointer(2); // Резервируем место под 3 фильтра
                        filters.put(stack.UTF8("*.obj"));
                        filters.put(stack.UTF8("*.fbx"));
                        filters.flip(); // Переключаем буфер в режим чтения

                        // 2. Вызываем диалог, передав наш буфер
                        String filePath = tinyfd_openFileDialog(
                                "Выберите модель",
                                "~",
                                filters,            // Передаем список фильтров
                                "Модели",      // Описание (будет видно в выпадающем списке)
                                false
                        );

                        if (filePath != null) {
                            System.out.println("Выбран: " + filePath);
                        }
                    }
                }
                // edit menu items
                ImGui.endMenu();
            }
            // ... other menus
            ImGui.endMainMenuBar();
        }
        // Основное пространство


        ImGui.dockSpaceOverViewport();
        ImGui.begin("Build info", ImGuiWindowFlags.NoCollapse);
        ImGui.text(String.format("Build No. %s", Passport.INSTANCE.getBuildNumber()));
        ImGui.text(String.format("OS: %s", Passport.INSTANCE.getBuildOS()));
        ImGui.text(String.format("Build timestamp: %s", Passport.INSTANCE.getBuildTime()));
        ImGui.text(String.format("Java: %s", Passport.INSTANCE.getJavaVersion()));
        ImGui.text(String.format("Git branch: %s", Passport.INSTANCE.getGitBranchHash()));
        ImGui.end();

        ImGui.begin("Camera Info", ImGuiWindowFlags.NoCollapse);
        if (camera != null) {
            ImGui.text(
                    String.format(
                            "Mode: %s",
                            camera.getCameraMode() == 0 ? "Free" : "Orbit"
                    )
            );
            Vector3f pos = camera.getPosition();
            Vector2f rot = camera.getYawPitch();
            ImGui.text(String.format("Pos: (%.2f, %.2f, %.2f)", pos.x, pos.y, pos.z));
            ImGui.text(String.format("Yaw: %.2f Pitch: %.2f", rot.x, rot.y));
        }
        ImGui.end();

        ImGui.begin("Viewport");
        float renderViewportWidth = ImGui.getContentRegionAvailX();
        float renderViewportHeight = ImGui.getContentRegionAvailY();
        float windowPosX = ImGui.getWindowPosX();
        float windowPosY = ImGui.getWindowPosY();

        if (sizeChanged((int) renderViewportWidth, (int) renderViewportHeight)) {
            onResize((int) renderViewportWidth, (int) renderViewportHeight); // Это ужас, нужно править путём создания отдельного метода. Но оно работает и норм.
        }
        ImGui.image(pathTracingTexture.id, renderViewportWidth, renderViewportHeight, 0, 1, 1, 0);
        ImDrawList drawList = ImGui.getWindowDrawList();
        //drawList.addText(windowPosX + 100, windowPosY + 100, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), "Hello, Image!");
        ImDrawList dl = ImGui.getWindowDrawList();
        float x = 13 + windowPosX, y = 30 + windowPosY;
        float xc = x + 0, yc = y + 20;
        float xt = x + 0, yt = yc + 20;

        double gpu = (double) programState.getArbitraryData("GPURenderTime");
        double cpu = (double) programState.getArbitraryData("CPURenderTime");
        int triangleCount = ((Scene) programState.getArbitraryData("Scene")).getTriangleCount();
        String gpuvalue = String.format("%.2f", Math.floor(gpu * 100) / 100);
        String cpuvalue = String.format("%.2f", Math.floor(cpu * 100) / 100);
        String trivalue = triangleCount >= 1000 ? String.format("%.2f", Math.floor((float) triangleCount / 1000 * 100) / 100) + "k" : Integer.toString(triangleCount);
        String gputext = "GPU Render Time: " + gpuvalue + " ms";
        String cputext = "CPU Render Time: " + cpuvalue + " ms";
        String tritext = "Triangle Count: " + trivalue;


        int textColor = ImGui.getColorU32(1,1,1,1);   // white
        int outlineColor = ImGui.getColorU32(0,0,0,1); // black
        int thickness = 2;
        for (int dx = -thickness; dx <= thickness; dx++) {
            for (int dy = -thickness; dy <= thickness; dy++) {
                // Skip the center (where the main text will go)
                if (dx == 0 && dy == 0) continue;
                dl.addText(xc + dx, yc + dy, outlineColor, cputext);
            }
        }
        dl.addText(xc, yc, textColor, cputext);
        for (int dx = -thickness; dx <= thickness; dx++) {
            for (int dy = -thickness; dy <= thickness; dy++) {
                // Skip the center (where the main text will go)
                if (dx == 0 && dy == 0) continue;
                dl.addText(x + dx, y + dy, outlineColor, gputext);
            }
        }
        dl.addText(x, y, textColor, gputext);
        dl.addText(xc, yc, textColor, cputext);
        for (int dx = -thickness; dx <= thickness; dx++) {
            for (int dy = -thickness; dy <= thickness; dy++) {
                // Skip the center (where the main text will go)
                if (dx == 0 && dy == 0) continue;
                dl.addText(xt + dx, yt + dy, outlineColor, tritext);
            }
        }
        dl.addText(xt, yt, textColor, tritext);

        ImGui.end();

        ImGui.begin("Render Settings");
        if (ImGui.inputInt("Samples", samples)) {
            int min = 1, max = 16384;
            int clamped = Math.clamp(samples.get(), min, max);
            samples.set(clamped);
            frame = 0;
        }
        if (ImGui.checkbox("Accumulate frames", accumulating)) {
            if (!accumulating.get()) {
                frame = 0;
            }
        }
        if (ImGui.button("Reset Accumulation")) {
            frame = 0;
        }
        String[] modeNames = Arrays.stream(ViewportRenderMode.values())
                .map(Enum::name)
                .toArray(String[]::new);

        // Render the combo box
        if (ImGui.combo("Render Mode", renderMode, modeNames)) {
            // Update the enum based on the selected index
            frame = 0;
            //renderMode.set(ViewportRenderMode.values()[currentIdx];
            //System.out.println(renderMode.get());
        }
        ImGui.end();

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
        /**imGuiGl3.newFrame();
        imGuiGlfw.newFrame();
        ImGui.newFrame();
        int windowFlags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse |
                ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove |
                ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoNavFocus |
                ImGuiWindowFlags.NoDocking;

        //if (cursorLocked) {
        //    ImGui.getIO().setMousePos(-Float.MAX_VALUE, -Float.MAX_VALUE);
        //}

        // Здесь строится UI
        //ImGui.showDemoWindow(); // Встроенное демо окно

        //ImGui.setNextWindowSize(300, 150, ImGuiCond.FirstUseEver);

        // Меню сверху
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("Save", "Ctrl+S")) {
                    // handle save
                }
                ImGui.endMenu();
            }
            if (ImGui.beginMenu("Edit")) {
                // edit menu items
                ImGui.endMenu();
            }
            // ... other menus
            ImGui.endMainMenuBar();
        }

        // Создаём основное the main docking space, covering the rest of the window
        ImGuiViewport viewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(viewport.getWorkPos());
        ImGui.setNextWindowSize(viewport.getWorkSize());
        ImGui.setNextWindowViewport(viewport.getID());
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f);
        windowFlags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse |
                ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove |
                ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoNavFocus |
                ImGuiWindowFlags.NoDocking;

        ImGui.begin("MainDockspace", windowFlags);
        ImGui.popStyleVar(2);
        int dockSpaceId = ImGui.getID("MyDockSpace");
        ImGui.dockSpace(dockSpaceId, 0.0f, 0.0f, ImGuiDockNodeFlags.PassthruCentralNode);

        if (!layoutInitialized) {
            setupDockLayout(dockSpaceId);
            layoutInitialized = true;
        }
        ImGui.end();



        ImGui.setNextWindowPos(0, 50, ImGuiCond.FirstUseEver);
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

        ImGui.setNextWindowPos(0, 240, ImGuiCond.FirstUseEver);
        ImGui.begin("Render Settings", windowFlags);
        if (ImGui.inputInt("Samples", samples)) {
            int min = 1,
                max = 16384;
            int clamped = Math.clamp(samples.get(), min, max);
            samples.set(clamped);
            frame = 0;
        }
        if (ImGui.inputInt("Reflections", reflections)) {
            int min = 1,
                max = 16384;
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
            int min = 0,
                max = 1;
            float clamped = Math.clamp(roughness.get(), min, max);
            roughness.set(clamped);
            frame = 0;
        }
        if (ImGui.button("Reset Accumulation")) {
            frame = 0;
        }

        String[] modeNames = Arrays.stream(ViewportRenderMode.values())
            .map(Enum::name)
            .toArray(String[]::new);

        // Render the combo box
        if (ImGui.combo("Render Mode", renderMode, modeNames)) {
            // Update the enum based on the selected index
            //renderMode.set(ViewportRenderMode.values()[currentIdx];
            //System.out.println(renderMode.get());
        }
        ImGui.end();

        // Render ImGui
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());*/
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
