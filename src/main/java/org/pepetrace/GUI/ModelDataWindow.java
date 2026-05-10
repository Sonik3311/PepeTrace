package org.pepetrace.GUI;

import imgui.ImGui;
import imgui.type.ImString;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.pepetrace.Main;
import org.pepetrace.Scene.ModelMetadata;
import org.pepetrace.Scene.Scene;

import java.util.List;

public class ModelDataWindow implements GuiWindow {

    private ImString nameEditBuffer = new ImString();


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
            nameEditBuffer.set(model.getName());
            if (ImGui.inputText("Name", nameEditBuffer)) {
                String newName = nameEditBuffer.get();
                if (newName.isBlank()) {
                    model.setName("Unnamed Model");
                } else {
                    model.setName(newName);
                }
             }
            ImGui.spacing();
            ImGui.separatorText("Transform");
            ImGui.spacing();

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
        Main mainProgram = (Main) programState.getArbitraryData("Main");
        mainProgram.refreshSceneBuffers(false, true);
    }
}
