package org.pepetrace;

import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glReadPixels;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;

import java.nio.ByteBuffer;
import org.pepetrace.Drawers.RTDrawer;
import org.pepetrace.GUI.RenderSettingsWindow;
import org.pepetrace.Scene.Scene;

public class RTDenoiserTest {

    static final String CUBE1_PATH = "./src/main/resources/models/cube1.obj";

    public static void main(String[] args) throws Exception {
        new RTDenoiserTest().run();
    }

    void run() throws Exception {
        out("Creating Main…");
        Main main = new Main();
        Scene scene = GlobalState.getInstance().getScene();
        out("Main ready");

        // Load cube1.obj
        out("Loading cube1.obj…");
        while (scene.getModelCount() > 0) scene.removeModel(0);
        scene.loadModel(CUBE1_PATH, 0, "cube1");
        main.refreshSceneBuffers(true, true);
        out("Model loaded, triangleCount=" + scene.getTriangleCount());

        // Set render settings: 512x512, 1 sample/frame, 2 bounces, 16 maxSpp
        RenderSettingsWindow settings = GlobalState.getInstance().getViewportDrawer().getRenderSettings();
        settings.rtWidth = 512;
        settings.rtHeight = 512;
        settings.rtSamples.set(1);
        settings.rtBounces.set(2);
        settings.rtMaxSpp.set(16);
        out("Render settings: "
            + settings.rtWidth + "x" + settings.rtHeight
            + ", samples=" + settings.rtSamples.get()
            + ", bounces=" + settings.rtBounces.get()
            + ", maxSpp=" + settings.rtMaxSpp.get());

        RTDrawer drawer = main.renderDrawer;

        // Open F12 render window
        main.testF12 = true;
        main.runOneFrame();
        out("Render window opened");

        // Run a few frames and check if the texture is getting updated
        for (int i = 0; i < 5; i++) {
            main.runOneFrame();
        }
        main.renderWindow.setActive();
        // Read display texture via glReadPixels from the display FBO
        ByteBuffer dispPx = ByteBuffer.allocateDirect(4);
        glBindFramebuffer(GL_FRAMEBUFFER, drawer.getDisplayFbo());
        glReadPixels(0, 0, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, dispPx);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        int dr = dispPx.get(0) & 0xff, dg = dispPx.get(1) & 0xff, db = dispPx.get(2) & 0xff;
        out("Before denoise: display[0]=" + dr + "," + dg + "," + db);

        // Run frames until done, with a safety timeout
        long deadline = System.nanoTime() + 30_000_000_000L; // 30 seconds
        int frames = 0;
        while (!drawer.isDenoised() && System.nanoTime() < deadline) {
            main.runOneFrame();
            frames++;
            if (frames % 20 == 0) {
                out("Frame " + frames + " (frameId=" + drawer.getFrameId() + ")");
            }
        }

        long elapsedMs = (System.nanoTime() - (deadline - 30_000_000_000L)) / 1_000_000;
        out("Ran " + frames + " frames in " + elapsedMs + " ms");

        if (!drawer.isDenoised()) {
            out("FAIL: render did not complete within timeout (frameId=" + drawer.getFrameId() + ")");
            System.exit(1);
        }

        if (!drawer.isDenoiseSucceeded()) {
            out("FAIL: denoiser reported failure");
            System.exit(1);
        }

        out("PASS: denoised=true, denoiseSucceeded=true, frameId=" + drawer.getFrameId());

        // Read back a few pixels to verify the denoised image is not black
        // (glReadPixels from the display FBO works correctly, unlike float readback from RGBA32F)
        main.renderWindow.setActive();
        int w = 512, h = 512;
        ByteBuffer pixelBuf = ByteBuffer.allocateDirect(4);
        float maxVal = 0f;
        int[][] samples = {{w/2, h/2}, {0, 0}, {w-1, 0}, {0, h-1}, {w-1, h-1}};
        glBindFramebuffer(GL_FRAMEBUFFER, drawer.getDisplayFbo());
        for (int[] p : samples) {
            pixelBuf.rewind();
            glReadPixels(p[0], p[1], 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixelBuf);
            float r = (pixelBuf.get(0) & 0xff) / 255f;
            float g = (pixelBuf.get(1) & 0xff) / 255f;
            float b = (pixelBuf.get(2) & 0xff) / 255f;
            maxVal = Math.max(maxVal, Math.max(r, Math.max(g, b)));
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        if (maxVal < 0.001f) {
            out("FAIL: denoised image is black (max pixel=" + maxVal + ")");
            System.exit(1);
        } else {
            out("Denoised image non-black (max pixel=" + maxVal + ")");
        }

        // Clean up
        glfwSetWindowShouldClose(main.renderWindow.getId(), true);
        main.runOneFrame();
        glfwTerminate();

        out("ALL CHECKS PASSED");
    }

    static void out(String msg) {
        System.out.println("[DENOISE] " + msg);
    }
}
