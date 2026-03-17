package org.pepetrace;

import org.joml.Vector2f;
import org.joml.Vector3f;
import static org.lwjgl.glfw.GLFW.*;

public class Camera {
    private Vector3f position;
    private Vector2f yawPitch;
    private int cameraMode = 0;
    private float mouseSensitivity = 0.1f;
    private float moveSpeed = 0.05f;
    private boolean wasEscapePressed = false;

    private Vector3f targetPoint = new Vector3f(0, 0, 0);
    private float orbitRadius = 5.0f;
    private float orbitYaw = 0.0f;
    private float orbitPitch = 0.0f;

    private final UBOCamera ubo = new UBOCamera(3);;

    public Camera() {
        position = new Vector3f(0.0f, 0.0f, -5.0f);
        yawPitch = new Vector2f(0.0f, 0.0f);
        ubo.updateBuffer(position, yawPitch);
    }

    public Camera(Vector3f position, Vector2f yawPitch) {
        this.position = new Vector3f(0.0f, 0.0f, -5.0f);
        this.yawPitch = yawPitch;
        ubo.updateBuffer(position, yawPitch);
    }

    public void updateCamera(Window inputWindow) {

        boolean escapePressed = inputWindow.isKeyPressed(GLFW_KEY_ESCAPE);
        if (escapePressed && !wasEscapePressed) {
            cameraMode = (cameraMode == 0) ? 1 : 0;
            if (cameraMode == 0) {
                inputWindow.setCursorMode(Window.CURSOR_DISABLED);
                inputWindow.resetMouse();
            } else {
                inputWindow.setCursorMode(Window.CURSOR_NORMAL);
            }
        }
        wasEscapePressed = escapePressed;

        boolean shouldUpdateBuffer = false;
        switch (cameraMode) {
            case 0 -> shouldUpdateBuffer = freeCameraTransform(inputWindow);
            case 1 -> shouldUpdateBuffer = orbitCameraTransform(inputWindow);
            default -> throw new IllegalStateException("Unexpected value: " + cameraMode);
        }
        if (shouldUpdateBuffer) ubo.updateBuffer(position, yawPitch);
    }

    private boolean freeCameraTransform(Window inputWindow) {
        boolean hasUpdated = false;

        float[] mouseDelta = inputWindow.getMouseDelta();
        yawPitch.x += mouseDelta[0] * mouseSensitivity;
        yawPitch.y += mouseDelta[1] * mouseSensitivity;
        yawPitch.y = Math.max(-89.0f, Math.min(89.0f, yawPitch.y));

        if (mouseDelta[0] != 0 || mouseDelta[1] != 0) { hasUpdated = true; }

        float yawRad = (float)Math.toRadians(yawPitch.x);
        float pitchRad = (float) Math.toRadians(yawPitch.y);

        // Вектор направления взгляда (полный 3D)
        Vector3f forward = new Vector3f(
                (float) (Math.cos(pitchRad) * Math.sin(yawRad)),
                (float) Math.sin(pitchRad),
                (float) (Math.cos(pitchRad) * Math.cos(yawRad))
        ).normalize();

        // Вектор вправо (горизонтальный, перпендикулярный взгляду)
        Vector3f worldUp = new Vector3f(0, 1, 0);
        Vector3f right = new Vector3f();
        forward.cross(worldUp, right).normalize();
        if (right.length() < 0.1f) { // если смотрим строго вверх/вниз
            right.set(1, 0, 0);
        } else {
            right.normalize();
        }

        if (inputWindow.isKeyPressed(GLFW_KEY_W)) {
            position.add(forward.mul(moveSpeed, new Vector3f()));
            hasUpdated = true;
        }
        if (inputWindow.isKeyPressed(GLFW_KEY_S)) {
            position.sub(forward.mul(moveSpeed, new Vector3f()));
            hasUpdated = true;
        }
        if (inputWindow.isKeyPressed(GLFW_KEY_A)) {
            position.sub(right.mul(moveSpeed, new Vector3f()));
            hasUpdated = true;
        }
        if (inputWindow.isKeyPressed(GLFW_KEY_D)) {
            position.add(right.mul(moveSpeed, new Vector3f()));
            hasUpdated = true;
        }
        if (inputWindow.isKeyPressed(GLFW_KEY_SPACE)) {
            position.y += moveSpeed;
            hasUpdated = true;
        }
        if (inputWindow.isKeyPressed(GLFW_KEY_LEFT_CONTROL) || inputWindow.isKeyPressed(GLFW_KEY_RIGHT_CONTROL)) {
            position.y -= moveSpeed;
            hasUpdated = true;
        }

        return hasUpdated;
    }

    private boolean orbitCameraTransform(Window inputWindow) {
        boolean hasUpdated = false;

        double scroll = inputWindow.getScrollDelta();
        if (scroll != 0) {
            orbitRadius -= scroll * 0.5f;
            orbitRadius = Math.max(1.0f, Math.min(50.0f, orbitRadius));
            hasUpdated = true;
            System.out.println("Колесико: " + scroll + ", радиус: " + orbitRadius);
        }

        if (inputWindow.isMouseButtonPressed(Window.MOUSE_BUTTON_LEFT)) {
            float[] mouseDelta = inputWindow.getMouseDelta();
            System.out.println("ЛКМ зажата, мышь: " + mouseDelta[0] + ", " + mouseDelta[1]);

            orbitYaw += mouseDelta[0] * mouseSensitivity;
            orbitPitch += mouseDelta[1] * mouseSensitivity;
            orbitPitch = Math.max(-89.0f, Math.min(89.0f, orbitPitch));
            if (mouseDelta[0] != 0 || mouseDelta[1] != 0) hasUpdated = true;
        }

        if (hasUpdated) {
            float yawRad = (float) Math.toRadians(orbitYaw);
            float pitchRad = (float) Math.toRadians(orbitPitch);

            position.x = targetPoint.x + orbitRadius * (float) (Math.cos(pitchRad) * Math.sin(yawRad));
            position.y = targetPoint.y + orbitRadius * (float) Math.sin(pitchRad);
            position.z = targetPoint.z + orbitRadius * (float) (Math.cos(pitchRad) * Math.cos(yawRad));

            Vector3f dirToTarget = new Vector3f(targetPoint).sub(position).normalize();
            float newYaw = (float) Math.atan2(dirToTarget.x, dirToTarget.z);
            float newPitch = (float) Math.asin(dirToTarget.y);
            yawPitch.x = (float) Math.toDegrees(newYaw);
            yawPitch.y = (float) Math.toDegrees(newPitch);
            yawPitch.y = Math.max(-89.0f, Math.min(89.0f, yawPitch.y));
        }

        return hasUpdated;
    }

    public int getCameraMode() { return cameraMode; }
    public Vector2f getYawPitch() { return yawPitch; }
    public Vector3f getPosition() { return position; }
}
