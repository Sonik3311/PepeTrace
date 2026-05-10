package org.pepetrace.GUI;

import imgui.ImGui;
import org.pepetrace.Drawers.ViewportDrawer;
import org.pepetrace.Util.ViewportRenderMode;

import java.util.Arrays;

public class RenderSettingsWindow implements GuiWindow {

    @Override
    public void render(int windowFlags) {
        ViewportDrawer drawer = programState.getViewportDrawer();//;

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
            int min = 1, max = 16384;
            int clamped = Math.clamp(drawer.ambientOcclusionSamples.get(), min, max);
            drawer.ambientOcclusionSamples.set(clamped);
            drawer.resetRender();
        }
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip("Amount of rays sent out for ray-traced ambient occlusion effect. Values lower that 4 yield inaccurate results");
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
        //if (ImGui.inputInt("Samples", drawer.samples)) {
        //    int min = 1, max = 16384;
        //    int clamped = Math.clamp(drawer.samples.get(), min, max);
        //    drawer.samples.set(clamped);
        //    drawer.frame = 0;
        //}
        //if (ImGui.checkbox("Accumulate frames", drawer.accumulating)) {
        //    if (!drawer.accumulating.get()) {
        //        drawer.frame = 0;
        //    }
        //}
        //if (ImGui.button("Reset Accumulation")) {
        //    drawer.frame = 0;
        //}

        ImGui.end();
    }
}
