package org.pepetrace.GUI;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiSelectableFlags;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.pepetrace.Scene.ModelMetadata;
import org.pepetrace.Scene.Scene;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutlinerWindow implements GuiWindow {

    private final Map<Integer, Boolean> expandedState = new HashMap<>();

    @Override
    public void render(int windowFlags) {
        Scene scene = programState.getScene();
        List<ModelMetadata> models = scene.getModels();
        ImGui.begin("Outliner", windowFlags);

        ImGui.setNextWindowSizeConstraints(200, 100, 1000, 1000);
        if (ImGui.getWindowWidth() < 200) {
            ImGui.setWindowSize(250, 0, ImGuiCond.Once);
        }

        for (int i = 0; i < models.size(); i++) {
            ModelMetadata model = models.get(i);
            boolean isSelected = (i == programState.getSelectedModelIndex());
            boolean isExpanded = expandedState.getOrDefault(i, false);
            ImGui.pushID(i);

            float availWidth = ImGui.getContentRegionAvailX();
            float deleteButtonWidth = 50;
            float nameWidth = availWidth - deleteButtonWidth - 5;

            // Имя модели – клик переключает развёрнутость и выделяет
            if (ImGui.selectable(model.getName(), isSelected, ImGuiSelectableFlags.None, nameWidth, 0)) {
                expandedState.put(i, !isExpanded);
                programState.setSelectedModelIndex(i);
            }

            ImGui.sameLine();
            if (ImGui.button("X##del" + i, deleteButtonWidth, 0)) {
                scene.removeModel(i);
                programState.getViewportDrawer().refreshSceneBuffers();
                int selected = programState.getSelectedModelIndex();
                if (selected == i) programState.setSelectedModelIndex(-1);
                else if (selected > i) programState.setSelectedModelIndex(selected - 1);
                programState.getViewportDrawer().resetRender();
                // исправляем карту развёрнутости после удаления
                expandedState.clear();
                for (int j = 0; j < models.size(); j++) {
                    expandedState.put(j, expandedState.getOrDefault(j + (j >= i ? 1 : 0), false));
                }
            }

            if (isExpanded) {
                ImGui.indent();
                Vector3f pos = model.getPosition();
                float[] posArr = {pos.x, pos.y, pos.z};
                if (ImGui.dragFloat3("Position", posArr, 0.1f)) {
                    model.setPosition(new Vector3f(posArr[0], posArr[1], posArr[2]));
                    updateTransformations(scene);
                }

                Quaternionf rot = model.getRotation();
                float[] euler = {
                        (float) Math.toDegrees(rot.x),
                        (float) Math.toDegrees(rot.y),
                        (float) Math.toDegrees(rot.z)
                };
                if (ImGui.dragFloat3("Rotation (deg)", euler, 1.0f)) {
                    Quaternionf newRot = new Quaternionf().rotateXYZ(
                            (float) Math.toRadians(euler[0]),
                            (float) Math.toRadians(euler[1]),
                            (float) Math.toRadians(euler[2])
                    );
                    model.setRotation(newRot);
                    updateTransformations(scene);
                }

                Vector3f scale = model.getScale();
                float[] scaleArr = {scale.x, scale.y, scale.z};
                if (ImGui.dragFloat3("Scale", scaleArr, 0.1f, 0.01f, 10.0f)) {
                    model.setScale(new Vector3f(scaleArr[0], scaleArr[1], scaleArr[2]));
                    updateTransformations(scene);
                }
                ImGui.unindent();
            }

            ImGui.popID();
        }

        ImGui.end();
    }

    private void updateTransformations(Scene scene) {
        programState.getViewportDrawer().updateModelMatricesOnGPU(scene.getModels());
        programState.getViewportDrawer().resetRender();
    }
}