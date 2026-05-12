package org.pepetrace;

import static org.lwjgl.glfw.GLFW.*;

import org.joml.Vector2f;
import org.joml.Vector3f;

public class Camera implements AutoCloseable {

    private static final float MIN_ORBIT_DISTANCE = 1.0f;
    private static final float MAX_ORBIT_DISTANCE = 50.0f;

    private static final Vector3f DEFAULT_POSITION = new Vector3f(0.0f, 3.0f, -12.0f);
    private static final Vector2f DEFAULT_YAW_PITCH = new Vector2f(0.0f, -15.0f);
    private static final Vector3f DEFAULT_ORBIT_TARGET = new Vector3f(0.0f, 0.0f, 0.0f);
    private static final float DEFAULT_ORBIT_DISTANCE = DEFAULT_POSITION.distance(DEFAULT_ORBIT_TARGET);

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
    private final UBOCamera ubo = new UBOCamera(2);
    private final GlobalState globalState;

    public Camera() {
        this.globalState = GlobalState.getInstance();
        position = new Vector3f(0.0f, 0.0f, -5.0f);
        yawPitch = new Vector2f(0.0f, 0.0f);
        updateOrbitTargetFromCurrentView();
        ubo.updateBuffer(position, yawPitch);
    }

    public Camera(Vector3f position, Vector2f yawPitch) {
        this.globalState = GlobalState.getInstance();
        this.position = new Vector3f(position);
        this.yawPitch = new Vector2f(yawPitch);
        updateOrbitTargetFromCurrentView();
        ubo.updateBuffer(position, yawPitch);
    }

    public void resetToDefault() {
        // Сбрасываем позицию и углы
        position.set(DEFAULT_POSITION);
        yawPitch.set(DEFAULT_YAW_PITCH);
        // Сбрасываем орбитальные параметры
        orbitTargetPoint.set(DEFAULT_ORBIT_TARGET);
        orbitDistance = DEFAULT_ORBIT_DISTANCE;
        // Синхронизируем углы орбиты (чтобы при переключении в орбитальный режим камера смотрела на цель)
        synchronizeOrbitAnglesFromCamera();
    }

    public boolean updateCamera(Window inputWindow, boolean blockInput, boolean blockCameraRotation) {
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

        if (inputWindow.isKeyPressed(GLFW_KEY_F1)) {
            resetToDefault();
            ubo.updateBuffer(position, yawPitch);
            return true;
        }

        boolean shouldUpdateBuffer = false;
        switch (cameraMode) {
            case 0 -> shouldUpdateBuffer = freeCameraTransform(inputWindow, blockInput);
            case 1 -> shouldUpdateBuffer = orbitCameraTransform(inputWindow, blockInput, blockCameraRotation);
            default -> throw new IllegalStateException("Unexpected mode: " + cameraMode);
        }
        if (shouldUpdateBuffer) {
            ubo.updateBuffer(position, yawPitch);
        }
        return shouldUpdateBuffer;
    }

