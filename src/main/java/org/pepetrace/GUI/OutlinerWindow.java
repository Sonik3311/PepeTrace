package org.pepetrace.GUI;

import imgui.ImGui;
import imgui.flag.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.pepetrace.Scene.ModelMetadata;
import org.pepetrace.Scene.Scene;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

        float availWidth  = ImGui.getContentRegionAvailX();
        float availHeight = ImGui.getContentRegionAvailY();

        // ----- 1. Цвета строк -----
        ImGui.pushStyleColor(ImGuiCol.TableRowBg,     0.17f, 0.17f, 0.19f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.TableRowBgAlt,  0.12f, 0.12f, 0.14f, 1.0f);

        // ----- 2. Убирание промежутков между строками -----
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0.0f, 0.0f);

        // ----- 3. Создание таблицы -----
        if (ImGui.beginTable("##SelectableTable", 1,
                ImGuiTableFlags.RowBg |
                        ImGuiTableFlags.ScrollY |
                        ImGuiTableFlags.NoBordersInBody |
                        ImGuiTableFlags.NoSavedSettings,
                availWidth, availHeight
        )) {
            ImGui.tableSetupColumn("", ImGuiTableFlags.None);

            // ----- 4. Реальные строки (кликабельные) -----
            for (int i = 0; i < models.size(); i++) {
                ImGui.tableNextRow();
                ImGui.tableSetColumnIndex(0);

                ModelMetadata model = models.get(i);
                boolean isSelected = (i == programState.getSelectedModelIndex());

                ImGui.pushID(i);
                if (ImGui.selectable(model.getName(), isSelected,
                        ImGuiSelectableFlags.SpanAllColumns |
                                ImGuiSelectableFlags.AllowDoubleClick)) {
                    programState.setSelectedModelIndex(i);
                }
                ImGui.popID();
            }

            // ----- Пустышки для заполнения пустого пространства -----
            float rowHeight = ImGui.getTextLineHeightWithSpacing();
            int rowsInView = (int) (availHeight / rowHeight / 1.3); // Я совершил преступление, какой нахуй 1.3 множитель
            if (models.size() < rowsInView) {
                int missingRows = rowsInView - models.size();
                for (int i = 0; i < missingRows; i++) {
                    ImGui.tableNextRow();
                    ImGui.tableSetColumnIndex(0);
                    ImGui.dummy(0, ImGui.getTextLineHeightWithSpacing());
                }
            }

            ImGui.endTable();
        }

        ImGui.popStyleVar();   // ItemSpacing
        ImGui.popStyleColor(2); // TableRow цвета

        ImGui.end();
    }
}