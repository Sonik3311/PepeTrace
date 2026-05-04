package org.pepetrace.GUI;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImDrawFlags;
import imgui.flag.ImGuiSelectableFlags;
import org.pepetrace.Buffers.Texture;
import org.pepetrace.Scene.Material.TextureMaterial;
import org.pepetrace.Scene.Scene;

import java.util.ArrayList;
import java.util.Objects;

public class MaterialManagerWindow implements GuiWindow {
    private final int THUMB_SIZE = 48;
    private final int MAX_THUMB_SIZE = 48;
    private final int MIN_THUMB_SIZE = 8;
    private final CreateMaterialPopup createMaterialPopup;

    public MaterialManagerWindow() {
        programState.initializeArbitraryData("selectedMaterialIndex", 0);
        createMaterialPopup = new CreateMaterialPopup(mat -> {
            Scene scene = programState.getScene();
            scene.addMaterial(mat);
            int newIndex = scene.getMaterials().size() - 1;
            programState.setArbitraryData("selectedMaterialIndex", newIndex);
        });
    }

    public void render(int windowFlags) {
        ImGui.begin("Material Manager", windowFlags);
        renderMaterialList();
        renderActionButtons();
        createMaterialPopup.render(0);
        ImGui.end();
    }

    private void renderMaterialList() {
        Scene scene = programState.getScene();
        ArrayList<TextureMaterial> materials = scene.getMaterials();
        int selectedMaterialIndex = (int) programState.getArbitraryData("selectedMaterialIndex");
        ImGui.beginChild("MaterialListRegion", 0, -40, true);

        final float ITEM_SPACING_BASE = 8.0f;
        final float THUMB_SIZE_REF = THUMB_SIZE;
        final float BORDER_THICKNESS = 1.5f;
        final float ROUNDING_RADIUS = 5.0f;
        final int BORDER_COLOR = ImGui.getColorU32(0.3f, 0.3f, 0.3f, 1.0f);
        // White for the image tint
        final int IMAGE_TINT = ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f);


        for (int i = 0; i < materials.size(); i++) {
            TextureMaterial mat = materials.get(i);
            boolean isSelected = (i == selectedMaterialIndex);

            ImGui.pushID(i);

            // Available horizontal space
            float availWidth = ImGui.getContentRegionAvailX();

            // Label text and its width
            String label = i + ":";
            float textWidth = ImGui.calcTextSize(label).x;

            // ---- Compute thumbnail size that fits:
            // text + 3 thumbs + 4 spacings (spacing scales with thumb size)
            float k = ITEM_SPACING_BASE / THUMB_SIZE_REF;   // spacing factor
            float denom = 3.0f + 4.0f * k;
            float thumbSize = (availWidth - textWidth) / denom;
            thumbSize = Math.max(MIN_THUMB_SIZE, Math.min(MAX_THUMB_SIZE, thumbSize));

            float spacing = k * thumbSize;          // actual scaled spacing
            float lineHeight = thumbSize + 4.0f;   // vertical padding

            // ---- Invisible selectable covering the whole block ----
            float startX = ImGui.getCursorPosX() + spacing * 0.5f;
            float startY = ImGui.getCursorPosY();

            ImGui.selectable("##material" + i, isSelected,
                    ImGuiSelectableFlags.SpanAllColumns |
                            ImGuiSelectableFlags.AllowItemOverlap,
                    availWidth, lineHeight);

            if (ImGui.isItemClicked()) {
                selectedMaterialIndex = i;
                programState.setArbitraryData("selectedMaterialIndex", selectedMaterialIndex);
            }

            // ---- Draw items in order: label, then three thumbnails ----
            // Label (leftmost)
            ImGui.setCursorPos(startX, startY + (lineHeight - ImGui.getFontSize()) * 0.5f);
            ImGui.text(label);

            // Helper to draw a thumb with rounded border
            float thumbY = startY + (lineHeight - thumbSize) * 0.5f;

            // Albedo
            float albedoX = startX + textWidth + spacing;
            ImGui.setCursorPos(albedoX, thumbY);
            ImVec2 min = new ImVec2(ImGui.getCursorScreenPos());
            ImVec2 max = new ImVec2(min.x + thumbSize, min.y + thumbSize);

            // Draw rounded image (this actually clips the corners)
            ImGui.getWindowDrawList().addImageRounded(
                    mat.getAlbedoTexture().id,
                    min, max,
                    new ImVec2(0, 0), new ImVec2(1, 1),
                    IMAGE_TINT, ROUNDING_RADIUS,
                    ImDrawFlags.RoundCornersAll);
            // Draw the border on top
            ImGui.getWindowDrawList().addRect(
                    min.x, min.y, max.x, max.y,
                    BORDER_COLOR, ROUNDING_RADIUS,
                    ImDrawFlags.RoundCornersAll, BORDER_THICKNESS);
            // Tooltip via manual rectangle hover check
            if (ImGui.isMouseHoveringRect(min.x, min.y, max.x, max.y)) {
                ImGui.setTooltip("Albedo: " + getShortPath(mat.getAlbedoTexture()));
            }


            // Normal
            float normalX = albedoX + thumbSize + spacing;
            ImGui.setCursorPos(normalX, thumbY);
            min = new ImVec2(ImGui.getCursorScreenPos());
            max = new ImVec2(min.x + thumbSize, min.y + thumbSize);
            ImGui.getWindowDrawList().addImageRounded(
                    mat.getNormalTexture().id,
                    min, max,
                    new ImVec2(0, 0), new ImVec2(1, 1),
                    IMAGE_TINT, ROUNDING_RADIUS,
                    ImDrawFlags.RoundCornersAll);
            ImGui.getWindowDrawList().addRect(
                    min.x, min.y, max.x, max.y,
                    BORDER_COLOR, ROUNDING_RADIUS,
                    ImDrawFlags.RoundCornersAll, BORDER_THICKNESS);
            if (ImGui.isMouseHoveringRect(min.x, min.y, max.x, max.y)) {
                ImGui.setTooltip("Normal: " + getShortPath(mat.getNormalTexture()));
            }

            // RMT
            float rmtX = normalX + thumbSize + spacing;
            ImGui.setCursorPos(rmtX, thumbY);
            min = new ImVec2(ImGui.getCursorScreenPos());
            max = new ImVec2(min.x + thumbSize, min.y + thumbSize);
            ImGui.getWindowDrawList().addImageRounded(
                    mat.getRMTTexture().id,
                    min, max,
                    new ImVec2(0, 0), new ImVec2(1, 1),
                    IMAGE_TINT, ROUNDING_RADIUS,
                    ImDrawFlags.RoundCornersAll);
            ImGui.getWindowDrawList().addRect(
                    min.x, min.y, max.x, max.y,
                    BORDER_COLOR, ROUNDING_RADIUS,
                    ImDrawFlags.RoundCornersAll, BORDER_THICKNESS);
            if (ImGui.isMouseHoveringRect(min.x, min.y, max.x, max.y)) {
                ImGui.setTooltip("RMT: " + getShortPath(mat.getRMTTexture()));
            }


            // ---- Move cursor to the end of the block ----
            ImGui.setCursorPos(startX - spacing * 0.5f, startY + lineHeight);

            ImGui.popID();


            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();
        }

