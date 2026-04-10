package org.pepetrace;

import static org.lwjgl.glfw.GLFW.*;

import org.joml.Vector3f;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.pepetrace.Scene.Scene;
import org.pepetrace.Util.GPUTimeQuerier;
import org.pepetrace.Util.Passport;

public class Main {

    private boolean isHardwareCompatible() {
        return true;
    }

    static void main() throws IOException {
        //Passport build = new Passport();
        Window window = new Window(512, 512, true, "smoll pepetrace");
        window.setActive();

        Drawer drawer = new Drawer(window);
        Camera camera = new Camera();
        drawer.setCamera(camera);

        Scene scene = new Scene();
        //scene.packTriangles(drawer.getTriangleBuffer());
        scene.packScene(drawer.getGeometryBuffer(), drawer.getMaterialIndicesBuffer(), drawer.getMaterialHandlesBuffer());

        GPUTimeQuerier timer = new GPUTimeQuerier();
        int passed_ticks = 0;
        double accumulated_time = 0;

        while (!window.shouldClose()) {
            timer.startTimer();
            if (camera.updateCamera(window)) {
                drawer.resetRender();
            }
            drawer.renderFrame();
            long duration = timer.stopTimer();
            accumulated_time += (double) (duration) / 1000000;
            passed_ticks++;
            if (passed_ticks == 5) {
                System.out.println(
                    "Рендер занял (accum)" + accumulated_time / 5 + " мс"
                );
                passed_ticks = 0;
                accumulated_time = 0;
            }
            System.out.println(
                    "Рендер занял " + (double) (duration) / 1000000 + " мс"
            );


            glfwSwapBuffers(window.getId());

            glfwPollEvents();
        }

        glfwTerminate();
        System.out.println("Finished");
    }
}
