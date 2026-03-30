package org.pepetrace;

import org.joml.Vector2f;
import org.joml.Vector3f;
import static org.lwjgl.glfw.GLFW.*;

public class Camera {
    private static final float MIN_ORBIT_DISTANCE = 1.0f;
    private static final float MAX_ORBIT_DISTANCE = 50.0f;

    private Vector3f position;
    private Vector2f yawPitch;
    private int cameraMode = 0;
    private float mouseSensitivity = 0.1f;
    private float moveSpeed = 0.1f;
    private Vector3f orbitTargetPoint = new Vector3f(0, 0, 0);
    private float orbitDistance = 5.0f;
    private float orbitYaw = 0.0f;
    private float orbitPitch = 0.0f;
    private boolean wasLeftMousePressed = false;
    private boolean wasEscapePressed = false;
    private final UBOCamera ubo = new UBOCamera(3);

    public Camera() {
        position = new Vector3f(0.0f, 0.0f, -5.0f);
        yawPitch = new Vector2f(0.0f, 0.0f);
        updateOrbitTargetFromCurrentView();
        ubo.updateBuffer(position, yawPitch);
    }

    public Camera(Vector3f position, Vector2f yawPitch) {
        this.position = new Vector3f(position);
        this.yawPitch = new Vector2f(yawPitch);
        updateOrbitTargetFromCurrentView();
        ubo.updateBuffer(position, yawPitch);
    }

