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
        boolean isChanged = false;
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
                isChanged = true;
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
                float rx = (float) Math.toRadians(eulerDeg[0]);
                float ry = (float) Math.toRadians(eulerDeg[1]);
                float rz = (float) Math.toRadians(eulerDeg[2]);

                model.setRotation(new Quaternionf().rotateXYZ(rx, ry, rz));
                isChanged = true;
            }

            // -- Scale (Vector3f) --
            Vector3f scale = model.getScale();
            float[] scaleArr = {scale.x, scale.y, scale.z};
            if (ImGui.dragFloat3("Scale", scaleArr, 0.1f)) {
                model.setScale(new Vector3f(scaleArr[0], scaleArr[1], scaleArr[2]));
                isChanged = true;
            }

        } else {
            ImGui.textDisabled("No model selected");
        }
        ImGui.end();

        if (isChanged) updateTransformations();
    }

    private void updateTransformations() {
        programState.getScene().updateModelMatricesOnGPU(programState.getViewportDrawer().getModelMatricesBuffer());
        programState.getViewportDrawer().resetRender();
    }
}
