package org.pepetrace;

import org.pepetrace.Shader.ComputeProgram;
import org.pepetrace.Shader.Program;
import org.pepetrace.Shader.Texture;

import java.io.FileNotFoundException;

import static java.lang.Math.ceil;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Main {

    private boolean isHardwareCompatible() {
        return true;
    }

    static void main() throws FileNotFoundException {

        Window window = new Window();
        window.setActive();

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
            //glActiveTexture(GL_TEXTURE0);
            //glBindTexture(GL_TEXTURE_2D, texture.id);

            glBindVertexArray(empty_vao);
            glDrawArrays(GL_TRIANGLES, 0, 3);

            glfwSwapBuffers(window.id);

            glfwPollEvents();
        }

        glfwTerminate();
        System.out.println("Finished");
    }
}
