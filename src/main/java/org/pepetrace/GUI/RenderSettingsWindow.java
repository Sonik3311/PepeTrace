package org.pepetrace.GUI;

import imgui.ImGui;
import imgui.type.ImInt;
import java.util.Arrays;
import org.pepetrace.Drawers.ViewportDrawer;
import org.pepetrace.Util.ViewportRenderMode;

public class RenderSettingsWindow implements GuiWindow {

    public final ImInt rtSamples = new ImInt(1);
    public final ImInt rtBounces = new ImInt(8);
    public int rtWidth = 1024;
    public int rtHeight = 1024;
    public final ImInt rtMaxSpp = new ImInt(16);

    @Override
    public void render(int windowFlags) {
        ViewportDrawer drawer = programState.getViewportDrawer(); //;

        ImGui.begin("Render Settings");

        ImGui.separatorText("Viewport");
        ImGui.spacing();

        if (ImGui.checkbox("Accumulate Frames", drawer.accumulateFrames)) {
            drawer.resetRender();
        }
        if (ImGui.checkbox("Ambient Occlusion", drawer.ambientOcclusion)) {
            drawer.resetRender();
        }
        ImGui.beginDisabled(!drawer.ambientOcclusion.get());
        if (ImGui.inputInt("AO Samples", drawer.ambientOcclusionSamples)) {
            int min = 1,
                max = 16384;
            int clamped = Math.clamp(
                drawer.ambientOcclusionSamples.get(),
                min,
                max
            );
            drawer.ambientOcclusionSamples.set(clamped);
            drawer.resetRender();
        }
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip(
                "Amount of rays sent out for ray-traced ambient occlusion effect. Values lower that 4 yield inaccurate results"
            );
        }
        ImGui.endDisabled();

        String[] modeNames = Arrays.stream(ViewportRenderMode.values())
            .map(Enum::name)
            .toArray(String[]::new);

        if (ImGui.combo("Render Mode", drawer.renderMode, modeNames)) {
            drawer.resetRender();
        }

        ImGui.spacing();
        ImGui.separatorText("Final render");
        if (ImGui.inputInt("Samples", rtSamples)) {
            int clamped = Math.max(1, rtSamples.get());
            rtSamples.set(clamped);
        }
        if (ImGui.inputInt("Max bounces", rtBounces)) {
            int clamped = Math.max(2, rtBounces.get());
            rtBounces.set(clamped);
        }
        if (ImGui.inputInt("Max SPP", rtMaxSpp)) {
            int clamped = Math.max(0, rtMaxSpp.get());
            rtMaxSpp.set(clamped);
        }
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip("0 = unlimited");
        }
        ImGui.spacing();
        int[] resolution = { rtWidth, rtHeight };
        if (ImGui.inputInt2("Render resolution", resolution)) {
            rtWidth = Math.max(64, resolution[0]);
            rtHeight = Math.max(64, resolution[1]);
        }

        ImGui.end();
    }
}
