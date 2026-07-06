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
     * A map of {@link Rectangle} objects representing the bounds of all work areas.
     * This is used to update the bounds of the {@link Area} objects in {@link #complexWorkArea} every tick.
     */
    protected Map<String, Rectangle> workAreaRects;

    /**
     * A lock used to allow concurrent access to {@link #screenRect}, {@link #screenRects}, and {@link #workAreaRects}.
     */
    protected final ReadWriteLock screenRectLock = new ReentrantReadWriteLock();

    /**
     * Whether to enable automatically calling {@link #updateScreenRect()} every 5 seconds to update the
     * {@link #screenRect}, {@link #screenRects}, and {@link #workAreaRects} fields. This should be set to
     * {@code false} if a subclass chooses to override the screen updating functionality.
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
     * Represents the work areas of all active displays.
     */
    private final ComplexArea complexWorkArea = new ComplexArea();

    /**
     * The cursor position.
     */
    private final Location cursor = new Location();

    /**
     * Fallback area that is used when {@link #getWorkAreaAt(Point)} fails to find a work area that contains
     * the specified point.
     */
    protected final Area invisibleScreen = new Area() {
        @Override
        public boolean isVisible() {
            return false;
        }
    };

    /**
     * Recalculates the bounds of all active displays and the union of those bounds, and assigns the values to
     * {@link #screenRects} and {@link #screenRect} respectively.
     */
    private void updateScreenRect() {
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice[] gs = ge.getScreenDevices();

        Rectangle unionBounds = new Rectangle();
        Map<String, Rectangle> screenRects = new HashMap<>(gs.length);
        Map<String, Rectangle> workAreaRects = new HashMap<>(gs.length);

        Toolkit toolkit = Toolkit.getDefaultToolkit();

        for (final GraphicsDevice gd : gs) {
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            String id = gd.getIDstring();

            Rectangle bounds = gc.getBounds();
            unionBounds = unionBounds.union(bounds);
            screenRects.put(id, bounds);

            // BUG: On X11, the return value of the below method call does not return the correct insets when
            // there are multiple active displays. I suspect that the implementation of getScreenInsets() on X11
            // uses the _NET_WORKAREA atom, which doesn't return the correct work area bounds when there are
            // multiple active displays.
            Insets insets = toolkit.getScreenInsets(gc);

            Rectangle workAreaRect;
            if (insets.left == 0 && insets.top == 0 && insets.right == 0 && insets.bottom == 0) {
                // Reuse the same Rectangle instance if the insets are all 0
                workAreaRect = bounds;
            } else {
                workAreaRect = new Rectangle(bounds);
                workAreaRect.x += insets.left;
                workAreaRect.y += insets.top;
                workAreaRect.width -= insets.left + insets.right;
                workAreaRect.height -= insets.top + insets.bottom;
            }
            workAreaRects.put(id, workAreaRect);
        }

        screenRectLock.writeLock().lock();
        try {
            screenRect = unionBounds;
            this.screenRects = screenRects;
            this.workAreaRects = workAreaRects;
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
            getComplexScreen().set(screenRects);
            getComplexWorkArea().set(workAreaRects);
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
    public Area getWorkAreaAt(int x, int y) {
        Collection<Area> workAreas = getComplexWorkArea().getAreas();
        if (!workAreas.isEmpty()) {
            return workAreas.stream().filter(workArea -> workArea.contains(x, y)).findFirst().orElse(invisibleScreen);
        }

        return invisibleScreen;
    }

    @Override
    public ComplexArea getComplexWorkArea() {
        return complexWorkArea;
    }

    @Override
    public Area getScreen() {
        return screen;
    }

    @Override
    public Collection<Area> getScreens() {
        return getComplexScreen().getAreas();
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
