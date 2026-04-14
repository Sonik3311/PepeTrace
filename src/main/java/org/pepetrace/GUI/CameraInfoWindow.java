package org.pepetrace.GUI;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.pepetrace.Camera;

public class CameraInfoWindow implements GuiWindow {

    @Override
    public void render(int windowFlags) {
        Camera camera = programState.getCamera();
        ImGui.begin("Camera Info", windowFlags);
        if (camera != null) {
            ImGui.text(
                    String.format(
                            "Mode: %s",
                            camera.getCameraMode() == 0 ? "Free" : "Orbit"
                    )
            );
            Vector3f pos = camera.getPosition();
            Vector2f rot = camera.getYawPitch();
            ImGui.text(String.format("Pos: (%.2f, %.2f, %.2f)", pos.x, pos.y, pos.z));
            ImGui.text(String.format("Yaw: %.2f Pitch: %.2f", rot.x, rot.y));
        }
        ImGui.end();
    }
}
