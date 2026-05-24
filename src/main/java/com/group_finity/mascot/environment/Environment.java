package com.group_finity.mascot.environment;

import com.group_finity.mascot.Manager;

import java.awt.*;
import java.util.Collection;

/**
 * Interacts with, and provides information about, the desktop environment.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
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
     * Gets the work area.
     * The work area typically encompasses all of a given screen except for the taskbar.
     *
     * @return the work area
     */
    Area getWorkArea();

    /**
     * Gets the area of the screen.
     * This area is the combined areas of all active displays.
     *
     * @return the screen area
     */
    Area getScreen();

    /**
     * Gets the areas of all active displays.
     *
     * @return the areas of all active displays
     */
    Collection<Area> getScreens();

    /**
     * Gets a {@link ComplexArea} representing the areas of all active displays.
     *
     * @return a {@link ComplexArea} representing the areas of all active displays
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
     * Repositions the active window so its top-left corner is at the given point.
     *
     * @param point the point at which the active window's top-left corner should be after it is moved
     */
    void moveActiveWindow(final Point point);

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
