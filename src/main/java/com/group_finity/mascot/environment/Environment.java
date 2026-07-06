package com.group_finity.mascot.environment;

import com.group_finity.mascot.Manager;

import java.awt.*;
import java.util.Collection;

/**
 * Interacts with, and provides information about, the desktop environment.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 * @see MascotEnvironment
 */
public interface Environment {
    /**
     * Initializes this environment. Should be called only once when this environment is created.
     */
    void init();

    /**
     * Advances this environment by one frame. Called every 40 milliseconds by {@link Manager}.
     */
    void tick();

    /**
     * Gets the work area that contains the specified point.
     * The work area typically encompasses all of a given screen except for the taskbar.
     *
     * @param point the specified point
     * @return the work area that contains the specified point
     */
    default Area getWorkAreaAt(Point point) {
        return getWorkAreaAt(point.x, point.y);
    }

    /**
     * Gets the work area that contains the specified location {@code (x, y)}.
     * The work area typically encompasses all of a given screen except for the taskbar.
     *
     * @param x the x-coordinate of the specified location
     * @param y the y-coordinate of the specified location
     * @return the work area that contains the specified location
     */
    Area getWorkAreaAt(int x, int y);

    /**
     * Gets a {@link ComplexArea} representing the areas of all work areas.
     *
     * @return a {@link ComplexArea} representing the areas of all work areas
     */
    ComplexArea getComplexWorkArea();

    /**
     * Gets the area of the screen.
     * This area is the union of the areas of all active displays.
     *
     * @return the screen area
     */
    Area getScreen();

    /**
     * Gets the areas of all active displays.
     *
     * @return the areas of all active displays
     * @see #getComplexScreen()
     */
    Collection<Area> getScreens();

    /**
     * Gets a {@link ComplexArea} representing the areas of all active displays.
     *
     * @return a {@link ComplexArea} representing the areas of all active displays
     * @see #getScreens()
     */
    ComplexArea getComplexScreen();

    /**
     * Gets the area of the active window.
     * If there is currently no active window, the return value is implementation-specific.
     *
     * @return the area of the active window
     */
    Area getActiveWindow();

    /**
     * Gets the title of the active window.
     * May return {@code null}, depending on the implementation.
     *
     * @return the title of the active window
     */
    String getActiveWindowTitle();

    /**
     * Gets the ID of the active window. If there is currently no active window, returns 0.
     *
     * @return the ID of the active window, or 0 if there is currently no active window
     */
    long getActiveWindowId();

    /**
     * Repositions the active window so its top-left corner is at the specified point.
     *
     * @param point the point at which the active window's top-left corner should be after it is moved
     */
    default void moveActiveWindow(Point point) {
        moveActiveWindow(point.x, point.y);
    }

    /**
     * Repositions the active window so its top-left corner is at the specified location {@code (x, y)}.
     *
     * @param x the x-coordinate at which the active window's left side should be after it is moved
     * @param y the y-coordinate at which the active window's top side should be after it is moved
     */
    void moveActiveWindow(int x, int y);

    /**
     * Searches for windows that have been thrown off-screen and repositions them to be on-screen.
     */
    void restoreWindows();

    /**
     * Gets the cursor position.
     *
     * @return a {@link Location} containing the cursor position and velocity
     */
    Location getCursor();

    /**
     * Clears the cached data for which windows are allowed to be interactable.
     */
    void refreshCache();

    /**
     * Releases any native resources held by this environment.
     */
    void dispose();
}
