package org.pepetrace.Util;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL33.glGetQueryObjectui64v;

public class GPUTimeQuerier {
    final private int[] queries = {0, 0};
    private boolean isTicking = false;

    public GPUTimeQuerier() {
        glGenQueries(queries);
    }

    public void startTimer() {
        if (isTicking) {
            System.err.println("Received start command without finishing previous, ignoring");
            return;
        }
        isTicking = true;
        glQueryCounter(queries[0], GL_TIMESTAMP);
    }

    public long stopTimer() {
        if (!isTicking) {
            System.err.println("Received stop command without starting, ignoring");
            return -1;
        }

        glQueryCounter(queries[1], GL_TIMESTAMP);

        // GPU закончила измерять время?
        int[] isStopped = {0};
        while(isStopped[0] == GL_FALSE) {
            glGetQueryObjectiv(queries[1], GL_QUERY_RESULT_AVAILABLE, isStopped);
        }

        isTicking = false;

        long[] startTime = {0};
        long[] endTime = {0};
        glGetQueryObjectui64v(queries[0], GL_QUERY_RESULT, startTime);
        glGetQueryObjectui64v(queries[1], GL_QUERY_RESULT, endTime);

        return endTime[0] - startTime[0];
    }

}
