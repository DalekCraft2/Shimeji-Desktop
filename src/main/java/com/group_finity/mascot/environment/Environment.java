package com.group_finity.mascot.environment;

import com.group_finity.mascot.Manager;

import java.awt.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public abstract class Environment {
    protected static Rectangle screenRect = new Rectangle(new Point(0, 0), Toolkit.getDefaultToolkit().getScreenSize());

    protected static Map<String, Rectangle> screenRects = new HashMap<>();

    protected static final Object screenRectLock = new Object();

    protected static boolean autoUpdateScreenRect = true;

    private static final Thread thread = new Thread(() -> {
        try {
            while (true) {
                if (autoUpdateScreenRect)
                    updateScreenRect();
                Thread.sleep(5000);
            }
        } catch (final InterruptedException ignored) {
        }
    }, "ScreenRectUpdater");

    private final Area screen = new Area();

    private final ComplexArea complexScreen = new ComplexArea();

    private final Location cursor = new Location();

    private static void updateScreenRect() {
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice[] gs = ge.getScreenDevices();

        Rectangle virtualBounds = new Rectangle();
        Map<String, Rectangle> screenRects = new HashMap<>(gs.length);

        for (final GraphicsDevice gd : gs) {
            Rectangle bounds = gd.getDefaultConfiguration().getBounds();
            screenRects.put(gd.getIDstring(), bounds);
            virtualBounds = virtualBounds.union(bounds);
        }

        synchronized (screenRectLock) {
            Environment.screenRects = screenRects;
            screenRect = virtualBounds;
        }
    }

    /**
     * Gets the cursor position.
     *
     * @return cursor position
     */
    private static Point getCursorPos() {
        PointerInfo info = MouseInfo.getPointerInfo();
        return info != null ? info.getLocation() : new Point(0, 0);
    }

    /**
     * Called when this environment is created.
     */
    public void init() {
        autoUpdateScreenRect = true;

        if (!thread.isAlive()) {
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }

        tick();
    }

    /**
     * Advances this environment by one frame. Called every 40 milliseconds by {@link Manager}.
     */
    public void tick() {
        synchronized (screenRectLock) {
            screen.set(screenRect);
            complexScreen.set(screenRects);
        }
        cursor.set(getCursorPos());
    }

    protected abstract Area getWorkArea();

    /**
     * Gets the area of the screen. This area includes everything from the top left to the bottom right of the display.
     *
     * @return screen area
     */
    public Area getScreen() {
        return screen;
    }

    public Collection<Area> getScreens() {
        return complexScreen.getAreas();
    }

    public ComplexArea getComplexScreen() {
        return complexScreen;
    }

    public boolean isScreenTopBottom(final Point location) {
        int count = 0;

        for (Area area : getScreens()) {
            if (area.getTopBorder().isOn(location)) {
                count++;
            }
            if (area.getBottomBorder().isOn(location)) {
                count++;
            }
        }

        if (count == 0) {
            if (getWorkArea().getTopBorder().isOn(location)) {
                return true;
            }
            if (getWorkArea().getBottomBorder().isOn(location)) {
                return true;
            }
        }

        return count == 1;
    }

    public boolean isScreenLeftRight(final Point location) {
        int count = 0;

        for (Area area : getScreens()) {
            if (area.getLeftBorder().isOn(location)) {
                count++;
            }
            if (area.getRightBorder().isOn(location)) {
                count++;
            }
        }

        if (count == 0) {
            if (getWorkArea().getLeftBorder().isOn(location)) {
                return true;
            }
            if (getWorkArea().getRightBorder().isOn(location)) {
                return true;
            }
        }

        return count == 1;
    }

    public abstract Area getActiveIE();

    public abstract String getActiveIETitle();

    public abstract long getActiveWindowId();

    public abstract void moveActiveIE(final Point point);

    public abstract void restoreIE();

    /**
     * Gets the cursor position as of the start of the last tick.
     *
     * @return a {@link Location} containing the cursor position and velocity
     */
    public Location getCursor() {
        return cursor;
    }

    public abstract void refreshCache();

    public abstract void dispose();
}
