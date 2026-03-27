package org.pepetrace;

import org.joml.Vector2f;
import org.joml.Vector3f;
import static org.lwjgl.glfw.GLFW.*;

public class Camera {
    // Position and orientation
    private Vector3f position;
    private Vector2f yawPitch;          // x = yaw, y = pitch (degrees)
    private int cameraMode = 0;         // 0 = free flight, 1 = orbit

    // Control parameters
    private float mouseSensitivity = 0.1f;
    private float moveSpeed = 0.05f;

    // Orbit mode state
    private Vector3f orbitTargetPoint = new Vector3f(0, 0, 0);
    private float orbitDistance = 5.0f;          // distance from camera to target
    private float orbitYaw = 0.0f;
    private float orbitPitch = 0.0f;
    private boolean wasLeftMousePressed = false; // to detect first press in orbit

    // Escape toggling
    private boolean wasEscapePressed = false;

    // UBO for GPU
    private final UBOCamera ubo = new UBOCamera(3);

    public Camera() {
        position = new Vector3f(0.0f, 0.0f, -5.0f);
        yawPitch = new Vector2f(0.0f, 0.0f);
        ubo.updateBuffer(position, yawPitch);
    }

    public Camera(Vector3f position, Vector2f yawPitch) {
        this.position = new Vector3f(position);
        this.yawPitch = new Vector2f(yawPitch);
        ubo.updateBuffer(position, yawPitch);
    }

    // Called every frame from Main
    public void updateCamera(Window inputWindow) {
        // Mode switching with Escape
        boolean escapePressed = inputWindow.isKeyPressed(GLFW_KEY_ESCAPE);
        if (escapePressed && !wasEscapePressed) {
            cameraMode = (cameraMode == 0) ? 1 : 0;

            if (cameraMode == 0) {
                // Switch to free flight: disable cursor
                inputWindow.setCursorMode(Window.CURSOR_DISABLED);
                inputWindow.resetMouse();
            } else {
                // Switch to orbit: enable cursor and compute target point
                inputWindow.setCursorMode(Window.CURSOR_NORMAL);
                updateOrbitTargetFromCurrentView();
                wasLeftMousePressed = false;
            }
        }
        wasEscapePressed = escapePressed;

        boolean shouldUpdateBuffer = false;
        switch (cameraMode) {
            case 0 -> shouldUpdateBuffer = freeCameraTransform(inputWindow);
            case 1 -> shouldUpdateBuffer = orbitCameraTransform(inputWindow);
            default -> throw new IllegalStateException("Unexpected mode: " + cameraMode);
        }
        if (shouldUpdateBuffer) {
            ubo.updateBuffer(position, yawPitch);
        }
    }

    // ---------- Free flight mode ----------
    private boolean freeCameraTransform(Window inputWindow) {
        boolean hasUpdated = false;

        // Mouse look
        float[] mouseDelta = inputWindow.getMouseDelta();
        yawPitch.x += mouseDelta[0] * mouseSensitivity;
        yawPitch.y += mouseDelta[1] * mouseSensitivity;
        yawPitch.y = Math.max(-89.0f, Math.min(89.0f, yawPitch.y));
        if (mouseDelta[0] != 0 || mouseDelta[1] != 0) hasUpdated = true;

        float yawRad = (float) Math.toRadians(yawPitch.x);
        float pitchRad = (float) Math.toRadians(yawPitch.y);

        // Forward direction (full 3D)
        Vector3f forward = new Vector3f(
                (float) (Math.cos(pitchRad) * Math.sin(yawRad)),
                (float) Math.sin(pitchRad),
                (float) (Math.cos(pitchRad) * Math.cos(yawRad))
        ).normalize();

        // Right vector (horizontal, perpendicular to forward)
        Vector3f worldUp = new Vector3f(0, 1, 0);
        Vector3f right = new Vector3f();
        forward.cross(worldUp, right);
        if (right.length() < 0.1f) {
            right.set(1, 0, 0); // fallback when looking straight up/down
        } else {
            right.normalize();
        }

        // Movement
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
        if (inputWindow.isKeyPressed(GLFW_KEY_LEFT_CONTROL) ||
                inputWindow.isKeyPressed(GLFW_KEY_RIGHT_CONTROL)) {
            position.y -= moveSpeed;
            hasUpdated = true;
        }

        // NOTE: Scroll wheel does NOT affect anything in free mode
        return hasUpdated;
    }

