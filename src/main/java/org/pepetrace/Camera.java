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
    private boolean cursorLocked = true;
    private boolean wasEscapePressed = false;

    private UBOCamera ubo;

    public Camera() {
        position = new Vector3f(0.0f, 0.0f, -5.0f);
        yawPitch = new Vector2f(0.0f, 0.0f);
        ubo = new UBOCamera(3);
    }

    public Camera(Vector3f position, Vector2f yawPitch) {
        this.position = new Vector3f(0.0f, 0.0f, -5.0f);
        this.yawPitch = yawPitch;
        ubo = new UBOCamera(3);
    }

    public void setActive() {
        ubo.updateBuffer(this.position, this.yawPitch);
    }

    public void updateCamera(Window inputWindow) {

        boolean escapePressed = inputWindow.isKeyPressed(GLFW_KEY_ESCAPE);
        if (escapePressed && !wasEscapePressed) {
            cursorLocked = !cursorLocked;
            inputWindow.setCursorMode(cursorLocked ? Window.CURSOR_DISABLED : Window.CURSOR_NORMAL);
            inputWindow.resetMouse();
        }
        wasEscapePressed = escapePressed;

        if (cursorLocked) {
            boolean shouldUpdateBuffer = false;
            switch (cameraMode) {
                case 0 -> shouldUpdateBuffer = freeCameraTransform(inputWindow);
                case 1 -> shouldUpdateBuffer = orbitCameraTransform(inputWindow);
                default -> throw new IllegalStateException("Unexpected value: " + cameraMode);
            }
            if (shouldUpdateBuffer) ubo.updateBuffer(position, yawPitch);
        }
    }

    private boolean freeCameraTransform(Window inputWindow) {
        boolean hasUpdated = false;

        float[] mouseDelta = inputWindow.getMouseDelta();
        yawPitch.x += mouseDelta[0] * mouseSensitivity;
        yawPitch.y += mouseDelta[1] * mouseSensitivity;
        yawPitch.y = Math.max(-89.0f, Math.min(89.0f, yawPitch.y));

        if (mouseDelta[0] != 0 || mouseDelta[1] != 0) {
            hasUpdated = true;
        }

        float yawRad = (float)Math.toRadians(yawPitch.x);

        if (inputWindow.isKeyPressed(GLFW_KEY_W)) {
            position.x += (float)Math.sin(yawRad) * moveSpeed;
            position.z += (float)Math.cos(yawRad) * moveSpeed;
            hasUpdated = true;
        }
        if (inputWindow.isKeyPressed(GLFW_KEY_S)) {
            position.x -= (float)Math.sin(yawRad) * moveSpeed;
            position.z -= (float)Math.cos(yawRad) * moveSpeed;
            hasUpdated = true;
        }
        if (inputWindow.isKeyPressed(GLFW_KEY_A)) {
            position.x += (float)Math.sin(yawRad + Math.PI/2) * moveSpeed;
            position.z += (float)Math.cos(yawRad + Math.PI/2) * moveSpeed;
            hasUpdated = true;
        }
        if (inputWindow.isKeyPressed(GLFW_KEY_D)) {
            position.x += (float)Math.sin(yawRad - Math.PI/2) * moveSpeed;
            position.z += (float)Math.cos(yawRad - Math.PI/2) * moveSpeed;
            hasUpdated = true;
        }
        if (inputWindow.isKeyPressed(GLFW_KEY_E)) {
            position.y += moveSpeed;
            hasUpdated = true;
        }
        if (inputWindow.isKeyPressed(GLFW_KEY_Q)) {
            position.y -= moveSpeed;
            hasUpdated = true;
        }

        return hasUpdated;
    }

    private boolean orbitCameraTransform(Window inputWindow) {
        System.out.println("orbitCameraTransform не имплементирован");
        return false;
    }

    public Vector2f getYawPitch() {
        return yawPitch;
    }

    public Vector3f getPosition() {
        return position;
    }
}
