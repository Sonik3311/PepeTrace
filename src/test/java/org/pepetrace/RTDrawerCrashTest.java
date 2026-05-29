package org.pepetrace;

import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwTerminate;

import org.pepetrace.Scene.Scene;

/**
 * Automated test: load cube1.obj, open F12 render window, run frames,
 * detect crashes/freezes via per-frame timeout.
 *
 * Run via: ./gradlew runRtCrashTest
 */
public class RTDrawerCrashTest {

    static final String CUBE1_PATH = "/models/cube1.obj";

    public static void main(String[] args) throws Exception {
        new RTDrawerCrashTest().run();
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

        // Open F12 render window
        main.testF12 = true;
        main.runOneFrame();
        out("Render window opened");

        int warmupFrames = 3;
        int testFrames = 10;
        long perFrameTimeoutNs = 3_000_000_000L; // 3 seconds
        int passed = 0;
        int failed = 0;

        // Let a few frames render (warmup)
        for (int i = 0; i < warmupFrames; i++) {
            long t0 = System.nanoTime();
            main.runOneFrame();
            long elapsed = System.nanoTime() - t0;
            out("Warmup frame " + i + ": " + (elapsed / 1_000_000) + " ms");
            if (elapsed > perFrameTimeoutNs) {
                out("WARMUP FRAME " + i + " EXCEEDED TIMEOUT (" + (elapsed / 1_000_000) + " ms)");
            }
        }

        // Test frames
        for (int i = 0; i < testFrames; i++) {
            long t0 = System.nanoTime();
            main.runOneFrame();
            long elapsed = System.nanoTime() - t0;

            if (elapsed > perFrameTimeoutNs) {
                out("FAIL: frame " + i + " took " + (elapsed / 1_000_000) + " ms (freeze/crash detected)");
                failed++;
            } else {
                out("PASS: frame " + i + " took " + (elapsed / 1_000_000) + " ms");
                passed++;
            }
        }

        out("");
        out("=== RESULTS ===");
        out("Passed: " + passed + "/" + testFrames + " frames");
        out("Failed: " + failed + "/" + testFrames + " frames");

        glfwSetWindowShouldClose(main.renderWindow.getId(), true);
        main.runOneFrame();
        glfwTerminate();

        if (failed > 0) {
            out("CRASH DETECTED: " + failed + " frames exceeded timeout");
            System.exit(1);
        } else {
            out("ALL FRAMES PASSED");
        }
    }

    static void out(String msg) {
        System.out.println("[RTCRASH] " + msg);
    }
}
