package org.pepetrace.Drawers;

import org.pepetrace.Shader.Program;
import org.pepetrace.Window;

import java.io.FileNotFoundException;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class RTDrawer extends AbstractDrawer {

    private final Program testrenderer = new Program("./src/main/glsl/renderers/RT/Test/test");
    private final int vao = glGenVertexArrays();

    public RTDrawer(Window window) throws FileNotFoundException {
        super(window);
    }

    @Override
    public void renderFrame() {
        glViewport(0, 0, currentWidth, currentHeight);
        testrenderer.use();
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);

        frameId++;
    }
}