    // ---------- Orbit mode ----------
    private boolean orbitCameraTransform(Window inputWindow) {
        boolean hasUpdated = false;

        // Scroll wheel: change distance (camera moves toward/away from target)
        double scroll = inputWindow.getScrollDelta();
        if (scroll != 0) {
            orbitDistance -= scroll * 0.5f;
            orbitDistance = Math.max(1.0f, Math.min(50.0f, orbitDistance));
            hasUpdated = true;
            // Immediately apply new distance to camera position
            updateCameraFromOrbitTarget();
        }
        System.out.println("Orbit distance: " + orbitDistance);
        System.out.println("Target point: " + orbitTargetPoint);
        System.out.println("Camera position: " + position);

        // Left mouse button: rotate around target
        boolean leftMousePressed = inputWindow.isMouseButtonPressed(Window.MOUSE_BUTTON_LEFT);

        if (leftMousePressed) {
            float[] mouseDelta = inputWindow.getMouseDelta();

            // On the first frame the button is pressed, ignore delta to avoid a jump
            if (!wasLeftMousePressed) {
                wasLeftMousePressed = true;
                // delta will be zero anyway because we just reset mouse position in resetMouse()
            } else {
                // Apply rotation
                orbitYaw += mouseDelta[0] * mouseSensitivity;
                orbitPitch -= mouseDelta[1] * mouseSensitivity; // invert for intuitive up/down
                orbitPitch = Math.max(-89.0f, Math.min(89.0f, orbitPitch));

                if (mouseDelta[0] != 0 || mouseDelta[1] != 0) {
                    hasUpdated = true;
                }
            }
        } else {
            wasLeftMousePressed = false;
        }

        // Recompute camera position if anything changed
        if (hasUpdated) {
            float yawRad = (float) Math.toRadians(orbitYaw);
            float pitchRad = (float) Math.toRadians(orbitPitch);

            // Position on sphere around target
            position.x = orbitTargetPoint.x + orbitDistance * (float) (Math.cos(pitchRad) * Math.sin(yawRad));
            position.y = orbitTargetPoint.y + orbitDistance * (float) Math.sin(pitchRad);
            position.z = orbitTargetPoint.z + orbitDistance * (float) (Math.cos(pitchRad) * Math.cos(yawRad));

            // Update yaw/pitch to always look at the target
            Vector3f dirToTarget = new Vector3f(orbitTargetPoint).sub(position).normalize();
            float newYaw = (float) Math.atan2(dirToTarget.x, dirToTarget.z);
            float newPitch = (float) Math.asin(dirToTarget.y);
            yawPitch.x = (float) Math.toDegrees(newYaw);
            yawPitch.y = (float) Math.toDegrees(newPitch);
            yawPitch.y = Math.max(-89.0f, Math.min(89.0f, yawPitch.y));
        }

        return hasUpdated;
    }

    // ----- Helper methods for orbit mode -----
    private void updateOrbitTargetFromCurrentView() {
        float yawRad = (float) Math.toRadians(yawPitch.x);
        float pitchRad = (float) Math.toRadians(yawPitch.y);
        Vector3f forward = new Vector3f(
                (float) (Math.cos(pitchRad) * Math.sin(yawRad)),
                (float) Math.sin(pitchRad),
                (float) (Math.cos(pitchRad) * Math.cos(yawRad))
        ).normalize();
        orbitTargetPoint = new Vector3f(position).add(forward.mul(orbitDistance, new Vector3f()));
    }

    private void updateCameraFromOrbitTarget() {
        // Keep the direction from camera to target unchanged, only adjust distance
        Vector3f dirToCamera = new Vector3f(position).sub(orbitTargetPoint).normalize();
        position = new Vector3f(orbitTargetPoint).add(dirToCamera.mul(orbitDistance, new Vector3f()));

        // Update yaw/pitch to look at the target
        Vector3f dirToTarget = new Vector3f(orbitTargetPoint).sub(position).normalize();
        float newYaw = (float) Math.atan2(dirToTarget.x, dirToTarget.z);
        float newPitch = (float) Math.asin(dirToTarget.y);
        yawPitch.x = (float) Math.toDegrees(newYaw);
        yawPitch.y = (float) Math.toDegrees(newPitch);
        yawPitch.y = Math.max(-89.0f, Math.min(89.0f, yawPitch.y));
    }

    // ----- Getters for UI -----
    public int getCameraMode() { return cameraMode; }
    public Vector2f getYawPitch() { return yawPitch; }
    public Vector3f getPosition() { return position; }
    public float getOrbitDistance() { return orbitDistance; }
    public Vector3f getOrbitTargetPoint() { return orbitTargetPoint; }
}