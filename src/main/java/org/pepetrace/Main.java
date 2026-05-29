package org.pepetrace;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.ARBClearTexture.glClearTexImage;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30C.GL_RED_INTEGER;
import static org.lwjgl.opengl.GL46.GL_READ_ONLY;
import static org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog;

import java.util.Set;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.pepetrace.Buffers.SSBO;
import org.pepetrace.Buffers.Texture;
import org.pepetrace.Drawers.RTDrawer;
import org.pepetrace.Drawers.ViewportDrawer;
import org.pepetrace.Scene.ModelMetadata;
import org.pepetrace.Scene.Scene;
import org.pepetrace.Util.GPUTimeQuerier;

public class Main {

    private final SSBO geometryBuffer;
    private final SSBO indexBuffer;
    private final SSBO materialIndicesBuffer;
    private final SSBO materialHandlesBuffer;
    private final SSBO modelMatricesBuffer;
    private final SSBO triangleModelIndicesBuffer;
    private Texture skyboxTexture;

    private final Window mainWindow;
    private final GlobalState programState;
    private final ViewportDrawer viewportDrawer;
    final Camera viewportCamera;
    private final Scene scene;

    final Window renderWindow;
    final RTDrawer renderDrawer;
    private boolean wasF12Pressed = false;
    private int f12Cooldown = 0;
    boolean testF12 = false;

    public Main() throws Exception {
        mainWindow = new Window(1024, 512, true, "Editor|Pepetrace");
        mainWindow.setActive();

        this.geometryBuffer = new SSBO(GL_STATIC_DRAW, 6);
        this.indexBuffer = new SSBO(GL_STATIC_DRAW, 7);
        this.materialIndicesBuffer = new SSBO(GL_STATIC_DRAW, 8);
        this.materialHandlesBuffer = new SSBO(GL_STATIC_DRAW, 9);
        this.modelMatricesBuffer = new SSBO(GL_DYNAMIC_DRAW, 10);
        this.triangleModelIndicesBuffer = new SSBO(GL_STATIC_DRAW, 11);
        this.skyboxTexture = Texture.createFromResourceHDR(
            4,
            true,
            GL_READ_ONLY,
            "/sunny_rose_garden_2k.hdr"
        );

        modelMatricesBuffer.fillBuffer(new float[0]);
        triangleModelIndicesBuffer.fillBuffer(new int[0]);

        viewportDrawer = new ViewportDrawer(mainWindow);
        viewportCamera = new Camera();
        viewportDrawer.setCamera(viewportCamera);

        scene = new Scene();

        programState = GlobalState.getInstance();
        programState.setScene(scene);
        programState.setCamera(viewportCamera);
        programState.setViewportDrawer(viewportDrawer);
        programState.setArbitraryData("Scene", scene);
        programState.setArbitraryData("skyboxTexture", skyboxTexture.id);
        programState.setArbitraryData("Main", this);
        programState.setArbitraryData("geometryBuffer", geometryBuffer);
        programState.setArbitraryData("indexBuffer", indexBuffer);
        programState.setArbitraryData(
            "materialIndicesBuffer",
            materialIndicesBuffer
        );
        programState.setArbitraryData(
            "materialHandlesBuffer",
            materialHandlesBuffer
        );
        programState.setArbitraryData(
            "modelMatricesBuffer",
            modelMatricesBuffer
        );
        programState.setArbitraryData(
            "triangleModelIndicesBuffer",
            triangleModelIndicesBuffer
        );
        mainWindow.setCursorMode(Window.CURSOR_DISABLED);

        renderWindow = new Window(
            1024,
            1024,
            true,
            "Render|Pepetrace",
            mainWindow,
            false
        );
        renderWindow.makeCurrent();
        renderDrawer = new RTDrawer(renderWindow);
        renderDrawer.initRender(1024, 1024, 1, 2, 256);

        mainWindow.setActive();
        mainWindow.show();
    }

    public void refreshSceneBuffers(
        boolean packScene,
        boolean updateModelMatrices
    ) {
        Scene scene = programState.getScene();
        if (packScene) scene.packScene(
            geometryBuffer,
            indexBuffer,
            materialIndicesBuffer,
            materialHandlesBuffer,
            triangleModelIndicesBuffer
        );
        if (updateModelMatrices) {
            scene.updateModelMatricesOnGPU(modelMatricesBuffer);
            // Пересчитываем мировые AABB для всех моделей (или только для выбранных)
            for (ModelMetadata model : scene.getModels()) {
                model.updateWorldAABB();
            }
            scene.updateTLAS();
        }
    }

