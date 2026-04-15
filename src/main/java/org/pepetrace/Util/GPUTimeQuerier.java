package org.pepetrace.Util;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL33.*;

public class GPUTimeQuerier implements AutoCloseable {

    private final int[] queries = { 0, 0 };
    private boolean isTicking = false;
    private boolean waitingForResult = false;

    public GPUTimeQuerier() {
        glGenQueries(queries);
    }

    /**
     * Запускает таймер. Не может быть вызван повторно без остановки.
     */
    public void startTimer() {
        if (isTicking) {
            //System.err.println("Received start command without finishing previous, ignoring");
            return;
        }
        if (waitingForResult) {
            //System.err.println("Received start command without waiting for previous result, ignoring");
            return;
        }
        isTicking = true;
        waitingForResult = false;
        glQueryCounter(queries[0], GL_TIMESTAMP);
    }

    /**
     * Отправляет запрос на остановку таймера. Метод возвращается мгновенно,
     * не дожидаясь результата GPU.
     *
     * @return true, если запрос успешно отправлен; false, если таймер не был запущен
     */
    public boolean stopTimerAsync() {
        if (!isTicking) {
            //System.err.println("Received stop command without starting, ignoring");
            return false;
        }

        glQueryCounter(queries[1], GL_TIMESTAMP);
        waitingForResult = true;
        isTicking = false; // Таймер больше не активен, но результат ещё не готов
        return true;
    }

    /**
     * Проверяет, готов ли результат измерения после вызова {@link #stopTimerAsync()}.
     * Этот метод можно вызывать в каждом кадре основного цикла без блокировки.
     *
     * @return true, если результат доступен и может быть получен через {@link #getResult()}
     */
    public boolean isResultReady() {
        if (!waitingForResult) return false;

        int[] available = { 0 };
        glGetQueryObjectiv(queries[1], GL_QUERY_RESULT_AVAILABLE, available);
        return available[0] != GL_FALSE;
    }

    /**
     * Возвращает измеренное время в наносекундах.
     * <p>
     * <b>Важно:</b> должен вызываться только после того, как {@link #isResultReady()} вернул true.
     *
     * @return разница между конечной и начальной временной меткой в наносекундах,
     *         или -1, если результат ещё не готов.
     */
    public long getResult() {
        if (!waitingForResult) {
            //System.err.println("No result is being waited for");
            return -1;
        }
        if (!isResultReady()) {
            //System.err.println("Result not available yet");
            return -1;
        }

        long[] startTime = { 0 };
        long[] endTime   = { 0 };
        glGetQueryObjectui64v(queries[0], GL_QUERY_RESULT, startTime);
        glGetQueryObjectui64v(queries[1], GL_QUERY_RESULT, endTime);

        waitingForResult = false;
        return endTime[0] - startTime[0];
    }

    /**
     * Устаревший метод, оставлен для обратной совместимости.
     * Вместо него рекомендуется использовать пару {@link #stopTimerAsync()} +
     * {@link #isResultReady()} + {@link #getResult()} в основном цикле.
     */
    @Deprecated
    public long stopTimer() {
        if (!stopTimerAsync()) return -1;

        // Блокирующее ожидание (старое поведение) – не рекомендуется!
        while (!isResultReady()) {
            Thread.yield(); // или кратковременный sleep
        }
        return getResult();
    }

    // Опционально: метод для освобождения ресурсов
    public void close() {
        glDeleteQueries(queries);
    }
}