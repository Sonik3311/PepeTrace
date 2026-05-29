package org.pepetrace.Drawers;

import static org.lwjgl.glfw.GLFW.glfwGetWindowContentScale;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.GL_READ_WRITE;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL42.GL_TEXTURE_FETCH_BARRIER_BIT;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BARRIER_BIT;
import static org.lwjgl.opengl.GL43.glDispatchCompute;

import imgui.ImGui;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;
import org.pepetrace.Buffers.Texture;
import org.pepetrace.GUI.*;
import org.pepetrace.Main;
import org.pepetrace.Scene.Scene;
import org.pepetrace.Shader.ComputeProgram;
import org.pepetrace.Shader.Program;
import org.pepetrace.UBORenderInts;
import org.pepetrace.Util.ViewportRenderMode;
import org.pepetrace.Window;

public class ViewportDrawer extends AbstractDrawer {

    // GUI windows
    private final BuildInfoWindow buildInfoWindow = new BuildInfoWindow();
    private final MainMenuBar mainMenuBar = new MainMenuBar();
    private final MaterialManagerWindow materialManagerWindow =
        new MaterialManagerWindow();
    private final CameraInfoWindow cameraInfoWindow = new CameraInfoWindow();
    private final ViewportWindow viewportWindow = new ViewportWindow();
    private final RenderSettingsWindow viewportRenderSettingsWindow =
        new RenderSettingsWindow();
    private final OutlinerWindow outlinerWindow = new OutlinerWindow();
    private final ModelDataWindow modelDataWindow = new ModelDataWindow();

    // Rendering prerequisites
    private Texture pathTracingTexture;
    private Texture modelStencilTexture;
    private Texture outputTexture;
    private int targetWidth;
    private int targetHeight;
    private int pixelWidth;
    private int pixelHeight;
    private final ComputeProgram pathTracingProgram = new ComputeProgram(
        "/glsl/renderers/viewport/program"
    );
    private final Program windowTextureDrawerProgram = new Program(
        "/glsl/screenQuad"
    );
    private final Program outlineProgram = new Program(
        "/glsl/renderers/viewport/outliner/outliner"
    );
    private final int fbo = glGenFramebuffers();
    private final int vao = glGenVertexArrays();
    private final UBORenderInts ubo = new UBORenderInts(3);

    // ImGui parameters
    public boolean draggingMouse = false;
    public ImInt renderMode = new ImInt(ViewportRenderMode.SHADED.ordinal());
    public ImBoolean accumulateFrames = new ImBoolean(false);
    public ImBoolean ambientOcclusion = new ImBoolean(false);
    public ImInt ambientOcclusionSamples = new ImInt(3);

    public Texture getOutputTexture() {
        return outputTexture;
    }

    public Texture getModelStencilTexture() {
        return modelStencilTexture;
    }

    public ViewportDrawer(Window window) throws FileNotFoundException {
        super(window);

        createTextures(currentWidth, currentHeight);
        targetWidth = currentWidth;
        targetHeight = currentHeight;
        pixelWidth = currentWidth;
        pixelHeight = currentHeight;

        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0,
            GL_TEXTURE_2D,
            outputTexture.id,
            0
        );
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("FBO not complete: " + status);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    protected void createTextures(int width, int height) {
        pathTracingTexture = new Texture(
            width,
            height,
            false,
            1,
            GL_RGBA8,
            GL_READ_WRITE,
            GL_NEAREST
        );
        modelStencilTexture = new Texture(
            width,
            height,
            false,
            12,
            GL_R32UI,
            GL_READ_WRITE,
            GL_NEAREST
        );
        outputTexture = new Texture(
            width,
            height,
            false,
            13,
            GL_RGBA8,
            GL_WRITE_ONLY,
            GL_LINEAR
        );
    }

