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
        ProgramState.setArbitraryData("Scene", scene);


        GPUTimeQuerier timer = new GPUTimeQuerier();
        int passed_ticks = 0;
        int samples = 15;
        double accumulated_gpu_time = 0;
        double accumulated_cpu_time = 0;

        while (!window.shouldClose()) {
            long cpuStart = System.nanoTime();

            timer.startTimer();
            if (camera.updateCamera(window)) {
                drawer.resetRender();
            }
            drawer.renderFrame();
            long gpu_duration = timer.stopTimer();
            accumulated_gpu_time += (double) (gpu_duration) / 1_000_000;
            long cpuEnd = System.nanoTime();
            accumulated_cpu_time += (double) ((cpuEnd - cpuStart) - gpu_duration) / 1_000_000 ;
            passed_ticks++;
            if (passed_ticks == samples) {
                ProgramState.setArbitraryData("GPURenderTime", accumulated_gpu_time / samples);
                ProgramState.setArbitraryData("CPURenderTime", accumulated_cpu_time / samples);
                passed_ticks = 0;
                accumulated_gpu_time = 0;
                accumulated_cpu_time = 0;
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
