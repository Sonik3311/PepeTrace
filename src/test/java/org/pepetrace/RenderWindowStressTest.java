package org.pepetrace;

import static org.lwjgl.glfw.GLFW.*;

import java.util.Random;
import org.pepetrace.Scene.Scene;

/**
 * Stress-test: open/close render window rapidly in a loop.
 * Run via: ./gradlew runStressTest
 */
public class RenderWindowStressTest {

    static final String[] MODEL_PATHS = {
        null,
        "/models/cube1.obj",
        "/models/cube3.obj"
    };
    static final String[] MODEL_NAMES = {
        null, "cube1", "cube3"
    };

    public static void main(String[] args) throws Exception {
        new RenderWindowStressTest().run();
    }

    void run() throws Exception {
        Random rng = new Random(42);

        out("creating Main…");
        Main main = new Main();
        Scene scene = GlobalState.getInstance().getScene();
        out("Main ready");

        long deadline = System.nanoTime() + 5_000_000_000L;
        long worstNs = 0;
        int cycles = 0;

        while (System.nanoTime() < deadline) {
            long cycleStart = System.nanoTime();

            while (scene.getModelCount() > 0) scene.removeModel(0);
            int choice = rng.nextInt(3);
            if (choice > 0) {
                scene.loadModel(MODEL_PATHS[choice], 0, MODEL_NAMES[choice]);
            }
            main.refreshSceneBuffers(true, true);

            main.viewportCamera.getPosition().set(
                rng.nextFloat() * 20f - 10f,
                rng.nextFloat() * 10f - 5f,
                rng.nextFloat() * 20f - 10f
            );
            main.viewportCamera.getYawPitch().set(
                rng.nextFloat() * 360f,
                rng.nextFloat() * 180f - 90f
            );

            main.testF12 = true;
            main.runOneFrame();
            main.runOneFrame();

            glfwSetWindowShouldClose(main.renderWindow.getId(), true);
            main.runOneFrame();

            long elapsed = System.nanoTime() - cycleStart;
            if (elapsed > worstNs) worstNs = elapsed;
            cycles++;

            if (elapsed > 500_000_000L) {
                out("FREEZE: cycle " + cycles + " took " + (elapsed / 1_000_000) + " ms");
            }

            int delayMs = rng.nextInt(51);
            if (delayMs > 0) Thread.sleep(delayMs);
        }

        out("Done: " + cycles + " cycles in 5 s, worst=" + (worstNs / 1_000_000) + " ms");
        glfwTerminate();
    }

    static void out(String msg) {
        System.out.println("[STRESS] " + msg);
    }
}
