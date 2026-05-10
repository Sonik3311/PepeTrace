package org.pepetrace.Drawers;

import org.pepetrace.Window;

public class RTDrawer extends AbstractDrawer {

    public RTDrawer(Window window) {
        super(window, "rtlayout.ini");
    }

    @Override
    public void renderFrame() {
        frameId++;
    }
}
