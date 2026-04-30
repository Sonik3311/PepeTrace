package org.pepetrace.GUI;

import imgui.ImGui;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.pepetrace.Scene.ModelMetadata;
import org.pepetrace.Scene.Scene;

import java.util.List;

public class ModelDataWindow implements GuiWindow {
    @Override
    public void render(int windowFlags) {
        Scene scene = programState.getScene();
        List<ModelMetadata> models = scene.getModels();
        ImGui.begin("Model Data");
        int selectedIdx = programState.getSelectedModelIndex();
        if (selectedIdx >= 0 && selectedIdx < models.size()) {
            ModelMetadata model = models.get(selectedIdx);

            // -- Name --
            /**byte[] nameBuf = new byte[128];
             String currentName = model.getName();
             if (currentName != null) {
             byte[] nameBytes = currentName.getBytes(java.nio.charset.StandardCharsets.UTF_8);
             System.arraycopy(nameBytes, 0, nameBuf, 0, Math.min(nameBytes.length, 127));
             }
             if (ImGui.inputText("Name", nameBuf)) {
             String newName = new String(nameBuf, java.nio.charset.StandardCharsets.UTF_8).trim();
             model.setName(newName);
             }*/

            // -- Position (Vector3f) --
            Vector3f pos = model.getPosition();
            float[] posArr = {pos.x, pos.y, pos.z};
            if (ImGui.dragFloat3("Position", posArr, 0.1f)) {
                model.setPosition(new Vector3f(posArr[0], posArr[1], posArr[2]));
            }

            // -- Rotation (Quaternionf -> Euler degrees -> edit -> back) --
            Quaternionf rot = model.getRotation();
            Vector3f eulerRad = rot.getEulerAnglesXYZ(new Vector3f());
            float[] eulerDeg = {
                    (float) Math.toDegrees(eulerRad.x),
                    (float) Math.toDegrees(eulerRad.y),
                    (float) Math.toDegrees(eulerRad.z)
            };
            if (ImGui.dragFloat3("Rotation", eulerDeg, 0.5f)) {
                model.setRotation(new Quaternionf().rotateXYZ(eulerDeg[0], eulerDeg[1], eulerDeg[2]));
            }
        } else {
            ImGui.textDisabled("No model selected");
        }
        ImGui.end();
    }
}
