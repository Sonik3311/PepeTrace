package org.pepetrace.Drawers;

import org.pepetrace.Buffers.Texture;
import org.pepetrace.Shader.Program;
import org.pepetrace.Shader.ComputeProgram;
import org.pepetrace.UBOCamera;
import org.pepetrace.UBORTSettings;
import org.pepetrace.Window;

import java.io.FileNotFoundException;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15C.GL_READ_WRITE;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30C.GL_RGBA32F;

public class RTDrawer extends AbstractDrawer {

    private final Program testrenderer = new Program("./src/main/glsl/renderers/RT/Test/test");
    private final ComputeProgram pathTracingProgram = new ComputeProgram("./src/main/glsl/renderers/RT/pathtracer");
    private final int vao = glGenVertexArrays();

    private final UBORTSettings settingsUbo = new UBORTSettings(31);
    private final UBOCamera cameraUbo = new UBOCamera(30);
    private Texture pathTracingTexture;

    private int max_bounces;
    private int max_samples;

    public RTDrawer(Window window) throws FileNotFoundException {
        super(window);
    }

    public void initRender(int width, int height, int samples, int bounces) {
        max_bounces = Math.max(samples, 1);
        max_bounces = Math.max(bounces, 2);
        pathTracingTexture = new Texture(
                width,
                height,
                false,
                32,
                GL_RGBA32F,
                GL_READ_WRITE,
                GL_NEAREST
        );
        frameId = 0;
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
