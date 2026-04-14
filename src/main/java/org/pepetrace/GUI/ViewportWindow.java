package org.pepetrace.GUI;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.pepetrace.Buffers.Texture;
import org.pepetrace.Camera;
import org.pepetrace.Drawer;
import org.pepetrace.Scene.Scene;

import javax.swing.text.View;

public class ViewportWindow implements GuiWindow {

    public ViewportWindow() {
        programState.initializeArbitraryData("CPURenderTime", 0.0);
        programState.initializeArbitraryData("GPURenderTime", 0.0);
    }

    private static class AxisLine {
        float endX, endY;
        int color;
        float thickness;
        float depthZ; // camera-space Z of the endpoint (more negative = farther)
    }

    private void drawRotatedAxisWidget(
            float yawRad, float pitchRad,
            float widgetSizePx, float posLen, float negLen, float thickness
    ) {
        ImVec2 imageMin = ImGui.getItemRectMin();
        ImVec2 imageMax = ImGui.getItemRectMax();
        if (imageMin.x == 0 && imageMin.y == 0 && imageMax.x == 0 && imageMax.y == 0) return;

        float originX = imageMax.x - widgetSizePx / 2;
        float originY = imageMin.y + widgetSizePx / 2 - 3;

        float cosYaw = (float) Math.cos(yawRad);
        float sinYaw = (float) Math.sin(yawRad);
        float cosPitch = (float) Math.cos(pitchRad);
        float sinPitch = (float) Math.sin(pitchRad);

        // Camera-space direction vectors (X, Y, Z)
        float camXx = cosYaw;
        float camXy = sinYaw * sinPitch;
        float camXz = -sinYaw * cosPitch;   // Z component of X axis after rotation

        float camYx = 0;
        float camYy = cosPitch;
        float camYz = sinPitch;

        float camZx = sinYaw;
        float camZy = -cosYaw * sinPitch;
        float camZz = cosYaw * cosPitch;    // Z component of Z axis

        // Build list of lines (positive and negative)
        java.util.ArrayList<AxisLine> lines = new java.util.ArrayList<>();

        // Positive X (red)
        float endX_xp = originX + camXx * posLen;
        float endY_xp = originY - camXy * posLen;
        lines.add(createLine(endX_xp, endY_xp, 0xFF0000FF, thickness, camXz));

        // Negative X (dimmer red)
        float endX_xn = originX - camXx * negLen;
        float endY_xn = originY + camXy * negLen;
        lines.add(createLine(endX_xn, endY_xn, 0xAA0000FF, thickness, -camXz));

        // Positive Y (green)
        float endX_yp = originX + camYx * posLen;
        float endY_yp = originY - camYy * posLen;
        lines.add(createLine(endX_yp, endY_yp, 0xFF00FF00, thickness, camYz));

        // Negative Y (dimmer green)
        float endX_yn = originX - camYx * negLen;
        float endY_yn = originY + camYy * negLen;
        lines.add(createLine(endX_yn, endY_yn, 0xAA00FF00, thickness, -camYz));

        // Positive Z (blue)
        float endX_zp = originX + camZx * posLen;
        float endY_zp = originY - camZy * posLen;
        lines.add(createLine(endX_zp, endY_zp, 0xFFFF0000, thickness, camZz));

        // Negative Z (dimmer blue)
        float endX_zn = originX - camZx * negLen;
        float endY_zn = originY + camZy * negLen;
        lines.add(createLine(endX_zn, endY_zn, 0xAAFF0000, thickness, -camZz));

        // Sort by depth: farthest (most negative Z) first
        lines.sort((a, b) -> Float.compare(b.depthZ, a.depthZ));

        // Draw all lines in sorted order
        ImDrawList dl = ImGui.getWindowDrawList();
        for (AxisLine line : lines) {
            // Use solid line for positive, dashed for negative based on alpha
            boolean isPositive = (line.color >>> 24) >= 0xFF; // alpha > 0x33
            if (isPositive) {
                dl.addLine(originX, originY, line.endX, line.endY, line.color, line.thickness);
            } else {
                drawDashedLine(dl, originX, originY, line.endX, line.endY, line.color, line.thickness, 6f, 4f);
            }
        }
    }

    private AxisLine createLine(float endX, float endY, int color, float thickness, float depthZ) {
        AxisLine l = new AxisLine();
        l.endX = endX;
        l.endY = endY;
        l.color = color;
        l.thickness = thickness;
        l.depthZ = depthZ;
        return l;
    }

