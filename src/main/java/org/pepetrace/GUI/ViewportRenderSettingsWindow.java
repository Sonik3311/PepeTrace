package org.pepetrace.GUI;

import imgui.ImGui;
import org.pepetrace.Drawers.ViewportDrawer;
import org.pepetrace.GUI.GuiWindow;
import org.pepetrace.Util.ViewportRenderMode;

import java.util.Arrays;//

public class ViewportRenderSettingsWindow implements GuiWindow {

    @Override
    public void render(int windowFlags) {
        ViewportDrawer drawer = programState.getViewportDrawer();//;

        ImGui.begin("Render Settings");
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
        //String[] modeNames = Arrays.stream(ViewportRenderMode.values())
        //        .map(Enum::name)
        //.toArray(String[]::new);

        //if (ImGui.combo("Render Mode", drawer.renderMode, modeNames)) {
        //    drawer.frame = 0;
        //}
        ImGui.end();
    }
}