    public void focusOnModel(Vector3f modelCenterWorld) {
        // Вычисляем направление от текущей позиции камеры к центру модели в мировых координатах
        Vector3f directionToCenter = new Vector3f(modelCenterWorld).sub(position).normalize();

        // Преобразуем направление в углы yaw/pitch
        float newYaw = (float) Math.atan2(directionToCenter.x, directionToCenter.z);
        float newPitch = (float) Math.asin(directionToCenter.y);

        // Обновляем углы обзора
        yawPitch.x = (float) Math.toDegrees(newYaw);
        yawPitch.y = (float) Math.toDegrees(newPitch);
        yawPitch.y = Math.max(-89.0f, Math.min(89.0f, yawPitch.y));

        // Если мы в орбитальном режиме, также обновляем цель орбиты и синхронизируем углы
        if (cameraMode == 1) {
            orbitTargetPoint.set(modelCenterWorld);
            synchronizeOrbitAnglesFromCamera();
            orbitDistance = position.distance(orbitTargetPoint);
        }

        // Обновляем UBO с новыми параметрами
        ubo.updateBuffer(position, yawPitch);
    }
    private boolean freeCameraTransform(Window inputWindow, boolean blockInput) {
        boolean hasUpdated = false;
        if (!blockInput) {
            float[] mouseDelta = inputWindow.getMouseDelta();
            yawPitch.x += mouseDelta[0] * mouseSensitivity;
            yawPitch.y += mouseDelta[1] * mouseSensitivity;
            yawPitch.y = Math.max(-89.0f, Math.min(89.0f, yawPitch.y));
            if (mouseDelta[0] != 0 || mouseDelta[1] != 0) hasUpdated = true;
        }

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
            newDistance = Math.max(
                MIN_ORBIT_DISTANCE,
                Math.min(MAX_ORBIT_DISTANCE, newDistance)
            );
            if (Math.abs(newDistance - orbitDistance) > 0.001f) {
                float scale = newDistance / orbitDistance;
                Vector3f directionToTarget = new Vector3f(orbitTargetPoint)
                    .sub(position)
                    .normalize();
                Vector3f newTarget = new Vector3f(position).add(
                    directionToTarget.mul(newDistance, new Vector3f())
                );
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
        if (
            inputWindow.isKeyPressed(GLFW_KEY_LEFT_CONTROL) ||
            inputWindow.isKeyPressed(GLFW_KEY_RIGHT_CONTROL)
        ) {
            position.y -= moveSpeed;
            hasUpdated = true;
        }

        return hasUpdated;
    }

    private boolean orbitCameraTransform(Window inputWindow, boolean blockInput, boolean blockCameraRotation) {
        boolean hasUpdated = false;

        boolean leftMousePressed = inputWindow.isMouseButtonPressed(Window.MOUSE_BUTTON_LEFT);

        // Вращение камеры только если не заблокировано вращение моделей
        if (!blockInput && leftMousePressed && !blockCameraRotation) {
            float[] mouseDelta = inputWindow.getMouseDelta();
            if (!wasLeftMousePressed) {
                wasLeftMousePressed = true;
            } else {
                orbitYaw += mouseDelta[0] * mouseSensitivity;
                orbitPitch -= mouseDelta[1] * mouseSensitivity;
                orbitPitch = Math.max(-89.0f, Math.min(89.0f, orbitPitch));
                if (mouseDelta[0] != 0 || mouseDelta[1] != 0) hasUpdated = true;
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

            updateCameraFromOrbitTarget();
        }

        return hasUpdated;
    }

    public void processScroll(double scroll) {
        if (cameraMode == 1) {
            orbitDistance -= scroll * 0.5f;
            orbitDistance = Math.max(MIN_ORBIT_DISTANCE, Math.min(MAX_ORBIT_DISTANCE, orbitDistance));
            updateCameraFromOrbitTarget();
            ubo.updateBuffer(position, yawPitch);
        }
    }

    private void updateOrbitTargetFromCurrentView() {
        float yawRad = (float) Math.toRadians(yawPitch.x);
        float pitchRad = (float) Math.toRadians(yawPitch.y);
        Vector3f forward = new Vector3f(
            (float) (Math.cos(pitchRad) * Math.sin(yawRad)),
            (float) Math.sin(pitchRad),
            (float) (Math.cos(pitchRad) * Math.cos(yawRad))
        ).normalize();
        orbitTargetPoint = new Vector3f(position).add(
            forward.mul(orbitDistance, new Vector3f())
        );
    }

    private void synchronizeOrbitAnglesFromCamera() {
        Vector3f dirFromTargetToCamera = new Vector3f(position)
            .sub(orbitTargetPoint)
            .normalize();
        orbitYaw = (float) Math.toDegrees(
            Math.atan2(dirFromTargetToCamera.x, dirFromTargetToCamera.z)
        );
        orbitPitch = (float) Math.toDegrees(Math.asin(dirFromTargetToCamera.y));
        orbitPitch = Math.max(-89.0f, Math.min(89.0f, orbitPitch));
    }

    private void updateCameraFromOrbitTarget() {
        float yawRad = (float) Math.toRadians(orbitYaw);
        float pitchRad = (float) Math.toRadians(orbitPitch);

        position.x =
            orbitTargetPoint.x +
            orbitDistance * (float) (Math.cos(pitchRad) * Math.sin(yawRad));
        position.y =
            orbitTargetPoint.y + orbitDistance * (float) Math.sin(pitchRad);
        position.z =
            orbitTargetPoint.z +
            orbitDistance * (float) (Math.cos(pitchRad) * Math.cos(yawRad));

        Vector3f dirToTarget = new Vector3f(orbitTargetPoint)
            .sub(position)
            .normalize();
        float newYaw = (float) Math.atan2(dirToTarget.x, dirToTarget.z);
        float newPitch = (float) Math.asin(dirToTarget.y);
        yawPitch.x = (float) Math.toDegrees(newYaw);
        yawPitch.y = (float) Math.toDegrees(newPitch);
        yawPitch.y = Math.max(-89.0f, Math.min(89.0f, yawPitch.y));
    }

    public int getCameraMode() {
        return cameraMode;
    }

    public Vector2f getYawPitch() {
        return yawPitch;
    }

    public Vector3f getPosition() {
        return position;
    }

    public float getOrbitDistance() {
        return orbitDistance;
    }

    public Vector3f getOrbitTargetPoint() {
        return orbitTargetPoint;
    }

    @Override
    public void close() throws Exception {
        ubo.close();
    }
}