    private void drawDashedLine(ImDrawList dl, float x1, float y1, float x2, float y2, int color, float thickness, float dashLen, float gapLen) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float lineLen = (float) Math.sqrt(dx*dx + dy*dy);
        if (lineLen < 0.001f) return;

        float udx = dx / lineLen;
        float udy = dy / lineLen;

        float t = 0;
        boolean draw = true;
        while (t < lineLen) {
            float startT = t;
            float endT = Math.min(t + (draw ? dashLen : gapLen), lineLen);
            float segX1 = x1 + udx * startT;
            float segY1 = y1 + udy * startT;
            float segX2 = x1 + udx * endT;
            float segY2 = y1 + udy * endT;
            if (draw) {
                dl.addLine(segX1, segY1, segX2, segY2, color, thickness);
            }
            t = endT;
            draw = !draw;
        }
    }

    @Override
    public void render(int windowFlags) {
        Drawer drawer = programState.getViewportDrawer();
        Texture pathTracingTexture = drawer.getRenderTexture();
        ImGui.begin("Viewport");
        float renderViewportWidth = ImGui.getContentRegionAvailX();
        float renderViewportHeight = ImGui.getContentRegionAvailY();
        float windowPosX = ImGui.getWindowPosX();
        float windowPosY = ImGui.getWindowPosY();

        if (drawer.sizeChanged((int) renderViewportWidth, (int) renderViewportHeight)) {
            drawer.onResize((int) renderViewportWidth, (int) renderViewportHeight); // Это ужас, нужно править путём создания отдельного метода. Но оно работает и норм.
        }
        ImGui.image(pathTracingTexture.id, renderViewportWidth, renderViewportHeight, 0, 1, 1, 0);

        ImDrawList dl = ImGui.getWindowDrawList();
        float x = 13 + windowPosX, y = 30 + windowPosY;
        float xc = x + 0, yc = y + 20;
        float xt = x + 0, yt = yc + 20;

        double gpu = (double) programState.getArbitraryData("GPURenderTime");
        double cpu = (double) programState.getArbitraryData("CPURenderTime");
        int triangleCount = ((Scene) programState.getArbitraryData("Scene")).getTriangleCount();
        String gpuvalue = String.format("%.2f", Math.floor(gpu * 100) / 100);
        String cpuvalue = String.format("%.2f", Math.floor(cpu * 100) / 100);
        String trivalue = triangleCount >= 1000 ? String.format("%.2f", Math.floor((float) triangleCount / 1000 * 100) / 100) + "k" : Integer.toString(triangleCount);
        String gputext = "GPU Render Time: " + gpuvalue + " ms";
        String cputext = "CPU Render Time: " + cpuvalue + " ms";
        String tritext = "Triangle Count: " + trivalue;


        int textColor = ImGui.getColorU32(1, 1, 1, 1);   // white
        int outlineColor = ImGui.getColorU32(0, 0, 0, 1); // black
        int thickness = 2;
        for (int dx = -thickness; dx <= thickness; dx++) {
            for (int dy = -thickness; dy <= thickness; dy++) {
                // Skip the center (where the main text will go)
                if (dx == 0 && dy == 0) continue;
                dl.addText(xc + dx, yc + dy, outlineColor, cputext);
            }
        }
        dl.addText(xc, yc, textColor, cputext);
        for (int dx = -thickness; dx <= thickness; dx++) {
            for (int dy = -thickness; dy <= thickness; dy++) {
                // Skip the center (where the main text will go)
                if (dx == 0 && dy == 0) continue;
                dl.addText(x + dx, y + dy, outlineColor, gputext);
            }
        }
        dl.addText(x, y, textColor, gputext);
        dl.addText(xc, yc, textColor, cputext);
        for (int dx = -thickness; dx <= thickness; dx++) {
            for (int dy = -thickness; dy <= thickness; dy++) {
                // Skip the center (where the main text will go)
                if (dx == 0 && dy == 0) continue;
                dl.addText(xt + dx, yt + dy, outlineColor, tritext);
            }
        }
        dl.addText(xt, yt, textColor, tritext);


        Camera camera = programState.getCamera();
        float yaw = (float) (camera.getYawPitch().x / 180 * Math.PI);
        float pitch = (float) (camera.getYawPitch().y / 180 * Math.PI);
        drawRotatedAxisWidget(yaw, pitch, 80, 27, 30, 2);
        ImGui.end();

    }
}