    public void forceClearRender() {
        // Принудительно очищаем выходную текстуру и stencil чёрным цветом
        glClearTexImage(
            viewportDrawer.getOutputTexture().id,
            0,
            GL_RGBA,
            GL_FLOAT,
            new float[] {0, 0, 0, 1}
        );
        glClearTexImage(
            viewportDrawer.getModelStencilTexture().id,
            0,
            GL_RED_INTEGER,
            GL_UNSIGNED_INT,
            new int[] {0}
        );
        // Сбрасываем накопление кадров
        viewportDrawer.resetRender();
        // Принудительная синхронизация
        glFinish();
    }

    void main() throws Exception {
        GPUTimeQuerier timer = new GPUTimeQuerier();
        boolean wasRotatingModels = false;
        // Для накопления углов вращения моделей (глобально для всех выбранных)
        float modelYaw = 0.0f;
        float modelPitch = 0.0f;
        while (!mainWindow.shouldClose()) {
            mainWindow.setActive();
            long cpuStart = System.nanoTime();

            if (
                viewportDrawer.draggingMouse &&
                viewportCamera.getCameraMode() == 1
            ) {
                mainWindow.setCursorMode(Window.CURSOR_NORMAL);
            } else if (
                viewportCamera.getCameraMode() == 1
            ) mainWindow.setCursorMode(Window.CURSOR_NORMAL);

            // Определяем, нужно ли блокировать вращение камеры (вращаем модели)
            boolean rotatingModelsNow =
                !programState.getSelectedModelIndices().isEmpty() &&
                (mainWindow.isKeyPressed(GLFW_KEY_LEFT_CONTROL) ||
                    mainWindow.isKeyPressed(GLFW_KEY_RIGHT_CONTROL)) &&
                mainWindow.isMouseButtonPressed(Window.MOUSE_BUTTON_LEFT);

            // ----- Обработка скролла (до вызова updateCamera) -----
            double scroll = mainWindow.getScrollDelta();
            if (scroll != 0) {
                boolean ctrlPressed =
                    mainWindow.isKeyPressed(GLFW_KEY_LEFT_CONTROL) ||
                    mainWindow.isKeyPressed(GLFW_KEY_RIGHT_CONTROL);
                if (
                    !programState.getSelectedModelIndices().isEmpty() &&
                    ctrlPressed
                ) {
                    // Перемещение выбранных моделей по глубине
                    Camera cam = viewportCamera;
                    float yawRad = (float) Math.toRadians(cam.getYawPitch().x);
                    float pitchRad = (float) Math.toRadians(
                        cam.getYawPitch().y
                    );
                    Vector3f forward = new Vector3f(
                        (float) (Math.cos(pitchRad) * Math.sin(yawRad)),
                        (float) Math.sin(pitchRad),
                        (float) (Math.cos(pitchRad) * Math.cos(yawRad))
                    ).normalize();
                    float depthMove = (float) scroll * 0.5f;
                    Vector3f worldDelta = new Vector3f(forward).mul(depthMove);
                    for (int idx : programState.getSelectedModelIndices()) {
                        ModelMetadata model = scene.getModels().get(idx);
                        model.setPosition(
                            new Vector3f(model.getPosition()).add(worldDelta)
                        );
                    }
                    refreshSceneBuffers(false, true);
                    viewportDrawer.resetRender();
                } else {
                    // Передаём скролл камере
                    viewportCamera.processScroll(scroll);
                }
            }

            if (
                viewportCamera.updateCamera(
                    mainWindow,
                    !viewportDrawer.draggingMouse &&
                        !(viewportCamera.getCameraMode() == 0),
                        rotatingModelsNow
                )
            ) {
                viewportDrawer.resetRender();
            }

            if (!renderWindow.isVisible()) {
                timer.startTimer();
                viewportDrawer.renderFrame();
                timer.stopTimerAsync();
            } else {
                viewportDrawer.renderFrame(true);
            }

            // ---------- F2: фокус на модель ----------
            if (mainWindow.isKeyPressed(GLFW_KEY_F2)) {
                Set<Integer> selectedForFocus =
                    programState.getSelectedModelIndices();
                if (!selectedForFocus.isEmpty()) {
                    Scene scene = programState.getScene();
                    int firstIdx = selectedForFocus.iterator().next();
                    ModelMetadata model = scene.getModels().get(firstIdx);
                    Vector3f localCenter = scene.calculateModelCenter(firstIdx);
                    Vector3f worldCenter = new Vector3f(
                        localCenter
                    ).mulPosition(model.getModelMatrix());
                    viewportCamera.focusOnModel(worldCenter);
                    viewportDrawer.resetRender();
                }
            }

            handleF12(mainWindow.isKeyPressed(GLFW_KEY_F12) || testF12);
            testF12 = false;

            // ---------- ESC: снять выделение ----------
            if (mainWindow.isKeyPressed(GLFW_KEY_ESCAPE)) {
                programState.clearSelectedModels();
            }

            // ---------- Трансформации выбранных моделей (стрелки, +/-, вращение) ----------
            Set<Integer> selected = programState.getSelectedModelIndices();
            if (!selected.isEmpty()) {
                float moveSpeed = 0.1f;
                Vector3f deltaMove = new Vector3f(0, 0, 0);
                boolean needUpdate = false;

                // Перемещение стрелками в плоскости экрана
                if (mainWindow.isKeyPressed(GLFW_KEY_LEFT)) deltaMove.x -=
                    moveSpeed;
                if (mainWindow.isKeyPressed(GLFW_KEY_RIGHT)) deltaMove.x +=
                    moveSpeed;
                if (mainWindow.isKeyPressed(GLFW_KEY_UP)) deltaMove.y +=
                    moveSpeed;
                if (mainWindow.isKeyPressed(GLFW_KEY_DOWN)) deltaMove.y -=
                    moveSpeed;

                if (deltaMove.x != 0 || deltaMove.y != 0) {
                    Camera cam = viewportCamera;
                    float yawRad = (float) Math.toRadians(cam.getYawPitch().x);
                    float pitchRad = (float) Math.toRadians(
                        cam.getYawPitch().y
                    );
                    Vector3f forward = new Vector3f(
                        (float) (Math.cos(pitchRad) * Math.sin(yawRad)),
                        (float) Math.sin(pitchRad),
                        (float) (Math.cos(pitchRad) * Math.cos(yawRad))
                    ).normalize();
                    Vector3f right = new Vector3f(forward)
                        .cross(new Vector3f(0, 1, 0))
                        .normalize();
                    Vector3f up = new Vector3f(right)
                        .cross(forward)
                        .normalize();

                    Vector3f worldDelta = new Vector3f(right)
                        .mul(deltaMove.x)
                        .add(new Vector3f(up).mul(deltaMove.y));
                    for (int idx : selected) {
                        ModelMetadata model = scene.getModels().get(idx);
                        model.setPosition(
                            new Vector3f(model.getPosition()).add(worldDelta)
                        );
                    }
                    needUpdate = true;
                }

                // Масштабирование клавишами +/-
                if (
                    mainWindow.isKeyPressed(GLFW_KEY_KP_ADD) ||
                    mainWindow.isKeyPressed(GLFW_KEY_EQUAL)
                ) {
                    for (int idx : selected) {
                        ModelMetadata model = scene.getModels().get(idx);
                        model.setScale(
                            new Vector3f(model.getScale()).mul(1.05f)
                        );
                    }
                    needUpdate = true;
                }
                if (
                    mainWindow.isKeyPressed(GLFW_KEY_KP_SUBTRACT) ||
                    mainWindow.isKeyPressed(GLFW_KEY_MINUS)
                ) {
                    for (int idx : selected) {
                        ModelMetadata model = scene.getModels().get(idx);
                        model.setScale(
                            new Vector3f(model.getScale()).mul(0.95f)
                        );
                    }
                    needUpdate = true;
                }

                // Вращение моделей при Ctrl+ЛКМ (плавное, с накоплением углов)
                if (rotatingModelsNow) {
                    float[] mouseDelta = mainWindow.getMouseDelta();
                    if (!wasRotatingModels) {
                        wasRotatingModels = true;
                        // Первый кадр – просто запоминаем начало, не меняем углы
                    } else {
                        float rotSpeed = 0.005f;
                        modelYaw -= mouseDelta[0] * rotSpeed;
                        modelPitch += mouseDelta[1] * rotSpeed;
                        if (mouseDelta[0] != 0 || mouseDelta[1] != 0) {
                            Quaternionf rot = new Quaternionf()
                                .rotateY(modelYaw)
                                .rotateX(modelPitch);
                            for (int idx : selected) {
                                ModelMetadata model = scene
                                    .getModels()
                                    .get(idx);
                                model.setRotation(rot);
                            }
                            needUpdate = true;
                        }
                    }
                } else {
                    wasRotatingModels = false;
                }

                if (needUpdate) {
                    refreshSceneBuffers(false, true);
                }
            }

            long cpuEnd = System.nanoTime();
            programState.setArbitraryData(
                "CPURenderTime",
                (double) (cpuEnd - cpuStart) / 1_000_000
            );
            boolean isTimerReady = timer.isResultReady();
            if (isTimerReady) {
                long gpuTimeNs = timer.getResult();
                programState.setArbitraryData(
                    "GPURenderTime",
                    (double) gpuTimeNs / 1_000_000
                );
            }

            glfwSwapBuffers(mainWindow.getId());

            handleRenderWindow();

            // Восстанавливаем контекст основного окна для pollEvents
            mainWindow.makeCurrent();

            glfwPollEvents();
        }

        glfwTerminate();
        System.out.println("Finished");
    }

