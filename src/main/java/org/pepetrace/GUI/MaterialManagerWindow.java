package org.pepetrace.GUI;

import imgui.ImGui;
import org.pepetrace.Buffers.Texture;
import org.pepetrace.Scene.Material.TextureMaterial;
import org.pepetrace.Scene.Scene;

import java.util.ArrayList;
import java.util.Objects;

public class MaterialManagerWindow implements GuiWindow {
    private int THUMB_SIZE = 64;

    public MaterialManagerWindow() {
        programState.initializeArbitraryData("selectedMaterialIndex", 0);
    }

    public void render(int windowFlags) {
        ImGui.begin("Material Manager", windowFlags);
        renderMaterialList();
        ImGui.end();
    }

    private void renderMaterialList() {
        Scene scene = programState.getScene();
        ArrayList<TextureMaterial> materials = scene.getMaterials();
        int selectedMaterialIndex = (int) programState.getArbitraryData("selectedMaterialIndex");
        ImGui.beginChild("MaterialListRegion", 0, -40, true);
        // Spacing between elements
        final float itemSpacing = 8.0f;
        final float thumbSize = THUMB_SIZE;
        final float lineHeight = thumbSize + 4.0f; // height per line (thumb + padding)

        for (int i = 0; i < materials.size(); i++) {
            TextureMaterial mat = materials.get(i);
            boolean isSelected = (i == selectedMaterialIndex);

            ImGui.pushID(i);

            // --- 1. Measure available width and element widths ---
            float availWidth = ImGui.getContentRegionAvailX();

            // Width of each thumbnail (same for all three)
            float albedoWidth = thumbSize;
            float normalWidth = thumbSize;
            float rmtWidth = thumbSize;

            // Width of the label text
            String label = "Material " + i;
            float textWidth = ImGui.calcTextSize(label).x;

            // Combine into a list for easy iteration
            float[] itemWidths = {albedoWidth, normalWidth, rmtWidth, textWidth};
            int itemCount = itemWidths.length;

            // --- 2. Calculate how many lines are needed ---
            float currentLineWidth = 0.0f;
            int linesNeeded = 1;

            for (int idx = 0; idx < itemCount; idx++) {
                float w = itemWidths[idx];
                // Add spacing before the item unless it's the first on the line
                float extra = (currentLineWidth > 0) ? itemSpacing : 0.0f;

                if (currentLineWidth + extra + w > availWidth && currentLineWidth > 0) {
                    // Move to next line
                    linesNeeded++;
                    currentLineWidth = w;
                } else {
                    currentLineWidth += extra + w;
                }
            }

            // Total block height = lines * lineHeight
            float blockHeight = linesNeeded * lineHeight;

            // --- 3. Draw invisible selectable covering the whole block ---
            float startX = ImGui.getCursorPosX() + (itemSpacing * 0.5f);
            float startY = ImGui.getCursorPosY();

            ImGui.selectable("##material" + i, isSelected,
                    imgui.flag.ImGuiSelectableFlags.SpanAllColumns |
                            imgui.flag.ImGuiSelectableFlags.AllowItemOverlap,
                    ImGui.getContentRegionAvailX(), blockHeight);

            if (ImGui.isItemClicked()) {
                selectedMaterialIndex = i;
            }

            // --- 4. Reset cursor to start and draw elements with wrapping ---
            ImGui.setCursorPos(startX, startY);

            float lineStartX = startX;
            float currentX = startX;
            float currentY = startY;
            currentLineWidth = 0.0f;

            for (int idx = 0; idx < itemCount; idx++) {
                float w = itemWidths[idx];
                float extra = (currentLineWidth > 0) ? itemSpacing : 0.0f;

                // If this item would overflow, wrap to next line
                if (currentLineWidth + extra + w > availWidth && currentLineWidth > 0) {
                    // Move to next line
                    currentY += lineHeight;
                    currentX = startX;
                    currentLineWidth = 0.0f;
                    extra = 0.0f; // no spacing at line start
                }

                // Position cursor for this item (vertically centred on the line)
                ImGui.setCursorPos(currentX + extra, currentY + (lineHeight - thumbSize) * 0.5f);

                // Draw the item
                if (idx == 0) {
                    ImGui.image(mat.getAlbedoTexture().id, thumbSize, thumbSize);
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip("Albedo: " + getShortPath(mat.getAlbedoTexture()));
                    }
                } else if (idx == 1) {
                    ImGui.image(mat.getNormalTexture().id, thumbSize, thumbSize);
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip("Normal: " + getShortPath(mat.getNormalTexture()));
                    }
                } else if (idx == 2) {
                    ImGui.image(mat.getRMTTexture().id, thumbSize, thumbSize);
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip("RMT: " + getShortPath(mat.getRMTTexture()));
                    }
                } else {
                    // Label – center it vertically using font size
                    ImGui.setCursorPosY(currentY + (lineHeight - ImGui.getFontSize()) * 0.5f);
                    ImGui.text(label);
                }

                // Update current X and line width
                currentX += extra + w;
                currentLineWidth += extra + w;
            }

            // --- 5. Move cursor to the end of the block ---
            ImGui.setCursorPos(startX, startY + blockHeight);

            ImGui.popID();
        }

        ImGui.endChild();
    }

    private String getShortPath(Texture texture) {
        // Helper to extract filename from full path
        // Implementation depends on your Texture class
        return texture.getSourceFilePath(); // Placeholder
    }
}