    public boolean updateCamera(Window inputWindow) {
        boolean escapePressed = inputWindow.isKeyPressed(GLFW_KEY_ESCAPE);
        if (escapePressed && !wasEscapePressed) {
            cameraMode = (cameraMode == 0) ? 1 : 0;

            if (cameraMode == 0) {
                inputWindow.setCursorMode(Window.CURSOR_DISABLED);
                inputWindow.resetMouse();
            } else {
                inputWindow.setCursorMode(Window.CURSOR_NORMAL);
                updateOrbitTargetFromCurrentView();
                synchronizeOrbitAnglesFromCamera();
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
        return shouldUpdateBuffer;
    }

    private boolean freeCameraTransform(Window inputWindow) {
        boolean hasUpdated = false;

        float[] mouseDelta = inputWindow.getMouseDelta();
        yawPitch.x += mouseDelta[0] * mouseSensitivity;
        yawPitch.y += mouseDelta[1] * mouseSensitivity;
        yawPitch.y = Math.max(-89.0f, Math.min(89.0f, yawPitch.y));
        if (mouseDelta[0] != 0 || mouseDelta[1] != 0) hasUpdated = true;

        double scroll = inputWindow.getScrollDelta();
        if (scroll != 0) {
            float yawRad = (float) Math.toRadians(yawPitch.x);
            float pitchRad = (float) Math.toRadians(yawPitch.y);
            Vector3f forward = new Vector3f(
                    (float) (Math.cos(pitchRad) * Math.sin(yawRad)),
                    (float) Math.sin(pitchRad),
                    (float) (Math.cos(pitchRad) * Math.cos(yawRad))
            ).normalize();

            float delta = (float) -scroll * moveSpeed * 10.0f;
            float newDistance = orbitDistance + delta;
            newDistance = Math.max(MIN_ORBIT_DISTANCE, Math.min(MAX_ORBIT_DISTANCE, newDistance));
            if (Math.abs(newDistance - orbitDistance) > 0.001f) {
                float scale = newDistance / orbitDistance;
                Vector3f directionToTarget = new Vector3f(orbitTargetPoint).sub(position).normalize();
                Vector3f newTarget = new Vector3f(position).add(directionToTarget.mul(newDistance, new Vector3f()));
                orbitTargetPoint.set(newTarget);
                orbitDistance = newDistance;
                synchronizeOrbitAnglesFromCamera();
                hasUpdated = true;
            }
        }

        float yawRad = (float) Math.toRadians(yawPitch.x);
        float pitchRad = (float) Math.toRadians(yawPitch.y);

        Vector3f forward = new Vector3f(
                (float) (Math.cos(pitchRad) * Math.sin(yawRad)),
                (float) Math.sin(pitchRad),
                (float) (Math.cos(pitchRad) * Math.cos(yawRad))
        ).normalize();

        Vector3f worldUp = new Vector3f(0, 1, 0);
        Vector3f right = new Vector3f();
        forward.cross(worldUp, right);
        if (right.length() < 0.1f) {
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
        if (inputWindow.isKeyPressed(GLFW_KEY_LEFT_CONTROL) ||
                inputWindow.isKeyPressed(GLFW_KEY_RIGHT_CONTROL)) {
            position.y -= moveSpeed;
            hasUpdated = true;
        }

        return hasUpdated;
    }

    private boolean orbitCameraTransform(Window inputWindow) {
        boolean hasUpdated = false;

        double scroll = inputWindow.getScrollDelta();
        if (scroll != 0) {
            orbitDistance -= scroll * 0.5f;
            orbitDistance = Math.max(MIN_ORBIT_DISTANCE, Math.min(MAX_ORBIT_DISTANCE, orbitDistance));
            hasUpdated = true;
            updateCameraFromOrbitTarget();
        }

        boolean leftMousePressed = inputWindow.isMouseButtonPressed(Window.MOUSE_BUTTON_LEFT);

        if (leftMousePressed) {
            float[] mouseDelta = inputWindow.getMouseDelta();

            if (!wasLeftMousePressed) {
                wasLeftMousePressed = true;
            } else {
                orbitYaw += mouseDelta[0] * mouseSensitivity;
                orbitPitch -= mouseDelta[1] * mouseSensitivity;
                orbitPitch = Math.max(-89.0f, Math.min(89.0f, orbitPitch));

                if (mouseDelta[0] != 0 || mouseDelta[1] != 0) {
                    hasUpdated = true;
                }
            }
        } else {
            wasLeftMousePressed = false;
        }

        if (hasUpdated) {
            float yawRad = (float) Math.toRadians(orbitYaw);
            float pitchRad = (float) Math.toRadians(orbitPitch);

            position.x = orbitTargetPoint.x + orbitDistance * (float) (Math.cos(pitchRad) * Math.sin(yawRad));
            position.y = orbitTargetPoint.y + orbitDistance * (float) Math.sin(pitchRad);
            position.z = orbitTargetPoint.z + orbitDistance * (float) (Math.cos(pitchRad) * Math.cos(yawRad));

            Vector3f dirToTarget = new Vector3f(orbitTargetPoint).sub(position).normalize();
            float newYaw = (float) Math.atan2(dirToTarget.x, dirToTarget.z);
            float newPitch = (float) Math.asin(dirToTarget.y);
            yawPitch.x = (float) Math.toDegrees(newYaw);
            yawPitch.y = (float) Math.toDegrees(newPitch);
            yawPitch.y = Math.max(-89.0f, Math.min(89.0f, yawPitch.y));
        }

        return hasUpdated;
    }

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

    private void synchronizeOrbitAnglesFromCamera() {
        Vector3f dirFromTargetToCamera = new Vector3f(position).sub(orbitTargetPoint).normalize();
        orbitYaw = (float) Math.toDegrees(Math.atan2(dirFromTargetToCamera.x, dirFromTargetToCamera.z));
        orbitPitch = (float) Math.toDegrees(Math.asin(dirFromTargetToCamera.y));
        orbitPitch = Math.max(-89.0f, Math.min(89.0f, orbitPitch));
    }

    private void updateCameraFromOrbitTarget() {
        float yawRad = (float) Math.toRadians(orbitYaw);
        float pitchRad = (float) Math.toRadians(orbitPitch);

        position.x = orbitTargetPoint.x + orbitDistance * (float) (Math.cos(pitchRad) * Math.sin(yawRad));
        position.y = orbitTargetPoint.y + orbitDistance * (float) Math.sin(pitchRad);
        position.z = orbitTargetPoint.z + orbitDistance * (float) (Math.cos(pitchRad) * Math.cos(yawRad));

        Vector3f dirToTarget = new Vector3f(orbitTargetPoint).sub(position).normalize();
        float newYaw = (float) Math.atan2(dirToTarget.x, dirToTarget.z);
        float newPitch = (float) Math.asin(dirToTarget.y);
        yawPitch.x = (float) Math.toDegrees(newYaw);
        yawPitch.y = (float) Math.toDegrees(newPitch);
        yawPitch.y = Math.max(-89.0f, Math.min(89.0f, yawPitch.y));
    }

    public int getCameraMode() { return cameraMode; }
    public Vector2f getYawPitch() { return yawPitch; }
    public Vector3f getPosition() { return position; }
    public float getOrbitDistance() { return orbitDistance; }
    public Vector3f getOrbitTargetPoint() { return orbitTargetPoint; }
}