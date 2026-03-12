package org.pepetrace;

import static org.lwjgl.glfw.GLFW.*;

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

        GPUTimeQuerier timer = new GPUTimeQuerier();

        while (!window.shouldClose()) {
            //timer.startTimer();
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
