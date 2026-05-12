package org.pepetrace.GUI;

import imgui.ImGui;
import imgui.flag.*;
import org.pepetrace.Main;
import org.pepetrace.Scene.ModelMetadata;
import org.pepetrace.Scene.Scene;

import java.util.List;

import static org.pepetrace.GUI.GuiWindow.programState;

public class OutlinerWindow implements GuiWindow {

    @Override
    public void render(int windowFlags) {
        Scene scene = programState.getScene();
        List<ModelMetadata> models = scene.getModels();
        ImGui.begin("Outliner", windowFlags);

        ImGui.setNextWindowSizeConstraints(200, 100, 1000, 1000);
        if (ImGui.getWindowWidth() < 200) {
            ImGui.setWindowSize(250, 0, ImGuiCond.Once);
        }

        float availWidth = ImGui.getContentRegionAvailX();
        float availHeight = ImGui.getContentRegionAvailY();

        ImGui.pushStyleColor(ImGuiCol.TableRowBg, 0.17f, 0.17f, 0.19f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.TableRowBgAlt, 0.12f, 0.12f, 0.14f, 1.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0.0f, 0.0f);

        if (ImGui.beginTable("##SelectableTable", 2,
                ImGuiTableFlags.RowBg | ImGuiTableFlags.ScrollY | ImGuiTableFlags.NoBordersInBody | ImGuiTableFlags.NoSavedSettings,
                availWidth, availHeight)) {
            ImGui.tableSetupColumn("Name", ImGuiTableColumnFlags.None);
            ImGui.tableSetupColumn("", ImGuiTableColumnFlags.WidthFixed, 30.0f);

            for (int i = 0; i < models.size(); i++) {
                ImGui.tableNextRow();
                ImGui.tableSetColumnIndex(0);

                ModelMetadata model = models.get(i);
                boolean isSelected = programState.getSelectedModelIndices().contains(i);

                ImGui.pushID(i);
                if (ImGui.selectable(model.getName(), isSelected, ImGuiSelectableFlags.AllowDoubleClick)) {
                    if (ImGui.isKeyDown(ImGuiKey.LeftShift) || ImGui.isKeyDown(ImGuiKey.RightShift)) {
                        programState.toggleModelSelection(i);
                    } else {
                        programState.setSelectedModels(i);
                    }
                }

                ImGui.tableSetColumnIndex(1);
                if (ImGui.button("x", 25, 0)) {
                    scene.removeModel(i);
                    // Удаляем индекс из выделения через метод GlobalState
                    programState.removeSelectedModel(i);
                    // Обновляем буферы на GPU
                    Main mainProgram = (Main) programState.getArbitraryData("Main");
                    mainProgram.refreshSceneBuffers(true, true);
                    ImGui.popID();
                    break;
                }
                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip("Delete model");
                }
                ImGui.popID();
            }

            // Пустышки
            float rowHeight = ImGui.getTextLineHeightWithSpacing();
            int rowsInView = (int) (availHeight / rowHeight / 1.3);
            if (models.size() < rowsInView) {
                int missingRows = rowsInView - models.size();
                for (int i = 0; i < missingRows; i++) {
                    ImGui.tableNextRow();
                    ImGui.tableSetColumnIndex(0);
                    ImGui.dummy(0, rowHeight);
                    ImGui.tableSetColumnIndex(1);
                    ImGui.dummy(0, rowHeight);
                }
            }

            ImGui.endTable();
        }

        ImGui.popStyleVar();
        ImGui.popStyleColor(2);
        ImGui.end();
    }
}