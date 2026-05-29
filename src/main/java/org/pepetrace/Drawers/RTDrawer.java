package org.pepetrace.Drawers;

import static org.lwjgl.glfw.GLFW.*;
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
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.stb.STBImageWrite.stbi_write_png;
import static org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_saveFileDialog;

import imgui.ImGui;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.pepetrace.Buffers.SSBO;
import org.pepetrace.Buffers.Texture;
import org.pepetrace.Camera;
import org.pepetrace.GUI.RTViewportWindow;
import org.pepetrace.GlobalState;
import org.pepetrace.Scene.Material.TextureMaterial;
import org.pepetrace.Scene.Scene;
import org.pepetrace.Shader.ComputeProgram;
import org.pepetrace.Shader.Program;
import org.pepetrace.UBOCamera;
import org.pepetrace.UBORTSettings;
import org.pepetrace.Util.GPUTimeQuerier;
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
    private Texture albedoTexture;
    private Texture normalTexture;
    private int displayFbo;
    private int displayTexId;
    private int albedoDisplayFbo;
    private int albedoDisplayTexId;
    private int normalDisplayFbo;
    private int normalDisplayTexId;

    private int max_bounces;
    private int max_samples;
    private int maxSpp;
    private int maxMaterialTextures;
    long lastDispatchNs = 0;
    private final GPUTimeQuerier gpuTimer = new GPUTimeQuerier();
    private final RTViewportWindow rtViewportWindow = new RTViewportWindow();
    private boolean attentionSignaled;
    private boolean denoised;
    private boolean denoiseSucceeded;
    private boolean wasEscapePressed;
    private int presentationMode = 1;
    private long avgFrameTimeNs;
    private long frameClock;
    private float etaSeconds;
    private org.pepetrace.Denoising.OIDNDenoiser denoiser;
    private final Vector3f cameraPosition = new Vector3f(0, 0, -5);
    private final Vector2f cameraYawPitch = new Vector2f(0, 0);

    public void resetRender() {
        frameId = 0;
        attentionSignaled = false;
        denoised = false;
        denoiseSucceeded = false;
        frameClock = 0;
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
            albedoTexture.close();
            normalTexture.close();
            initRender(newWidth, newHeight, max_samples, max_bounces, maxSpp);
        }
    }

    public void initRender(
        int width,
        int height,
        int samples,
        int bounces,
        int maxSpp
    ) {
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
        albedoTexture = new Texture(
            width,
            height,
            false,
            17,
            GL_RGBA32F,
            GL_READ_WRITE,
            GL_LINEAR
        );
        normalTexture = new Texture(
            width,
            height,
            false,
            18,
            GL_RGBA32F,
            GL_READ_WRITE,
            GL_LINEAR
        );

        // Create display FBO + RGBA8 texture for ImGui display
        if (displayTexId != 0) {
            glDeleteTextures(displayTexId);
            glDeleteFramebuffers(displayFbo);
            glDeleteTextures(albedoDisplayTexId);
            glDeleteFramebuffers(albedoDisplayFbo);
            glDeleteTextures(normalDisplayTexId);
            glDeleteFramebuffers(normalDisplayFbo);
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
        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0,
            GL_TEXTURE_2D,
            displayTexId,
            0
        );
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        // Albedo display
        albedoDisplayTexId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, albedoDisplayTexId);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA8, width, height);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glBindTexture(GL_TEXTURE_2D, 0);

        albedoDisplayFbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, albedoDisplayFbo);
        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0,
            GL_TEXTURE_2D,
            albedoDisplayTexId,
            0
        );
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        // Normal display
        normalDisplayTexId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, normalDisplayTexId);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA8, width, height);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glBindTexture(GL_TEXTURE_2D, 0);

        normalDisplayFbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, normalDisplayFbo);
        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0,
            GL_TEXTURE_2D,
            normalDisplayTexId,
            0
        );
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
        glBindImageTexture(
            16,
            pathTracingTexture.id,
            0,
            false,
            0,
            GL_READ_WRITE,
            GL_RGBA32F
        );

        // SSBOs (buffer IDs never change)
        SSBO[] ssbos = {
            (SSBO) programState.getArbitraryData("geometryBuffer"),
            (SSBO) programState.getArbitraryData("indexBuffer"),
            (SSBO) programState.getArbitraryData("materialIndicesBuffer"),
            (SSBO) programState.getArbitraryData("modelMatricesBuffer"),
            (SSBO) programState.getArbitraryData("triangleModelIndicesBuffer"),
        };
        for (SSBO ssbo : ssbos) {
            glBindBufferBase(
                GL_SHADER_STORAGE_BUFFER,
                ssbo.getBinding(),
                ssbo.getId()
            );
        }

        // Skybox texture
        glActiveTexture(GL_TEXTURE4);
        glBindTexture(
            GL_TEXTURE_2D,
            (int) programState.getArbitraryData("skyboxTexture")
        );
        pathTracingProgram.setInt("skybox", 4);

        frameId = 0;
    }

    public void rebindSkybox(int skyboxId) {
        glActiveTexture(GL_TEXTURE4);
        glBindTexture(GL_TEXTURE_2D, skyboxId);
        pathTracingProgram.setInt("skybox", 4);
        resetRender();
    }

    @Override
    public void renderFrame() {
        if (pathTracingTexture == null) {
            return;
        }

        long clock = System.nanoTime();
        if (frameClock != 0) {
            long rawDelta = clock - frameClock;
            if (avgFrameTimeNs == 0) {
                avgFrameTimeNs = rawDelta;
            } else {
                avgFrameTimeNs += (long)((rawDelta - avgFrameTimeNs) * 0.05);
            }
        }
        frameClock = clock;

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
                    new float[] { 0, 0, 0, 0 }
                );
                glClearTexImage(
                    albedoTexture.id,
                    0,
                    GL_RGBA,
                    GL_FLOAT,
                    new float[] { 0, 0, 0, 0 }
                );
                glClearTexImage(
                    normalTexture.id,
                    0,
                    GL_RGBA,
                    GL_FLOAT,
                    new float[] { 0, 0, 0, 0 }
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
            cameraUbo.updateBuffer(cameraPosition, cameraYawPitch);
            glMemoryBarrier(
                GL_SHADER_STORAGE_BARRIER_BIT |
                    GL_TEXTURE_UPDATE_BARRIER_BIT |
                    GL_TEXTURE_FETCH_BARRIER_BIT |
                    GL_SHADER_IMAGE_ACCESS_BARRIER_BIT
            );

            pathTracingProgram.use();
            int groupsX = (pathTracingTexture.getWidth() + 15) / 16;
            int groupsY = (pathTracingTexture.getHeight() + 15) / 16;
            if (groupsX < 1) groupsX = 1;
            if (groupsY < 1) groupsY = 1;

            glDispatchCompute(groupsX, groupsY, 1);

            glMemoryBarrier(
                GL_SHADER_IMAGE_ACCESS_BARRIER_BIT |
                    GL_TEXTURE_FETCH_BARRIER_BIT
            );
            gpuTimer.stopTimerAsync();

            renderDisplayPass();

            frameId++;
        }

        // --- Denoise when rendering is finished ---
        if (done && !denoised) {
            denoiseResult();
        }

        // --- ETA: remaining frames based on current SPP ---
        int remainingSpp = Math.max(0, maxSpp - spp);
        if (remainingSpp > 0) {
            int remainingBatches =
                (remainingSpp + max_samples - 1) / max_samples;
            int remainingFrames = remainingBatches * 4;
            etaSeconds = (remainingFrames * avgFrameTimeNs) / 1_000_000_000f;
        } else {
            etaSeconds = 0;
        }

        // --- ImGui viewport with display texture ---
        // Clear mouse so main-window events from glfwPollEvents don't leak in
        ImGui.getIO().setMousePos(-Float.MAX_VALUE, -Float.MAX_VALUE);
        for (int i = 0; i < 5; i++) ImGui.getIO().setMouseDown(i, false);
        ImGui.getIO().setMouseWheel(0);
        imGuiGl3.newFrame();
        // imGuiGlfw.newFrame() is intentionally not called (would leak input),
        // so we must propagate DPI scale manually.
        // currentWidth/currentHeight are framebuffer pixels, but ImGui expects
        // displaySize in screen points and framebufferScale = pixels / points.
        int[] winW = new int[1],
            winH = new int[1];
        glfwGetWindowSize(window.getId(), winW, winH);
        ImGui.getIO().setDisplaySize(winW[0], winH[0]);
        ImGui.getIO().setDisplayFramebufferScale(
            (float) currentWidth / winW[0],
            (float) currentHeight / winH[0]
        );
        ImGui.newFrame();

        rtViewportWindow.setState(
            displayTexId,
            albedoDisplayTexId,
            normalDisplayTexId,
            pathTracingTexture.getWidth(),
            pathTracingTexture.getHeight(),
            winW[0],
            winH[0],
            frameId,
            max_samples,
            max_bounces,
            maxSpp,
            lastDispatchNs,
            done,
            etaSeconds,
            presentationMode
        );
        rtViewportWindow.render(0);

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());

        // Save via Ctrl+S in the render window
        if (
            window.isKeyPressed(GLFW_KEY_S) &&
            window.isKeyPressed(GLFW_KEY_LEFT_CONTROL)
        ) {
            saveImage();
        }

        boolean escapePressed = window.isKeyPressed(GLFW_KEY_ESCAPE);
        if (escapePressed && !wasEscapePressed) {
            presentationMode = (presentationMode + 1) % 2;
        }
        wasEscapePressed = escapePressed;

        glfwSetWindowTitle(
            window.getId(),
            done ? "(DONE) Render | Pepetrace" : "Render | Pepetrace"
        );
    }

    private void renderDisplayPass() {
        int w = pathTracingTexture.getWidth();
        int h = pathTracingTexture.getHeight();
        glDisable(GL_DEPTH_TEST);
        windowRenderer.use();

        // Color
        glBindFramebuffer(GL_FRAMEBUFFER, displayFbo);
        glViewport(0, 0, w, h);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, pathTracingTexture.id);
        windowRenderer.setInt("u_tex", 0);
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 3);

        // Albedo
        glBindFramebuffer(GL_FRAMEBUFFER, albedoDisplayFbo);
        glViewport(0, 0, w, h);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, albedoTexture.id);
        windowRenderer.setInt("u_tex", 0);
        glDrawArrays(GL_TRIANGLES, 0, 3);

        // Normal
        glBindFramebuffer(GL_FRAMEBUFFER, normalDisplayFbo);
        glViewport(0, 0, w, h);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, normalTexture.id);
        glDrawArrays(GL_TRIANGLES, 0, 3);

        glBindVertexArray(0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void denoiseResult() {
        if (denoiser == null) {
            try {
                denoiser = new org.pepetrace.Denoising.OIDNDenoiser();
            } catch (Exception e) {
                System.err.println(
                    "OIDN denoiser unavailable: " + e.getMessage()
                );
                denoised = true;
                return;
            }
        }
        try {
            int sampleCount = (frameId / 4) * max_samples;
            if (sampleCount < 1) sampleCount = 1;
            denoiser.denoise(
                pathTracingTexture.id,
                albedoTexture.id,
                normalTexture.id,
                pathTracingTexture.id,
                pathTracingTexture.getWidth(),
                pathTracingTexture.getHeight(),
                sampleCount
            );
            renderDisplayPass();
            denoiseSucceeded = true;
        } catch (Exception e) {
            System.err.println("Denoise failed: " + e.getMessage());
        }
        denoised = true;
    }

    private void saveImage() {
        String defaultName = "rt_render_" + System.currentTimeMillis() + ".png";
        String path;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.png"));
            filters.flip();
            path = tinyfd_saveFileDialog(
                "Save Render As",
                defaultName,
                filters,
                "PNG Image (*.png)"
            );
        }
        if (path == null) {
            return;
        }

        int w = pathTracingTexture.getWidth();
        int h = pathTracingTexture.getHeight();
        int rowSize = w * 4;

        // Read the tonemapped RGBA8 display texture (OpenGL bottom-left origin)
        ByteBuffer rgba = ByteBuffer.allocateDirect(w * h * 4);
        glBindFramebuffer(GL_FRAMEBUFFER, displayFbo);
        glReadPixels(0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, rgba);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

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

    public boolean isDenoised() {
        return denoised;
    }

    public boolean isDenoiseSucceeded() {
        return denoiseSucceeded;
    }

    public int getDisplayTexId() {
        return displayTexId;
    }

    public int getDisplayFbo() {
        return displayFbo;
    }

    public int getPathTracingTexId() {
        return pathTracingTexture.id;
    }
}