    void handleF12(boolean f12Pressed) {
        if (f12Pressed && !wasF12Pressed && f12Cooldown == 0) {
            var settings = viewportDrawer.getRenderSettings();
            renderWindow.resetCloseFlag();
            renderWindow.makeCurrent();
            renderDrawer.initRender(
                settings.rtWidth,
                settings.rtHeight,
                settings.rtSamples.get(),
                settings.rtBounces.get(),
                settings.rtMaxSpp.get()
            );
            renderDrawer.copyCameraFrom(viewportCamera);
            renderDrawer.resetRender();
            mainWindow.makeCurrent();
            renderWindow.show();
            f12Cooldown = 3;
        }
        wasF12Pressed = f12Pressed;
        if (f12Cooldown > 0) f12Cooldown--;
    }

    public void startRender() {
        handleF12(true);
        handleF12(false);
    }

    public void resetRender() {
        renderDrawer.resetRender();
    }

    public void loadSkyboxHDR() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(2);
            filters.put(stack.UTF8("*.hdr"));
            filters.put(stack.UTF8("*.exr"));
            filters.flip();
            String path = tinyfd_openFileDialog("Load Skybox HDR", "~", filters, "HDR Images", false);
            if (path != null) {
                if (skyboxTexture != null) skyboxTexture.close();
                skyboxTexture = Texture.createFromFileHDR(4, true, GL_READ_ONLY, path);
                programState.setArbitraryData("skyboxTexture", skyboxTexture.id);
                renderDrawer.rebindSkybox(skyboxTexture.id);
                viewportDrawer.resetRender();
            }
        }
    }

    public void loadSkybox() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.png"));
            filters.flip();
            String path = tinyfd_openFileDialog("Load Skybox", "~", filters, "PNG Images", false);
            if (path != null) {
                if (skyboxTexture != null) skyboxTexture.close();
                skyboxTexture = Texture.createFromFile(4, true, GL_READ_ONLY, path);
                programState.setArbitraryData("skyboxTexture", skyboxTexture.id);
                renderDrawer.rebindSkybox(skyboxTexture.id);
                viewportDrawer.resetRender();
            }
        }
    }

    void handleRenderWindow() {
        if (renderWindow.isVisible()) {
            renderWindow.setActive();
            glfwPollEvents();
            if (renderWindow.shouldClose()) {
                renderWindow.pacedHide();
            } else {
                renderDrawer.renderFrame();
                glfwSwapBuffers(renderWindow.getId());
            }
        }
    }

    // Для модуль-тестов.
    void runOneFrame() {
        mainWindow.setActive();

        if (!renderWindow.isVisible()) {
            viewportDrawer.renderFrame();
        } else {
            viewportDrawer.renderFrame(true);
        }

        handleF12(mainWindow.isKeyPressed(GLFW_KEY_F12) || testF12);
        testF12 = false;

        glfwSwapBuffers(mainWindow.getId());
        handleRenderWindow();
        mainWindow.makeCurrent();
        glfwPollEvents();
    }
}
