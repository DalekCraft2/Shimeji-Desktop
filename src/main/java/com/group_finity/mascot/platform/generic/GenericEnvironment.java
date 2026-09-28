package com.group_finity.mascot.platform.generic;

import com.group_finity.mascot.environment.AbstractEnvironment;
import com.group_finity.mascot.environment.Area;

/**
 * A cross-platform implementation of {@link AbstractEnvironment} that provides access to basic information about
 * the native environment.
 * <p>
 * Because this implementation is not specialized for any specific operating system, it only contains the functionality
 * that is already present in {@code AbstractEnvironment}.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
class GenericEnvironment extends AbstractEnvironment {

    /**
     * The area of the active window.
     * Because the generic environment does not support interaction with windows,
     * this area's position is always {@code (0, 0)}, and its dimensions are always {@code (0, 0)}.
     * The area's visibility is set to {@code false} every tick.
     *
     * @see #getActiveWindow()
     */
    private final Area activeWindow = new Area();

    @Override
    public void tick() {
        super.tick();
        activeWindow.setVisible(false);
    }

    @Override
    public Area getActiveWindow() {
        return activeWindow;
    }

    @Override
    public String getActiveWindowTitle() {
        return null;
    }

    @Override
    public long getActiveWindowId() {
        return 0;
    }

    @Override
    public void moveActiveWindow(final int x, final int y) {
    }

    @Override
    public void restoreWindows() {
    }

    @Override
    public void refreshCache() {
        // I feel so refreshed
    }
}
