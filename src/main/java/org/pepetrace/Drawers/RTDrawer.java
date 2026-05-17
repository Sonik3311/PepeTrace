package org.pepetrace.Drawers;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15C.GL_READ_WRITE;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30C.GL_RGBA32F;
import static org.lwjgl.opengl.GL42C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
import static org.lwjgl.opengl.GL42C.GL_TEXTURE_FETCH_BARRIER_BIT;
import static org.lwjgl.opengl.GL42C.glMemoryBarrier;
import static org.lwjgl.opengl.GL43C.glDispatchCompute;
import static org.lwjgl.opengl.GL42C.glBindImageTexture;

import java.io.FileNotFoundException;
import org.pepetrace.Buffers.Texture;
import org.pepetrace.Shader.ComputeProgram;
import org.pepetrace.Shader.Program;
import org.pepetrace.UBOCamera;
import org.pepetrace.UBORTSettings;
import org.pepetrace.Window;

public class RTDrawer extends AbstractDrawer {

    private final Program testrenderer = new Program(
        "./src/main/glsl/renderers/RT/Test/test"
    );
    private final ComputeProgram pathTracingProgram = new ComputeProgram(
        "./src/main/glsl/renderers/RT/pathtracer"
    );
    private final Program windowRenderer = new Program(
        "./src/main/glsl/screenQuad"
    );
    private final int vao = glGenVertexArrays();

    private final UBORTSettings settingsUbo = new UBORTSettings(30);
    private final UBOCamera cameraUbo = new UBOCamera(29);
    private Texture pathTracingTexture;

    private int max_bounces;
    private int max_samples;

    public RTDrawer(Window window) throws FileNotFoundException {
        super(window);
    }

    public void initRender(int width, int height, int samples, int bounces) {
        max_samples = Math.max(samples, 1);
        max_bounces = Math.max(bounces, 2);
        pathTracingTexture = new Texture(
            width,
            height,
            false,
            16,
            GL_RGBA32F,
            GL_READ_WRITE,
            GL_NEAREST
        );
        frameId = 0;
    }

    @Override
    public void renderFrame() {
        if (pathTracingTexture == null) {
            return;
        }
        //settingsUbo.updateBuffer(frameId, max_samples, max_bounces, programState.getScene().getTriangleCount());
        //glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT | GL_TEXTURE_FETCH_BARRIER_BIT);

        glBindImageTexture(16, pathTracingTexture.id, 0, false, 0, GL_READ_WRITE, GL_RGBA32F);
        pathTracingProgram.use();
        int groupsX = (pathTracingTexture.getWidth() + 15) / 16;
        int groupsY = (pathTracingTexture.getHeight() + 15) / 16;
        glDispatchCompute(groupsX, groupsY, 1);

        glMemoryBarrier(
            GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL_TEXTURE_FETCH_BARRIER_BIT
        );

        windowRenderer.use();
        glViewport(0, 0, currentWidth, currentHeight);

        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, pathTracingTexture.id);
        windowRenderer.setInt("u_tex", 2);
        int error = glGetError();
        if (error != GL_NO_ERROR) System.out.println("GL ERROR RT " + error);
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glActiveTexture(GL_TEXTURE0);
        glBindVertexArray(0);

        frameId++;
    }
}
