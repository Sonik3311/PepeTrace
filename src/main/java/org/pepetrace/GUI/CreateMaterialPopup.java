package org.pepetrace.GUI;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.pepetrace.Buffers.Texture;
import org.pepetrace.Scene.Material.TextureMaterial;

import java.util.ArrayList;
import java.util.function.Consumer;

public class CreateMaterialPopup implements GuiWindow {
    private boolean shouldOpen = false;
    private boolean isOpen = false;

    // Input buffers (ImString works like a mutable String for ImGui)
    private ImString materialName = new ImString("New Material", 256);
    private ImString albedoPath = new ImString("", 512);
    private ImString normalPath = new ImString("", 512);
    private ImString rmtPath = new ImString("", 512);

    private static final String TEXTURE_FILTERS = "*.png;*.jpg;*.jpeg;*.bmp;*.tga";
    private static final String FILTER_DESC = "Image files";

    // Callback when material is created – receives the new TextureMaterial
    private final Consumer<TextureMaterial> onMaterialCreated;

    public CreateMaterialPopup(Consumer<TextureMaterial> onMaterialCreated) {
        this.onMaterialCreated = onMaterialCreated;
    }

    /** Call this to show the popup (e.g. from a button). */
    public void open() {
        shouldOpen = true;
    }

    /** Returns true if the popup is currently visible. */
    public boolean isVisible() {
        return isOpen;
    }

    /** Must be called every frame. Renders the popup if it should be open. */
    @Override
    public void render(int windowFlags) {
        // 1. Open the popup if requested
        if (shouldOpen) {
            ImGui.openPopup("Create Material");
            shouldOpen = false;
            isOpen = true;
        }

        // 2. Render the modal (centered, always on top)
        if (ImGui.beginPopupModal("Create Material", ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.text("Define new material properties");
            ImGui.separator();

            // Material name
            ImGui.inputText("Name", materialName);

            // Texture paths with some padding
            ImGui.spacing();
            renderTextureInput("Albedo", albedoPath);
            renderTextureInput("Normal", normalPath);
            renderTextureInput("RMT", rmtPath);


            ImGui.spacing();
            ImGui.separator();

            // Buttons
            if (ImGui.button("OK", 120, 0)) {
                createMaterialAndClose();
            }
            ImGui.sameLine();
            if (ImGui.button("Cancel", 120, 0)) {
                ImGui.closeCurrentPopup();
                isOpen = false;
            }

            ImGui.endPopup();
        } else {
            // Popup was closed externally (e.g. Escape key)
            isOpen = false;
        }
    }

    /** Opens a file picker for image files and returns the chosen path (or null if cancelled) */
    private String openTextureFileDialog(String title) {
        // tinyfd_openFileDialog(title, defaultPath, numFilters, filterPatterns, singleFilterDesc, allowMulti)
        // We use the version with filters – LWJGL3's binding
        return TinyFileDialogs.tinyfd_openFileDialog(
                "Select " + title + " texture",
                System.getProperty("user.home"),   // default path
                null,                             // filter patterns – null means “all files”
                FILTER_DESC,
                false                              // single file only
        );

        // If you want to use the filter string, you can create a PointerBuffer:
        // PointerBuffer filterBuf = PointerBuffer.allocateDirect(1);
        // filterBuf.put(0, MemoryUtil.memUTF8("*.png"));
        // But the simple version above is often enough. To be safe, we leave it null.
    }

    private void renderTextureInput(String label, ImString pathBuffer) {
        ImGui.text(label + ":");
        ImGui.sameLine();

        // Use a constant width – pick a value that fits typical file paths
        ImGui.pushItemWidth(300);
        ImGui.inputText("##" + label, pathBuffer);
        ImGui.popItemWidth();

        ImGui.sameLine();
        if (ImGui.button("...##" + label, 50, 0)) {
            String selected = openTextureFileDialog(label);
            if (selected != null) {
                pathBuffer.set(selected);
            }
        }
    }



    private void createMaterialAndClose() {
        // Build the material (adjust constructors / factory methods to your project)
        TextureMaterial mat = TextureMaterial.create(albedoPath.get(), normalPath.get(), rmtPath.get());

        // Optional: set a custom name / ID if your material supports it
        // mat.setName(materialName.get());

        // Notify caller (e.g. add to scene and select it)
        if (onMaterialCreated != null) {
            onMaterialCreated.accept(mat);
        }

        ImGui.closeCurrentPopup();
        isOpen = false;
    }
}
