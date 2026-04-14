package org.pepetrace.GUI;

import org.pepetrace.GlobalState;

public interface GuiWindow {
    GlobalState programState = GlobalState.getInstance();
    void render(int windowFlags);
}
