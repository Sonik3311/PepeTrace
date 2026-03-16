package org.pepetrace;

import static org.lwjgl.glfw.GLFW.*;

import org.joml.Vector3f;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.pepetrace.Util.GPUTimeQuerier;
import org.pepetrace.Util.Passport;

public class Main {

    private boolean isHardwareCompatible() {
        return true;
    }

    static void main() throws IOException {
        //Passport build = new Passport();
        Window window = new Window();
        window.setActive();

        Drawer drawer = new Drawer(window);
        Camera camera = new Camera();

        GPUTimeQuerier timer = new GPUTimeQuerier();

        while (!window.shouldClose()) {
            //timer.startTimer();
            //drawer.handleCameraInput();
            camera.updateCamera(window);
            drawer.renderFrame();
            //long duration = timer.stopTimer();
            //System.out.println(
            //    "Рендер занял " + (double) (duration) / 1000000 + " мс"
            //);

            glfwSwapBuffers(window.getId());

            glfwPollEvents();
        }

        glfwTerminate();
        System.out.println("Finished");
    }
}
