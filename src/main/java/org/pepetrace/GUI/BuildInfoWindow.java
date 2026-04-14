package org.pepetrace.GUI;

import imgui.ImGui;
import org.pepetrace.Util.Passport;

public class BuildInfoWindow implements GuiWindow {
    public void render(int windowFlags) {
        ImGui.begin("Build info", windowFlags);
        ImGui.text(String.format("Build No. %s", Passport.INSTANCE.getBuildNumber()));
        ImGui.text(String.format("OS: %s", Passport.INSTANCE.getBuildOS()));
        ImGui.text(String.format("Build timestamp: %s", Passport.INSTANCE.getBuildTime()));
        ImGui.text(String.format("Java: %s", Passport.INSTANCE.getJavaVersion()));
        ImGui.text(String.format("Git branch: %s", Passport.INSTANCE.getGitBranchHash()));
        ImGui.end();
    }
}
