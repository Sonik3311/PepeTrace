package org.pepetrace.Drawers;

import static org.lwjgl.opengl.ARBClearTexture.glClearTexImage;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15C.GL_READ_WRITE;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30C.GL_RGBA32F;
import static org.lwjgl.opengl.GL42C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
import static org.lwjgl.opengl.GL42C.GL_TEXTURE_FETCH_BARRIER_BIT;
import static org.lwjgl.opengl.GL42C.GL_TEXTURE_UPDATE_BARRIER_BIT;
import static org.lwjgl.opengl.GL42C.glBindImageTexture;
import static org.lwjgl.opengl.GL42C.glMemoryBarrier;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL43C.GL_MAX_COMPUTE_TEXTURE_IMAGE_UNITS;
import static org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BARRIER_BIT;
import static org.lwjgl.opengl.GL43C.glDispatchCompute;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.pepetrace.Buffers.SSBO;
import org.pepetrace.Buffers.Texture;
import org.pepetrace.Camera;
import org.pepetrace.GlobalState;
import org.pepetrace.Scene.Material.TextureMaterial;
import org.pepetrace.Scene.Scene;
import org.pepetrace.Shader.ComputeProgram;
import org.pepetrace.Shader.Program;
import org.pepetrace.UBOCamera;
import org.pepetrace.UBORTSettings;
import org.pepetrace.Window;

public class RTDrawer extends AbstractDrawer {

    private final Program testrenderer = new Program(
        "./src/main/glsl/renderers/RT/Test/test"
    );
    private ComputeProgram pathTracingProgram;
    private final Program windowRenderer = new Program(
        "./src/main/glsl/screenQuad"
    );
    private final int vao = glGenVertexArrays();

    private final UBORTSettings settingsUbo = new UBORTSettings(30);
    private final UBOCamera cameraUbo = new UBOCamera(29);
    private Texture pathTracingTexture;

    private int max_bounces;
    private int max_samples;
    private int maxMaterialTextures;
    long lastDispatchNs = 0;
    private final Vector3f cameraPosition = new Vector3f(0, 0, -5);
    private final Vector2f cameraYawPitch = new Vector2f(0, 0);

    public void resetRender() {
        frameId = 0;
    }

    public void copyCameraFrom(Camera camera) {
        cameraPosition.set(camera.getPosition());
        cameraYawPitch.set(camera.getYawPitch());
    }

    public RTDrawer(Window window) throws FileNotFoundException {
        super(window);
    }

    @Override
    public void onResize(int newWidth, int newHeight, boolean isFromGlfw) {
        if (!isFromGlfw) {
            super.onResize(newWidth, newHeight, isFromGlfw);
            if (pathTracingTexture != null) {
                pathTracingTexture.close();
                initRender(newWidth, newHeight, max_samples, max_bounces);
            }
        }
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
        // Query GPU limit for compute shader texture image units.
        // Each material uses 3 texture units (albedo, normal, rmtt).
        // One unit is reserved for the skybox at binding 4.
        int maxUnits = glGetInteger(GL_MAX_COMPUTE_TEXTURE_IMAGE_UNITS);
        maxMaterialTextures = Math.max(1, (maxUnits - 1) / 3);
        GlobalState.getInstance().setMaxMaterials(maxMaterialTextures);

        // Rebuild compute program with the correct material count
        if (pathTracingProgram != null) {
            pathTracingProgram.deleteProgram();
        }
        Map<String, String> defines = new HashMap<>();
        defines.put("MAX_MATERIALS", String.valueOf(maxMaterialTextures));
        try {
            pathTracingProgram = new ComputeProgram(
                "./src/main/glsl/renderers/RT/pathtracer",
                defines
            );
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        // --- Persistent bindings (set once, persist across frames) ---

        // Output image
        glBindImageTexture(16, pathTracingTexture.id, 0, false, 0, GL_READ_WRITE, GL_RGBA32F);

        // SSBOs (buffer IDs never change)
        SSBO[] ssbos = {
            (SSBO) programState.getArbitraryData("geometryBuffer"),
            (SSBO) programState.getArbitraryData("indexBuffer"),
            (SSBO) programState.getArbitraryData("materialIndicesBuffer"),
            (SSBO) programState.getArbitraryData("modelMatricesBuffer"),
            (SSBO) programState.getArbitraryData("triangleModelIndicesBuffer"),
        };
        for (SSBO ssbo : ssbos) {
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, ssbo.getBinding(), ssbo.getId());
        }

        // Skybox texture
        glActiveTexture(GL_TEXTURE4);
        glBindTexture(GL_TEXTURE_2D, (int) programState.getArbitraryData("skyboxTexture"));
        pathTracingProgram.setInt("skybox", 4);

        // Fullscreen quad output texture binding
        windowRenderer.use();
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, pathTracingTexture.id);
        windowRenderer.setInt("u_tex", 2);

        frameId = 0;
    }

    @Override
    public void renderFrame() {
        if (pathTracingTexture == null) {
            return;
        }
        // Clear the texture on first frame after reset to avoid stale pixels
        if (frameId == 0) {
            glClearTexImage(
                pathTracingTexture.id,
                0,
                GL_RGBA,
                GL_FLOAT,
                new float[] {0, 0, 0, 0}
            );
            // Bind material textures once on first frame (cached per context)
            Scene scene = programState.getScene();
            if (scene != null) {
                List<TextureMaterial> materials = scene.getMaterials();
                int count = Math.min(materials.size(), maxMaterialTextures);
                int albedoBase = 10;
                int normalBase = albedoBase + maxMaterialTextures;
                int rmttBase = albedoBase + 2 * maxMaterialTextures;
                for (int i = 0; i < count; i++) {
                    TextureMaterial mat = materials.get(i);
                    glActiveTexture(GL_TEXTURE0 + albedoBase + i);
                    glBindTexture(GL_TEXTURE_2D, mat.getAlbedoTexture().id);
                    glActiveTexture(GL_TEXTURE0 + normalBase + i);
                    glBindTexture(GL_TEXTURE_2D, mat.getNormalTexture().id);
                    glActiveTexture(GL_TEXTURE0 + rmttBase + i);
                    glBindTexture(GL_TEXTURE_2D, mat.getRMTTexture().id);
                }
            }
        }
        settingsUbo.updateBuffer(
            frameId,
            max_samples,
            max_bounces,
            programState.getScene().getTriangleCount()
        );
        cameraUbo.updateBuffer(
            cameraPosition,
            cameraYawPitch
        );
        glMemoryBarrier(
            GL_SHADER_STORAGE_BARRIER_BIT | GL_TEXTURE_UPDATE_BARRIER_BIT | GL_TEXTURE_FETCH_BARRIER_BIT | GL_SHADER_IMAGE_ACCESS_BARRIER_BIT
        );

        pathTracingProgram.use();
        int groupsX = (pathTracingTexture.getWidth() + 15) / 16;
        int groupsY = (pathTracingTexture.getHeight() + 15) / 16;
        if (groupsX < 1) groupsX = 1;
        if (groupsY < 1) groupsY = 1;
        long t0 = System.nanoTime();
        glDispatchCompute(groupsX, groupsY, 1);

        glMemoryBarrier(
            GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL_TEXTURE_FETCH_BARRIER_BIT
        );
        lastDispatchNs = System.nanoTime() - t0;

        windowRenderer.use();
        glViewport(0, 0, currentWidth, currentHeight);
        glDisable(GL_DEPTH_TEST);
        int error = glGetError();
        if (error != GL_NO_ERROR) System.out.println("GL ERROR RT " + error);
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);

        frameId++;
    }
}
