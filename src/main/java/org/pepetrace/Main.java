package org.pepetrace;

import org.pepetrace.Shader.ComputeProgram;
import org.pepetrace.Shader.Program;
import org.pepetrace.Shader.Texture;

import java.io.FileNotFoundException;

import static java.lang.Math.ceil;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import imgui.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;


public class Main {



    private boolean isHardwareCompatible() {
        return true;
    }

    static void main() throws FileNotFoundException {

        Window window = new Window();
        window.setActive();

        Drawer drawer = new Drawer(window);

        GPUTimeQuerier timer = new GPUTimeQuerier();

        while (!window.shouldClose()) {
            timer.startTimer();
            drawer.renderFrame();
            long duration = timer.stopTimer();
            System.out.println("Рендер занял " + (double) (duration) / 1000000 + " мс");

            glfwSwapBuffers(window.getId());

            glfwPollEvents();
        }

        glfwTerminate();
        System.out.println("Finished");
    }
}