    @Override
    public void onResize(int width, int height, boolean isFromGlfw) {
        if (currentWidth == width && currentHeight == height) return;
        super.onResize(width, height, isFromGlfw);
        // width/height from ImGui content region are in points; scale to pixels for GL
        float[] xscale = {0};
        float[] yscale = {0};
        glfwGetWindowContentScale(window.getId(), xscale, yscale);
        pixelWidth = (int) (width * xscale[0]);
        pixelHeight = (int) (height * yscale[0]);
        if (pixelWidth < 1) pixelWidth = 1;
        if (pixelHeight < 1) pixelHeight = 1;
        pathTracingTexture.close();
        modelStencilTexture.close();
        outputTexture.close();
        createTextures(pixelWidth, pixelHeight);
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0,
            GL_TEXTURE_2D,
            outputTexture.id,
            0
        );
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        resetRender();
    }

    @Override
    public void renderFrame() {
        renderFrame(false);
    }

    public void renderFrame(boolean skipSceneRender) {
        if (skipSceneRender) {
            programState.setArbitraryData("GPURenderTime", -1.0);
        }
        Scene scene = programState.getScene();
        int triangleCount = scene.getTriangleCount();

        // --- ImGui (early, so ViewportWindow detects any resize before compute dispatch) ---
        imGuiGl3.newFrame();
        imGuiGlfw.newFrame();
        ImGui.newFrame();
        ImGui.getIO().setWantCaptureMouse(draggingMouse);

        // Save selection before ImGui windows (drag-drop may change it for highlight)
        Set<Integer> savedSelection = new HashSet<>(programState.getSelectedModelIndices());

        mainMenuBar.render(0);
        ImGui.dockSpaceOverViewport();
        buildInfoWindow.render(ImGuiWindowFlags.NoCollapse);
        materialManagerWindow.render(ImGuiWindowFlags.NoCollapse);
        cameraInfoWindow.render(ImGuiWindowFlags.NoCollapse);
        viewportWindow.render(0, outputTexture);
        viewportRenderSettingsWindow.render(ImGuiWindowFlags.NoCollapse);
        outlinerWindow.render(ImGuiWindowFlags.NoCollapse);
        modelDataWindow.render(ImGuiWindowFlags.NoCollapse);
        ImGui.showStyleEditor();

        // Apply any pending resize detected by ViewportWindow
        if (targetWidth != currentWidth || targetHeight != currentHeight) {
            onResize(targetWidth, targetHeight, false);
        }

        // --- Compute dispatch (now at the correct size) ---
        if (!skipSceneRender) {
            ubo.updateBuffer(
                frameId,
                ambientOcclusionSamples.get(),
                2,
                ambientOcclusion.get(),
                ViewportRenderMode.values()[renderMode.get()],
                triangleCount
            );
            glMemoryBarrier(
                GL_SHADER_STORAGE_BARRIER_BIT | GL_TEXTURE_FETCH_BARRIER_BIT
            );

            pathTracingProgram.use();
            glActiveTexture(GL_TEXTURE1);
            glBindTexture(
                GL_TEXTURE_2D,
                (int) programState.getArbitraryData("skyboxTexture")
            );
            pathTracingProgram.setInt("blurrySkybox", 1);
            int groupsX = (pixelWidth + 15) / 16;
            int groupsY = (pixelHeight + 15) / 16;
            glDispatchCompute(groupsX, groupsY, 1);

            glMemoryBarrier(
                GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL_TEXTURE_FETCH_BARRIER_BIT
            );
        }

        // Outline renders after ImGui windows so drag-highlight selection change takes effect
        glViewport(0, 0, pixelWidth, pixelHeight);
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glDisable(GL_DEPTH_TEST);
        outlineProgram.use();

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, modelStencilTexture.id);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, pathTracingTexture.id);

        outlineProgram.setInt("u_stencilTexture", 0);
        outlineProgram.setInt("u_colorTexture", 1);
        outlineProgram.setIntArray(
            "u_selectedID",
            programState
                .getSelectedModelIndices()
                .stream()
                .mapToInt(Integer::intValue)
                .toArray()
        );
        outlineProgram.setInt(
            "u_totalSelectedModels",
            programState.getSelectedModelIndices().size()
        );

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glEnable(GL_DEPTH_TEST);

        // Restore selection after outline if it was only a drag highlight
        if (viewportWindow.dragHighlightActive) {
            programState.clearSelectedModels();
            for (int idx : savedSelection) {
                programState.addSelectedModel(idx);
            }
        }

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
        boolean guiWantsMouse = ImGui.getIO().getWantCaptureMouse();

        // Удаление через Delete (без изменений)
        if (!guiWantsMouse && ImGui.isKeyPressed(ImGuiKey.Delete)) {
            Set<Integer> selectedIndices =
                programState.getSelectedModelIndices();
            if (!selectedIndices.isEmpty()) {
                int selected = selectedIndices.iterator().next();
                // Используем уже объявленную ранее переменную scene
                if (selected >= 0 && selected < scene.getModels().size()) {
                    scene.removeModel(selected);
                    programState.removeSelectedModel(selected);
                    Main mainProgram = (Main) programState.getArbitraryData(
                        "Main"
                    );
                    if (mainProgram != null) {
                        mainProgram.refreshSceneBuffers(true, true);
                        if (scene.getModels().isEmpty()) {
                            mainProgram.forceClearRender();
                        }
                    }
                }
            }
        }

        if (accumulateFrames.get()) frameId++;
    }

    public void setViewportTargetSize(int width, int height) {
        targetWidth = width;
        targetHeight = height;
    }

    public int getOutputTextureId() {
        return outputTexture.id;
    }

    public RenderSettingsWindow getRenderSettings() {
        return viewportRenderSettingsWindow;
    }

    public void resetRender() {
        frameId = 0;
    }
}
