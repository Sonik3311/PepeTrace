package org.pepetrace.GUI;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;

public class RTViewportWindow implements GuiWindow {

    private int texId;
    private int texW, texH;
    private int currentWidth, currentHeight;
    private int frameId, maxSamples, maxBounces, maxSpp;
    private long lastDispatchNs;
    private boolean done;

    public void setState(int texId, int texW, int texH,
                         int currentWidth, int currentHeight,
                         int frameId, int maxSamples, int maxBounces,
                         int maxSpp, long lastDispatchNs, boolean done) {
        this.texId = texId;
        this.texW = texW;
        this.texH = texH;
        this.currentWidth = currentWidth;
        this.currentHeight = currentHeight;
        this.frameId = frameId;
        this.maxSamples = maxSamples;
        this.maxBounces = maxBounces;
        this.maxSpp = maxSpp;
        this.lastDispatchNs = lastDispatchNs;
        this.done = done;
    }

    @Override
    public void render(int windowFlags) {
        int flags = windowFlags
            | ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize
            | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoCollapse
            | ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoScrollbar
            | ImGuiWindowFlags.NoScrollWithMouse | ImGuiWindowFlags.NoInputs;
        ImGui.setNextWindowPos(0, 0);
        ImGui.setNextWindowSize(currentWidth, currentHeight);
        ImGui.pushStyleColor(ImGuiCol.WindowBg, 0.15f, 0.15f, 0.15f, 1.0f);
        ImGui.begin("RT Viewport", flags);

        int spp = (frameId / 4) * maxSamples;
        int cycle = (frameId % 4) + 1;
        String info;
        if (maxSpp > 0) {
            info = String.format("SPP %d/%d  |  Cycle %d  |  Frame %d  |  Samples %d  |  Bounces %d  |  GPU %.1f ms",
                spp, maxSpp, cycle, frameId, maxSamples, maxBounces, lastDispatchNs / 1_000_000.0);
        } else {
            info = String.format("SPP %d  |  Cycle %d  |  Frame %d  |  Samples %d  |  Bounces %d  |  GPU %.1f ms",
                spp, cycle, frameId, maxSamples, maxBounces, lastDispatchNs / 1_000_000.0);
        }
        if (done) {
            info += "  |  Done";
        }
        info += "    [Ctrl+S to save]";

        // Draw menu-bar-like overlay at top of window using ImDrawList
        ImDrawList dl = ImGui.getWindowDrawList();
        ImVec2 winPos = new ImVec2();
        ImGui.getWindowPos(winPos);
        float barHeight = 26;
        dl.addRectFilled(winPos.x, winPos.y, winPos.x + currentWidth, winPos.y + barHeight,
            ImGui.getColorU32(0.08f, 0.08f, 0.08f, 1.0f));
        float padX = 13;
        float textY = winPos.y + (barHeight - 14) / 2;
        dl.addText(ImGui.getFont(), 14, winPos.x + padX + 1, textY + 1,
            ImGui.getColorU32(0, 0, 0, 255), info);
        dl.addText(ImGui.getFont(), 14, winPos.x + padX, textY,
            ImGui.getColorU32(255, 255, 255, 255), info);

        // Content starts below the menu bar
        ImGui.setCursorPos(0, barHeight);
        float contentW = ImGui.getContentRegionAvailX();
        float contentH = ImGui.getContentRegionAvailY();
        float texAspect = (float) texW / texH;
        float areaAspect = contentW / contentH;

        float imgW, imgH;
        if (texAspect > areaAspect) {
            imgW = contentW;
            imgH = contentW / texAspect;
        } else {
            imgH = contentH;
            imgW = contentH * texAspect;
        }

        float offX = (contentW - imgW) * 0.5f + ImGui.getCursorPosX();
        float offY = (contentH - imgH) * 0.5f + ImGui.getCursorPosY();
        ImGui.setCursorPos(offX, offY);

        ImVec2 imageMin = new ImVec2(offX - 1, offY - 1);
        ImVec2 imageMax = new ImVec2(offX + imgW + 1, offY + imgH + 1);
        ImGui.getWindowDrawList().addRect(imageMin, imageMax, ImGui.getColorU32(0.3f, 0.3f, 0.3f, 1.0f), 0, 0, 1.5f);
        ImGui.image(texId, imgW, imgH, 0, 1, 1, 0);

        ImGui.end();
        ImGui.popStyleColor();
    }
}
