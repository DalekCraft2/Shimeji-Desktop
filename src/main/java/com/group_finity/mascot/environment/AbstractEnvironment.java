package com.group_finity.mascot.environment;

import java.awt.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A skeletal implementation of {@link Environment} that provides base functionality
 * for updating the screen size and cursor position.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public abstract class AbstractEnvironment implements Environment {
    /**
     * A {@link Rectangle} representing the union of the bounds of all active displays.
     * This is used to update the bounds of {@link #screen} every tick.
     */
    protected Rectangle screenRect;

    /**
     * A map of {@link Rectangle} objects representing the bounds of all active displays.
     * This is used to update the bounds of the {@link Area} objects in {@link #complexScreen} every tick.
     */
    protected Map<String, Rectangle> screenRects;

    /**
     * A lock used to allow concurrent access to {@link #screenRect} and {@link #screenRects}.
     */
    protected final ReadWriteLock screenRectLock = new ReentrantReadWriteLock();

    /**
     * Whether to enable automatically calling {@link #updateScreenRect()} every 5 seconds to update the
     * {@link #screenRects} and {@link #screenRect} fields. This should be set to {@code false} if a
     * subclass chooses to override the screen updating functionality.
     */
    protected boolean autoUpdateScreenRect = true;

    /**
     * A thread that calls {@link #updateScreenRect()} every 5 seconds,
     * if {@link #autoUpdateScreenRect} is {@code true}.
     */
    private final Thread thread = new Thread(() -> {
        try {
            while (true) {
                if (autoUpdateScreenRect)
                    updateScreenRect();
                Thread.sleep(5000);
            }
        } catch (final InterruptedException ignored) {
        }
    }, "ScreenRectUpdater");

    {
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
    }

    /**
     * The area of the screen.
     * This area is the union of the areas of all active displays.
     */
    private final Area screen = new Area(false);

    /**
     * Represents the areas of all active displays.
     */
    private final ComplexArea complexScreen = new ComplexArea();

    /**
     * The cursor position.
     */
    private final Location cursor = new Location();

    /**
     * Recalculates the bounds of all active displays and the union of those bounds, and assigns the values to
     * {@link #screenRects} and {@link #screenRect} respectively.
     */
    private void updateScreenRect() {
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice[] gs = ge.getScreenDevices();

        Rectangle virtualBounds = new Rectangle();
        Map<String, Rectangle> screenRects = new HashMap<>(gs.length);

        for (final GraphicsDevice gd : gs) {
            Rectangle bounds = gd.getDefaultConfiguration().getBounds();
            screenRects.put(gd.getIDstring(), bounds);
            virtualBounds = virtualBounds.union(bounds);
        }

        screenRectLock.writeLock().lock();
        try {
            this.screenRects = screenRects;
            screenRect = virtualBounds;
        } finally {
            screenRectLock.writeLock().unlock();
        }
    }

    @Override
    public void init() {
        autoUpdateScreenRect = true;

        // Call this here to ensure that the screen rects are initialized before tick() is called
        updateScreenRect();

        if (!thread.isAlive()) {
            thread.start();
        }

        tick();
    }

    @Override
    public void tick() {
        screenRectLock.readLock().lock();
        try {
            screen.set(screenRect);
            complexScreen.set(screenRects);
        } finally {
            screenRectLock.readLock().unlock();
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

    @Override
    public void dispose() {
        if (thread.isAlive()) {
            thread.interrupt();
        }
    }
}