        ImGui.endChild();
    }

    private void oldrenderMaterialList() {
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
                programState.setArbitraryData("selectedMaterialIndex", selectedMaterialIndex);
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
            ImGui.setCursorPos(startX - itemSpacing / 2, startY + blockHeight);

            ImGui.popID();
        }

        ImGui.endChild();
    }

    private void renderActionButtons() {
        Scene scene = programState.getScene();
        ArrayList<TextureMaterial> materials = scene.getMaterials();
        int selectedMaterialIndex = (int) programState.getArbitraryData("selectedMaterialIndex");

        // A small horizontal layout
        ImGui.beginGroup();
        if (ImGui.button("Create Material")) {
            // Add a new default material – adjust the constructor to suit your project
            //TextureMaterial newMat = new TextureMaterial();
            //materials.add(newMat);
            // Select the newly created material
            //selectedMaterialIndex = materials.size() - 1;
            //programState.setArbitraryData("selectedMaterialIndex", selectedMaterialIndex);
            createMaterialPopup.open();
        }

        ImGui.sameLine();

        // Disable remove button if no material is selected or list is empty
        boolean canRemove = !materials.isEmpty() && selectedMaterialIndex >= 0;
        if (!canRemove) ImGui.beginDisabled();
        if (ImGui.button("Remove Selected")) {
            scene.removeMaterial(selectedMaterialIndex);
            //materials.remove(selectedMaterialIndex);
            // Adjust selection
            if (!materials.isEmpty()) {
                selectedMaterialIndex = Math.min(selectedMaterialIndex, materials.size() - 1);
                programState.setArbitraryData("selectedMaterialIndex", selectedMaterialIndex);
            } else {
                programState.setArbitraryData("selectedMaterialIndex", 0);
            }
        }
        if (!canRemove) ImGui.endDisabled();

        ImGui.endGroup();
    }


    private String getShortPath(Texture texture) {
        // Helper to extract filename from full path
        // Implementation depends on your Texture class
        return texture.getSourceFilePath(); // Placeholder
    }
}
