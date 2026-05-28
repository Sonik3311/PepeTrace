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

import imgui.ImGui;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_saveFileDialog;
import static org.lwjgl.stb.STBImageWrite.stbi_write_png;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.pepetrace.Buffers.SSBO;
import org.pepetrace.Buffers.Texture;
import org.pepetrace.Camera;
import org.pepetrace.GlobalState;
import org.pepetrace.GUI.RTViewportWindow;
import org.pepetrace.Scene.Material.TextureMaterial;
import org.pepetrace.Util.GPUTimeQuerier;
import org.pepetrace.Scene.Scene;
import org.pepetrace.Shader.ComputeProgram;
import org.pepetrace.Shader.Program;
import org.pepetrace.UBOCamera;
import org.pepetrace.UBORTSettings;
import org.pepetrace.Window;

import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.glfw.GLFW.*;

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
    private int displayFbo;
    private int displayTexId;

    private int max_bounces;
    private int max_samples;
    private int maxSpp;
    private int maxMaterialTextures;
    long lastDispatchNs = 0;
    private final GPUTimeQuerier gpuTimer = new GPUTimeQuerier();
    private final RTViewportWindow rtViewportWindow = new RTViewportWindow();
    private boolean attentionSignaled;
    private final Vector3f cameraPosition = new Vector3f(0, 0, -5);
    private final Vector2f cameraYawPitch = new Vector2f(0, 0);

    public void resetRender() {
        frameId = 0;
        attentionSignaled = false;
    }

    public void copyCameraFrom(Camera camera) {
        cameraPosition.set(camera.getPosition());
        cameraYawPitch.set(camera.getYawPitch());
    }

    public RTDrawer(Window window) throws FileNotFoundException {
        super(window);
    }

    @Override
    protected void init(String imGuiLayoutFile) {
        super.init(imGuiLayoutFile);
        currentHeight = window.getHeight();
        currentWidth = window.getWidth();
    }

    @Override
    public void onResize(int newWidth, int newHeight, boolean isFromGlfw) {
        super.onResize(newWidth, newHeight, isFromGlfw);
        if (pathTracingTexture != null && !isFromGlfw) {
            pathTracingTexture.close();
            initRender(newWidth, newHeight, max_samples, max_bounces, maxSpp);
        }
    }

    public void initRender(int width, int height, int samples, int bounces, int maxSpp) {
        max_samples = Math.max(samples, 1);
        max_bounces = Math.max(bounces, 2);
        this.maxSpp = Math.max(maxSpp, 0);
        if (pathTracingTexture != null) {
            pathTracingTexture.close();
        }
        pathTracingTexture = new Texture(
            width,
            height,
            false,
            16,
            GL_RGBA32F,
            GL_READ_WRITE,
            GL_LINEAR
        );

        // Create display FBO + RGBA8 texture for ImGui display
        if (displayTexId != 0) {
            glDeleteTextures(displayTexId);
            glDeleteFramebuffers(displayFbo);
        }
        displayTexId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, displayTexId);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA8, width, height);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glBindTexture(GL_TEXTURE_2D, 0);

        displayFbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, displayFbo);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, displayTexId, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

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

        frameId = 0;
    }

    @Override
    public void renderFrame() {
        if (pathTracingTexture == null) {
            return;
        }

        if (gpuTimer.isResultReady()) {
            lastDispatchNs = gpuTimer.getResult();
        }

        int spp = (frameId / 4) * max_samples;
        boolean done = maxSpp > 0 && spp >= maxSpp;

        if (done && !attentionSignaled) {
            attentionSignaled = true;
            glfwRequestWindowAttention(window.getId());
        }

        if (!done) {
            gpuTimer.startTimer();
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

            glDispatchCompute(groupsX, groupsY, 1);

            glMemoryBarrier(
                GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL_TEXTURE_FETCH_BARRIER_BIT
            );
            gpuTimer.stopTimerAsync();

            // --- Display FBO pass: render fullscreen quad (divides by sample count) to RGBA8 texture ---
            glBindFramebuffer(GL_FRAMEBUFFER, displayFbo);
            glViewport(0, 0, pathTracingTexture.getWidth(), pathTracingTexture.getHeight());
            glDisable(GL_DEPTH_TEST);
            windowRenderer.use();
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, pathTracingTexture.id);
            windowRenderer.setInt("u_tex", 0);
            glBindVertexArray(vao);
            glDrawArrays(GL_TRIANGLES, 0, 3);
            glBindVertexArray(0);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);

            frameId++;
        }

        // --- ImGui viewport with display texture ---
        // Clear mouse so main-window events from glfwPollEvents don't leak in
        ImGui.getIO().setMousePos(-Float.MAX_VALUE, -Float.MAX_VALUE);
        for (int i = 0; i < 5; i++) ImGui.getIO().setMouseDown(i, false);
        ImGui.getIO().setMouseWheel(0);
        imGuiGl3.newFrame();
        ImGui.getIO().setDisplaySize(currentWidth, currentHeight);
        ImGui.newFrame();

        rtViewportWindow.setState(
            displayTexId,
            pathTracingTexture.getWidth(),
            pathTracingTexture.getHeight(),
            currentWidth,
            currentHeight,
            frameId,
            max_samples,
            max_bounces,
            maxSpp,
            lastDispatchNs,
            done
        );
        rtViewportWindow.render(0);

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());

        // Save via Ctrl+S in the render window
        if (window.isKeyPressed(GLFW_KEY_S) && window.isKeyPressed(GLFW_KEY_LEFT_CONTROL)) {
            saveImage();
        }

        glfwSetWindowTitle(window.getId(), done ? "(DONE) Render | Pepetrace" : "Render | Pepetrace");
    }

    private void saveImage() {
        String defaultName = "rt_render_" + System.currentTimeMillis() + ".png";
        String path;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.png"));
            filters.flip();
            path = tinyfd_saveFileDialog("Save Render As", defaultName, filters, "PNG Image (*.png)");
        }
        if (path == null) {
            return;
        }

        int w = pathTracingTexture.getWidth();
        int h = pathTracingTexture.getHeight();
        int rowSize = w * 4;

        // Read the tonemapped RGBA8 display texture (OpenGL bottom-left origin)
        ByteBuffer rgba = ByteBuffer.allocateDirect(w * h * 4);
        glGetTextureImage(displayTexId, 0, GL_RGBA, GL_UNSIGNED_BYTE, rgba);

        // Flip vertically for PNG top-left origin
        ByteBuffer flipped = ByteBuffer.allocateDirect(w * h * 4);
        for (int y = 0; y < h; y++) {
            rgba.position((h - 1 - y) * rowSize);
            rgba.limit((h - y) * rowSize);
            flipped.put(rgba);
        }
        rgba.clear();
        flipped.rewind();

        stbi_write_png(path, w, h, 4, flipped, rowSize);
        System.out.println("Saved render to: " + path);
    }
}
