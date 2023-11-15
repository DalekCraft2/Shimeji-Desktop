package com.group_finity.mascot.environment;

import java.awt.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
public abstract class Environment {
    protected abstract Area getWorkArea();

    public abstract Area getActiveIE();

    public abstract String getActiveIETitle();

    public abstract void moveActiveIE(final Point point);

    public abstract void restoreIE();

    public abstract void refreshCache();

    public abstract void dispose();

    protected static Rectangle screenRect = new Rectangle(new Point(0, 0), Toolkit.getDefaultToolkit().getScreenSize());

    protected static Map<String, Rectangle> screenRects = new HashMap<>();

    private static final Thread thread = new Thread(() -> {
        try {
            while (true) {
                updateScreenRect();
                Thread.sleep(5000);
            }
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    });

    public ComplexArea complexScreen = new ComplexArea();

    public Area screen = new Area();

    public Location cursor = new Location();

    private static void updateScreenRect() {
        Rectangle virtualBounds = new Rectangle();

        Map<String, Rectangle> screenRects = new HashMap<>();

        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice[] gs = ge.getScreenDevices();

        for (final GraphicsDevice gd : gs) {
            screenRects.put(gd.getIDstring(), gd.getDefaultConfiguration().getBounds());
            virtualBounds = virtualBounds.union(gd.getDefaultConfiguration().getBounds());
        }

        Environment.screenRects = screenRects;

        screenRect = virtualBounds;
    }

    protected static Rectangle getScreenRect() {
        return screenRect;
    }

    private static Point getCursorPos() {
        PointerInfo info = MouseInfo.getPointerInfo();
        return info != null ? info.getLocation() : new Point(0, 0);
    }

    public void init() {
        if (!thread.isAlive()) {
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }

        tick();
    }

    public void tick() {
        screen.set(getScreenRect());
        complexScreen.set(screenRects);
        cursor.set(getCursorPos());
    }

    public Area getScreen() {
        return screen;
    }

    public Collection<Area> getScreens() {
        return complexScreen.getAreas();
    }

    public ComplexArea getComplexScreen() {
        return complexScreen;
    }

    public Location getCursor() {
        return cursor;
    }

    public boolean isScreenTopBottom(final Point location) {
        int count = 0;

        for (Area area : getScreens()) {
            if (area.getTopBorder().isOn(location)) {
                ++count;
            }
            if (area.getBottomBorder().isOn(location)) {
                ++count;
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
                ++count;
            }
            if (area.getRightBorder().isOn(location)) {
                ++count;
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
}
