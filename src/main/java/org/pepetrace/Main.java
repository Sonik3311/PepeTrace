package org.pepetrace;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.ARBClearTexture.glClearTexImage;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30C.GL_RED_INTEGER;

import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiMouseCursor;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.pepetrace.Buffers.SSBO;
import org.pepetrace.Buffers.Texture;
import org.pepetrace.Drawers.RTDrawer;
import org.pepetrace.Drawers.ViewportDrawer;
import org.pepetrace.Scene.ModelMetadata;
import org.pepetrace.Scene.Scene;
import org.pepetrace.Util.GPUTimeQuerier;

import java.util.Set;

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
    private final Camera viewportCamera;
    private final Scene scene;

    private final Window renderWindow;
    private final RTDrawer renderDrawer;


    public Main() throws Exception {
        mainWindow = new Window(1024, 512, true, "Editor|Pepetrace");
        mainWindow.setActive();

        this.geometryBuffer = new SSBO(GL_STATIC_DRAW, 6);
        this.indexBuffer = new SSBO(GL_STATIC_DRAW, 7);
        this.materialIndicesBuffer = new SSBO(GL_STATIC_DRAW, 8);
        this.materialHandlesBuffer = new SSBO(GL_STATIC_DRAW, 9);
        this.modelMatricesBuffer = new SSBO(GL_DYNAMIC_DRAW, 10);
        this.triangleModelIndicesBuffer = new SSBO(GL_STATIC_DRAW, 11);
        this.skyboxTexture = Texture.createFromFile(
                4,
                true,
                GL_READ_ONLY,
                "./src/main/resources/Textures/grey_background.png"
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
        mainWindow.setCursorMode(Window.CURSOR_DISABLED);

        renderWindow = new Window(1024, 1024, true, "Render|Pepetrace", mainWindow);
        renderWindow.setActive();
        renderDrawer = new RTDrawer(renderWindow);

        mainWindow.setActive();
        mainWindow.show();
        renderWindow.show();
    }

    public void refreshSceneBuffers(boolean packScene, boolean updateModelMatrices) {
        Scene scene = programState.getScene();
        if (packScene) scene.packScene(geometryBuffer, indexBuffer, materialIndicesBuffer, materialHandlesBuffer, triangleModelIndicesBuffer);
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
        glClearTexImage(viewportDrawer.getOutputTexture().id, 0, GL_RGBA, GL_FLOAT, new float[]{0,0,0,1});
        glClearTexImage(viewportDrawer.getModelStencilTexture().id, 0, GL_RED_INTEGER, GL_UNSIGNED_INT, new int[]{0});
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

            if (viewportDrawer.draggingMouse && viewportCamera.getCameraMode() == 1) {
                mainWindow.setCursorMode(Window.CURSOR_NORMAL);
            } else if (viewportCamera.getCameraMode() == 1) mainWindow.setCursorMode(Window.CURSOR_NORMAL);

            // Определяем, нужно ли блокировать вращение камеры (вращаем модели)
            boolean rotatingModelsNow = !programState.getSelectedModelIndices().isEmpty() &&
                    (mainWindow.isKeyPressed(GLFW_KEY_LEFT_CONTROL) || mainWindow.isKeyPressed(GLFW_KEY_RIGHT_CONTROL)) &&
                    mainWindow.isMouseButtonPressed(Window.MOUSE_BUTTON_LEFT);
            boolean blockCameraRotation = rotatingModelsNow;

            // ----- Обработка скролла (до вызова updateCamera) -----
            double scroll = mainWindow.getScrollDelta();
            if (scroll != 0) {
                boolean ctrlPressed = mainWindow.isKeyPressed(GLFW_KEY_LEFT_CONTROL) || mainWindow.isKeyPressed(GLFW_KEY_RIGHT_CONTROL);
                if (!programState.getSelectedModelIndices().isEmpty() && ctrlPressed) {
                    // Перемещение выбранных моделей по глубине
                    Camera cam = viewportCamera;
                    float yawRad = (float) Math.toRadians(cam.getYawPitch().x);
                    float pitchRad = (float) Math.toRadians(cam.getYawPitch().y);
                    Vector3f forward = new Vector3f(
                            (float)(Math.cos(pitchRad) * Math.sin(yawRad)),
                            (float) Math.sin(pitchRad),
                            (float)(Math.cos(pitchRad) * Math.cos(yawRad))
                    ).normalize();
                    float depthMove = (float) scroll * 0.5f;
                    Vector3f worldDelta = new Vector3f(forward).mul(depthMove);
                    for (int idx : programState.getSelectedModelIndices()) {
                        ModelMetadata model = scene.getModels().get(idx);
                        model.setPosition(new Vector3f(model.getPosition()).add(worldDelta));
                    }
                    refreshSceneBuffers(false, true);
                    viewportDrawer.resetRender();
                } else {
                    // Передаём скролл камере
                    viewportCamera.processScroll(scroll);
                }
            }

            if (viewportCamera.updateCamera(mainWindow, !viewportDrawer.draggingMouse && !(viewportCamera.getCameraMode() == 0), blockCameraRotation)) {
                viewportDrawer.resetRender();
            }

            timer.startTimer();
            viewportDrawer.renderFrame();
            timer.stopTimerAsync();

            // ---------- F2: фокус на модель ----------
            if (mainWindow.isKeyPressed(GLFW_KEY_F2)) {
                Set<Integer> selectedForFocus = programState.getSelectedModelIndices();
                if (!selectedForFocus.isEmpty()) {
                    Scene scene = programState.getScene();
                    int firstIdx = selectedForFocus.iterator().next();
                    ModelMetadata model = scene.getModels().get(firstIdx);
                    Vector3f localCenter = scene.calculateModelCenter(firstIdx);
                    Vector3f worldCenter = new Vector3f(localCenter).mulPosition(model.getModelMatrix());
                    viewportCamera.focusOnModel(worldCenter);
                    viewportDrawer.resetRender();
                }
            }

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
                if (mainWindow.isKeyPressed(GLFW_KEY_LEFT))  deltaMove.x -= moveSpeed;
                if (mainWindow.isKeyPressed(GLFW_KEY_RIGHT)) deltaMove.x += moveSpeed;
                if (mainWindow.isKeyPressed(GLFW_KEY_UP))    deltaMove.y += moveSpeed;
                if (mainWindow.isKeyPressed(GLFW_KEY_DOWN))  deltaMove.y -= moveSpeed;

                if (deltaMove.x != 0 || deltaMove.y != 0) {
                    Camera cam = viewportCamera;
                    float yawRad = (float) Math.toRadians(cam.getYawPitch().x);
                    float pitchRad = (float) Math.toRadians(cam.getYawPitch().y);
                    Vector3f forward = new Vector3f(
                            (float)(Math.cos(pitchRad) * Math.sin(yawRad)),
                            (float) Math.sin(pitchRad),
                            (float)(Math.cos(pitchRad) * Math.cos(yawRad))
                    ).normalize();
                    Vector3f right = new Vector3f(forward).cross(new Vector3f(0,1,0)).normalize();
                    Vector3f up = new Vector3f(right).cross(forward).normalize();

                    Vector3f worldDelta = new Vector3f(right).mul(deltaMove.x)
                            .add(new Vector3f(up).mul(deltaMove.y));
                    for (int idx : selected) {
                        ModelMetadata model = scene.getModels().get(idx);
                        model.setPosition(new Vector3f(model.getPosition()).add(worldDelta));
                    }
                    needUpdate = true;
                }

                // Масштабирование клавишами +/-
                if (mainWindow.isKeyPressed(GLFW_KEY_KP_ADD) || mainWindow.isKeyPressed(GLFW_KEY_EQUAL)) {
                    for (int idx : selected) {
                        ModelMetadata model = scene.getModels().get(idx);
                        model.setScale(new Vector3f(model.getScale()).mul(1.05f));
                    }
                    needUpdate = true;
                }
                if (mainWindow.isKeyPressed(GLFW_KEY_KP_SUBTRACT) || mainWindow.isKeyPressed(GLFW_KEY_MINUS)) {
                    for (int idx : selected) {
                        ModelMetadata model = scene.getModels().get(idx);
                        model.setScale(new Vector3f(model.getScale()).mul(0.95f));
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
                            Quaternionf rot = new Quaternionf().rotateY(modelYaw).rotateX(modelPitch);
                            for (int idx : selected) {
                                ModelMetadata model = scene.getModels().get(idx);
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
            programState.setArbitraryData("CPURenderTime", (double) (cpuEnd - cpuStart) / 1_000_000);
            boolean isTimerReady = timer.isResultReady();
            if (isTimerReady) {
                long gpuTimeNs = timer.getResult();
                programState.setArbitraryData("GPURenderTime", (double) gpuTimeNs / 1_000_000);
            }

            glfwSwapBuffers(mainWindow.getId());


            renderWindow.setActive();
            if (renderWindow.shouldClose()) {
                renderWindow.hide();
            } else if (renderWindow.isVisible()) {
                renderDrawer.renderFrame();
                glfwSwapBuffers(renderWindow.getId());
            }


            glfwPollEvents();
        }

        glfwTerminate();
        System.out.println("Finished");
    }
}
