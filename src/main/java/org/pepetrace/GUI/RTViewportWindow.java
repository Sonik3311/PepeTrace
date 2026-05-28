package org.pepetrace.GUI;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;

public class RTViewportWindow implements GuiWindow {

    private int texId;
    private int albedoTexId;
    private int normalTexId;
    private int texW, texH;
    private int currentWidth, currentHeight;
    private int frameId, maxSamples, maxBounces, maxSpp;
    private long lastDispatchNs;
    private boolean done;
    private float etaSeconds;

    public void setState(int texId, int albedoTexId, int normalTexId,
                         int texW, int texH,
                         int currentWidth, int currentHeight,
                         int frameId, int maxSamples, int maxBounces,
                         int maxSpp, long lastDispatchNs, boolean done,
                         float etaSeconds) {
        this.texId = texId;
        this.albedoTexId = albedoTexId;
        this.normalTexId = normalTexId;
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
        this.etaSeconds = etaSeconds;
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
        String etaStr;
        if (etaSeconds > 0 && !done) {
            if (etaSeconds < 60) {
                etaStr = String.format("%5.1fs", etaSeconds);
            } else {
                int totalSec = (int) Math.ceil(etaSeconds);
                int h = totalSec / 3600;
                int m = (totalSec % 3600) / 60;
                int s = totalSec % 60;
                etaStr = h > 0
                    ? String.format("%dh %02dm %02ds", h, m, s)
                    : String.format("%02dm %02ds", m, s);
            }
        } else {
            etaStr = null;
        }
        if (maxSpp > 0) {
            info = String.format("SPP %3d/%-3d  |  Cycle %d  |  Frame %d  |  Samples %d  |  Bounces %d  |  GPU %5.1f ms",
                spp, maxSpp, cycle, frameId, maxSamples, maxBounces, lastDispatchNs / 1_000_000.0);
        } else {
            info = String.format("SPP %3d  |  Cycle %d  |  Frame %d  |  Samples %d  |  Bounces %d  |  GPU %5.1f ms",
                spp, cycle, frameId, maxSamples, maxBounces, lastDispatchNs / 1_000_000.0);
        }
        if (done) {
            info += "  |  Done";
        }
        if (etaStr != null) {
            info += "  |  ETA " + etaStr;
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

        float aspect = (float) texW / texH;
        float colW = contentW / 3;
        float imgW = colW - 4;
        float imgH = imgW / aspect;
        if (imgH > contentH) {
            imgH = contentH;
            imgW = imgH * aspect;
        }

        drawImagePanel(dl, 0, texId, "Color", barHeight, contentW, contentH, colW, imgW, imgH);
        drawImagePanel(dl, 1, albedoTexId, "Albedo", barHeight, contentW, contentH, colW, imgW, imgH);
        drawImagePanel(dl, 2, normalTexId, "Normal", barHeight, contentW, contentH, colW, imgW, imgH);

        ImGui.end();
        ImGui.popStyleColor();
    }

    private void drawImagePanel(ImDrawList dl, int col, int tex, String label,
                                 float barHeight, float contentW, float contentH,
                                 float colW, float imgW, float imgH) {
        float pad = 2;
        float x = col * colW + (colW - imgW) / 2;
        float y = barHeight + (contentH - imgH) / 2;

        // Border
        dl.addRect(
            ImGui.getWindowPosX() + x - pad,
            ImGui.getWindowPosY() + y - pad,
            ImGui.getWindowPosX() + x + imgW + pad,
            ImGui.getWindowPosY() + y + imgH + pad,
            ImGui.getColorU32(0.3f, 0.3f, 0.3f, 1.0f), 0, 0, 1.5f
        );

        // Image with flipped Y
        ImGui.setCursorPos(x, y);
        ImGui.image(tex, imgW, imgH, 0, 1, 1, 0);

        // Label
        float labelY = ImGui.getWindowPosY() + y + imgH + 2;
        float labelX = ImGui.getWindowPosX() + x + (imgW - ImGui.calcTextSize(label).x) / 2;
        dl.addText(ImGui.getFont(), 14, labelX + 1, labelY + 1,
            ImGui.getColorU32(0, 0, 0, 255), label);
        dl.addText(ImGui.getFont(), 14, labelX, labelY,
            ImGui.getColorU32(255, 255, 255, 255), label);
    }
}
