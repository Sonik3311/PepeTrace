package org.pepetrace;

import static org.lwjgl.glfw.GLFW.*;

import org.pepetrace.Scene.Scene;
import org.pepetrace.Util.GPUTimeQuerier;

public class Main {

    private boolean isHardwareCompatible() {
        return true;
    }

    static void main() throws Exception {
        //Passport build = new Passport();
        Window window = new Window(512, 512, true, "smoll pepetrace");
        window.setActive();

        Drawer drawer = new Drawer(window);
        Camera camera = new Camera();
        drawer.setCamera(camera);

        Scene scene = new Scene();
        //scene.packTriangles(drawer.getTriangleBuffer());
        scene.packScene(drawer.getGeometryBuffer(), drawer.getIndexBuffer(), drawer.getMaterialIndicesBuffer(), drawer.getMaterialHandlesBuffer());

        GlobalState ProgramState = GlobalState.getInstance();
        ProgramState.setScene(scene);
        ProgramState.setCamera(camera);
        ProgramState.setViewportDrawer(drawer);
        ProgramState.setArbitraryData("Scene", scene);


        GPUTimeQuerier timer = new GPUTimeQuerier();

        while (!window.shouldClose()) {
            long cpuStart = System.nanoTime();



            if (camera.updateCamera(window)) {
                drawer.resetRender();
            }

            timer.startTimer();
            drawer.renderFrame();

            timer.stopTimerAsync();
            long cpuEnd = System.nanoTime();
            ProgramState.setArbitraryData("CPURenderTime", (double) (cpuEnd - cpuStart) / 1_000_000);
            boolean isTimerReady = timer.isResultReady();
            if (isTimerReady) {
                long gpuTimeNs = timer.getResult();
                ProgramState.setArbitraryData("GPURenderTime", (double) gpuTimeNs / 1_000_000);
            }

            glfwSwapBuffers(window.getId());
            glfwPollEvents();
        }

        scene.close();
        drawer.close();
        camera.close();
        window.close();
        glfwTerminate();
        System.out.println("Finished");
    }
}
