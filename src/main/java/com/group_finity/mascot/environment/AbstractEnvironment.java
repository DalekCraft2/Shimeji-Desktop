package com.group_finity.mascot.environment;

import java.awt.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public abstract class AbstractEnvironment implements Environment {
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

    private final Area screen = new Area(false);

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
            AbstractEnvironment.screenRects = screenRects;
            screenRect = virtualBounds;
        }
    }

    @Override
    public void init() {
        autoUpdateScreenRect = true;

        if (!thread.isAlive()) {
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }

        tick();
    }

    @Override
    public void tick() {
        synchronized (screenRectLock) {
            screen.set(screenRect);
            complexScreen.set(screenRects);
        }
        PointerInfo info = MouseInfo.getPointerInfo();
        if (info != null) {
            cursor.set(info.getLocation());
        } else {
            cursor.set(0, 0);
        }
    }

    @Override
    public Area getScreen() {
        return screen;
    }

    @Override
    public Collection<Area> getScreens() {
        return complexScreen.getAreas();
    }

    @Override
    public ComplexArea getComplexScreen() {
        return complexScreen;
    }

    @Override
    public Location getCursor() {
        return cursor;
    }
}
