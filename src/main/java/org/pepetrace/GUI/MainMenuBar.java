package org.pepetrace.GUI;

import imgui.ImGui;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.pepetrace.Drawer;
import org.pepetrace.Scene.Scene;
import static org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog;

public class MainMenuBar implements GuiWindow {


    public void render(int flags) {
        Scene scene = programState.getScene();
        Drawer drawer = programState.getViewportDrawer();
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("Save", "Ctrl+S")) {
                    // handle save
                }

                ImGui.endMenu();
            }if (ImGui.beginMenu("Edit")) {
                if (ImGui.menuItem("Open Model", "Ctrl+K+O")) {


                    try (MemoryStack stack = MemoryStack.stackPush()) {
                        PointerBuffer filters = stack.mallocPointer(2);
                        filters.put(stack.UTF8("*.obj"));
                        filters.put(stack.UTF8("*.fbx"));
                        filters.flip();

                        // 2. Вызываем диалог, передав наш буфер
                        String filePath = tinyfd_openFileDialog(
                                "Выберите модель",
                                "~",
                                filters,            // Передаем список фильтров
                                "Модели",      // Описание (будет видно в выпадающем списке)
                                false
                        );

                        if (filePath != null) {
                            System.out.println("Выбран: " + filePath);
                            scene.loadModel(filePath, 0);
                            scene.packScene(
                                    drawer.getGeometryBuffer(),
                                    drawer.getIndexBuffer(),
                                    drawer.getMaterialIndicesBuffer(),
                                    drawer.getMaterialHandlesBuffer()
                            );
                            drawer.resetRender();
                        }
                    }
                }
                // edit menu items
                ImGui.endMenu();
            }
            // ... other menus
            ImGui.endMainMenuBar();
        }
    }
}
