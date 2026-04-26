package org.pepetrace.GUI;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.ImVec4;
import imgui.flag.*;
import imgui.type.ImBoolean;

import java.util.ArrayList;
import java.util.List;

public class OutlinerWindow implements GuiWindow {

    private List<String> items = new ArrayList<>();
    private int selectedIndex = -1;

    public OutlinerWindow() {
        for (int i = 1; i <= 20; i++) {   // try with 5 items to see empty rows fill
            items.add("Item " + i);
        }
    }


    @Override
    public void render(int windowFlags) {
        ImGui.begin("Outliner", windowFlags);
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
            for (int i = 0; i < items.size(); i++) {
                ImGui.tableNextRow();
                ImGui.tableSetColumnIndex(0);

                ImGui.pushID(i);
                boolean isSelected = (i == selectedIndex);
                if (ImGui.selectable(items.get(i), isSelected,
                        ImGuiSelectableFlags.SpanAllColumns |
                                ImGuiSelectableFlags.AllowDoubleClick)) {
                    selectedIndex = i;
                }
                ImGui.popID();
            }

            // ----- Пустышки для заполнения пустого пространства -----
            float rowHeight = ImGui.getTextLineHeightWithSpacing();
            int rowsInView = (int) (availHeight / rowHeight / 1.3); // Я совершил преступление, какой нахуй 1.3 множитель
            if (items.size() < rowsInView) {
                int missingRows = rowsInView - items.size();
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
