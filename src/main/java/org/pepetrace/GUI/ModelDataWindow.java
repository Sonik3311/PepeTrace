package org.pepetrace.GUI;

import imgui.ImGui;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.pepetrace.Main;
import org.pepetrace.Scene.ModelMetadata;
import org.pepetrace.Scene.Scene;

import java.util.Set;

public class ModelDataWindow implements GuiWindow {

    private ImString nameEditBuffer = new ImString();

    @Override
    public void render(int windowFlags) {
        Scene scene = programState.getScene();
        Set<Integer> selectedIndices = programState.getSelectedModelIndices();
        boolean isTransformChanged = false;
        boolean isSceneChanged = false;

        ImGui.begin("Model Data");

        if (!selectedIndices.isEmpty()) {
            // Берем первую модель для отображения текущих значений (как образец)
            int firstIdx = selectedIndices.iterator().next();
            ModelMetadata firstModel = scene.getModels().get(firstIdx);

            // -- Name --
            nameEditBuffer.set(firstModel.getName());
            if (ImGui.inputText("Name", nameEditBuffer)) {
                String newName = nameEditBuffer.get();
                for (int idx : selectedIndices) {
                    scene.getModels().get(idx).setName(newName.isBlank() ? "Unnamed Model" : newName);
                }
                isTransformChanged = true;
            }

            ImGui.spacing();
            ImGui.separatorText("Transform");
            ImGui.spacing();

            // -- Position --
            Vector3f pos = firstModel.getPosition();
            float[] posArr = {pos.x, pos.y, pos.z};
            if (ImGui.dragFloat3("Position", posArr, 0.1f)) {
                Vector3f delta = new Vector3f(posArr[0] - pos.x, posArr[1] - pos.y, posArr[2] - pos.z);
                for (int idx : selectedIndices) {
                    ModelMetadata m = scene.getModels().get(idx);
                    m.setPosition(new Vector3f(m.getPosition()).add(delta));
                }
                isTransformChanged = true;
            }

            // -- Rotation --
            Quaternionf rot = firstModel.getRotation();
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
                Quaternionf newRot = new Quaternionf().rotateXYZ(rx, ry, rz);
                for (int idx : selectedIndices) {
                    scene.getModels().get(idx).setRotation(newRot);
                }
                isTransformChanged = true;
            }

            // -- Scale --
            Vector3f scale = firstModel.getScale();
            float[] scaleArr = {scale.x, scale.y, scale.z};
            if (ImGui.dragFloat3("Scale", scaleArr, 0.1f)) {
                for (int idx : selectedIndices) {
                    scene.getModels().get(idx).setScale(new Vector3f(scaleArr[0], scaleArr[1], scaleArr[2]));
                }
                isTransformChanged = true;
            }

            ImGui.spacing();
            ImGui.separatorText("Material");
            ImGui.spacing();

            // -- Material --
            int matCount = scene.getMaterials().size();
            String[] matLabels = new String[matCount];
            for (int i = 0; i < matCount; i++) {
                matLabels[i] = String.valueOf(i);
            }
            if (matCount > 0) {
                ImInt matIdx = new ImInt(firstModel.getMaterialIndex());
                if (ImGui.combo("Material", matIdx, matLabels, matCount)) {
                    for (int idx : selectedIndices) {
                        scene.setModelMaterial(idx, matIdx.get());
                    }
                    isSceneChanged = true;
                }
            } else {
                ImGui.textDisabled("No materials available");
            }
        } else {
            ImGui.textDisabled("No model selected");
        }

        ImGui.end();

        if (isTransformChanged || isSceneChanged) {
            updateTransformations();
            if (isSceneChanged) updateSceneBuffers();
        }
    }

    private void updateTransformations() {
        Main mainProgram = (Main) programState.getArbitraryData("Main");
        mainProgram.refreshSceneBuffers(false, true);
    }

    private void updateSceneBuffers() {
        Main mainProgram = (Main) programState.getArbitraryData("Main");
        mainProgram.refreshSceneBuffers(true, true);
    }
}