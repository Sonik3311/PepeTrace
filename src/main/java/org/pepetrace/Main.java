package org.pepetrace;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL15.*;

import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiMouseCursor;
import org.pepetrace.Buffers.SSBO;
import org.pepetrace.Buffers.Texture;
import org.pepetrace.Drawers.ViewportDrawer;
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
    private final Camera viewportCamera;
    private final Scene scene;

    public Main() throws Exception {
        mainWindow = new Window(1024, 512, true, "Pepetrace");
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
    }

    public void refreshSceneBuffers(boolean packScene, boolean updateModelMatricies) {
        Scene scene = programState.getScene();
        if (packScene) scene.packScene(geometryBuffer, indexBuffer, materialIndicesBuffer, materialHandlesBuffer, triangleModelIndicesBuffer);
        if (updateModelMatricies) scene.updateModelMatricesOnGPU(modelMatricesBuffer);
        viewportDrawer.resetRender();
    }

    void main() {
        GPUTimeQuerier timer = new GPUTimeQuerier();
        while (!mainWindow.shouldClose()) {
            long cpuStart = System.nanoTime();

            if (viewportDrawer.draggingMouse && viewportCamera.getCameraMode() == 1) {
                mainWindow.setCursorMode(Window.CURSOR_DISABLED);
            } else if (viewportCamera.getCameraMode() == 1) mainWindow.setCursorMode(Window.CURSOR_NORMAL);

            if (viewportCamera.updateCamera(mainWindow, !viewportDrawer.draggingMouse && !(viewportCamera.getCameraMode() == 0))) {
                viewportDrawer.resetRender();
            }

            timer.startTimer();
            viewportDrawer.renderFrame();

            timer.stopTimerAsync();
            long cpuEnd = System.nanoTime();
            programState.setArbitraryData("CPURenderTime", (double) (cpuEnd - cpuStart) / 1_000_000);
            boolean isTimerReady = timer.isResultReady();
            if (isTimerReady) {
                long gpuTimeNs = timer.getResult();
                programState.setArbitraryData("GPURenderTime", (double) gpuTimeNs / 1_000_000);
            }

            glfwSwapBuffers(mainWindow.getId());
            glfwPollEvents();
        }

        //TODO
        //scene.close();
        //drawer.close();
        //camera.close();
        //window.close();
        glfwTerminate();
        System.out.println("Finished");
    }
}
