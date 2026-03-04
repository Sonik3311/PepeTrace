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

        ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
        ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
        // 1. Create the ImGui context
        ImGui.createContext();

        // 2. Set basic configuration (optional)
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null); // Disable .ini file to avoid saving state
        io.setDisplaySize(1024, 768); // Set your initial window size
        io.getFonts().addFontDefault(); // Load default font

        // 3. Initialize the GLFW and OpenGL 3 bindings
        imGuiGlfw.init(window.id, true); // The boolean is for integrating the callbacks
        imGuiGl3.init("#version 460"); // Your GLSL version




        // TODO: Перекинуть всё в класс Drawer (кроме цикла while)

        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        System.out.println("Working Directory = " + System.getProperty("user.dir"));


        Texture texture = new Texture(1600, 900, GL_RGBA, GL_FLOAT, GL_RGBA32F);

        int[] max_work_group = ComputeProgram.getMaxWorkGroupCount();
        System.out.println("Max supported work group: x:" + max_work_group[0] + " y:" + max_work_group[1] + " z:" + max_work_group[2]);
        ComputeProgram computeShader = new ComputeProgram("./src/main/glsl/program");

        Program screenQuad = new Program("./src/main/glsl/screenQuad");

        int empty_vao = glGenVertexArrays();


        int[] queries = {0, 0};
        glGenQueries(queries);

        while (!glfwWindowShouldClose(window.id)) {

            // 1. Запуск compute шейдера
            glQueryCounter(queries[0], GL_TIMESTAMP);
            computeShader.use();
            glDispatchCompute(1600 / 8, (int) 113, 1);
            glQueryCounter(queries[1], GL_TIMESTAMP);

            int[] params = {0};
            while(params[0] == GL_FALSE) {
                glGetQueryObjectiv(queries[1], GL_QUERY_RESULT_AVAILABLE, params);
            }

            long[] startTime = {0};
            long[] endTime = {0};
            glGetQueryObjectui64v(queries[0], GL_QUERY_RESULT, startTime);
            glGetQueryObjectui64v(queries[1], GL_QUERY_RESULT, endTime);
            System.out.println("Compute Shader занял " + ((double) (endTime[0] - startTime[0]) / 1000000.0) + " мс");

            //System.out.println(end_time - start_time);

            // 2. Барьер памяти - важно для синхронизации
            glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL_TEXTURE_FETCH_BARRIER_BIT);

            // 3. Рендеринг квада
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // чистим прошлый фрэймбуффер (опционально)
            screenQuad.use();
            screenQuad.setInt("tex", 0);

            // Убедимся, что текстура привязана (опционально)
            glBindVertexArray(empty_vao);
            glDrawArrays(GL_TRIANGLES, 0, 3);

            // Start a new ImGui frame
            imGuiGl3.newFrame();
            imGuiGlfw.newFrame();
            ImGui.newFrame();

            // Build your UI
            ImGui.showDemoWindow(); // Show the built-in demo window
            if (ImGui.button("Hello, World!")) {
                System.out.println("Button clicked!");
            }

            // Render ImGui
            ImGui.render();
            imGuiGl3.renderDrawData(ImGui.getDrawData());


            glfwSwapBuffers(window.id);

            glfwPollEvents();
        }

        glfwTerminate();
        System.out.println("Finished");
    }
}
