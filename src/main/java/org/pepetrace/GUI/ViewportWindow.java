package org.pepetrace.GUI;

import imgui.ImDrawList;
import imgui.ImGui;
import org.pepetrace.Buffers.Texture;
import org.pepetrace.Drawer;
import org.pepetrace.Scene.Scene;

import javax.swing.text.View;

public class ViewportWindow implements GuiWindow {

    public ViewportWindow() {
        programState.initializeArbitraryData("CPURenderTime", 0.0);
        programState.initializeArbitraryData("GPURenderTime", 0.0);
    }

    @Override
    public void render(int windowFlags) {
        Drawer drawer = programState.getViewportDrawer();
        Texture pathTracingTexture = drawer.getRenderTexture();
        ImGui.begin("Viewport");
        float renderViewportWidth = ImGui.getContentRegionAvailX();
        float renderViewportHeight = ImGui.getContentRegionAvailY();
        float windowPosX = ImGui.getWindowPosX();
        float windowPosY = ImGui.getWindowPosY();

        if (drawer.sizeChanged((int) renderViewportWidth, (int) renderViewportHeight)) {
            drawer.onResize((int) renderViewportWidth, (int) renderViewportHeight); // Это ужас, нужно править путём создания отдельного метода. Но оно работает и норм.
        }
        ImGui.image(pathTracingTexture.id, renderViewportWidth, renderViewportHeight, 0, 1, 1, 0);

        ImDrawList dl = ImGui.getWindowDrawList();
        float x = 13 + windowPosX, y = 30 + windowPosY;
        float xc = x + 0, yc = y + 20;
        float xt = x + 0, yt = yc + 20;

        double gpu = (double) programState.getArbitraryData("GPURenderTime");
        double cpu = (double) programState.getArbitraryData("CPURenderTime");
        int triangleCount = ((Scene) programState.getArbitraryData("Scene")).getTriangleCount();
        String gpuvalue = String.format("%.2f", Math.floor(gpu * 100) / 100);
        String cpuvalue = String.format("%.2f", Math.floor(cpu * 100) / 100);
        String trivalue = triangleCount >= 1000 ? String.format("%.2f", Math.floor((float) triangleCount / 1000 * 100) / 100) + "k" : Integer.toString(triangleCount);
        String gputext = "GPU Render Time: " + gpuvalue + " ms";
        String cputext = "CPU Render Time: " + cpuvalue + " ms";
        String tritext = "Triangle Count: " + trivalue;


        int textColor = ImGui.getColorU32(1, 1, 1, 1);   // white
        int outlineColor = ImGui.getColorU32(0, 0, 0, 1); // black
        int thickness = 2;
        for (int dx = -thickness; dx <= thickness; dx++) {
            for (int dy = -thickness; dy <= thickness; dy++) {
                // Skip the center (where the main text will go)
                if (dx == 0 && dy == 0) continue;
                dl.addText(xc + dx, yc + dy, outlineColor, cputext);
            }
        }
        dl.addText(xc, yc, textColor, cputext);
        for (int dx = -thickness; dx <= thickness; dx++) {
            for (int dy = -thickness; dy <= thickness; dy++) {
                // Skip the center (where the main text will go)
                if (dx == 0 && dy == 0) continue;
                dl.addText(x + dx, y + dy, outlineColor, gputext);
            }
        }
        dl.addText(x, y, textColor, gputext);
        dl.addText(xc, yc, textColor, cputext);
        for (int dx = -thickness; dx <= thickness; dx++) {
            for (int dy = -thickness; dy <= thickness; dy++) {
                // Skip the center (where the main text will go)
                if (dx == 0 && dy == 0) continue;
                dl.addText(xt + dx, yt + dy, outlineColor, tritext);
            }
        }
        dl.addText(xt, yt, textColor, tritext);

        ImGui.end();

    }
}
