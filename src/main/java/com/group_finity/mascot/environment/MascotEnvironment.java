package com.group_finity.mascot.environment;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.platform.NativeFactory;

import java.awt.*;

/**
 * Provides mascots with information about the desktop environment, for use in scripts.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 * @see Environment
 */
public class MascotEnvironment {
    /**
     * The delegate {@link Environment} object that is used by this {@code MascotEnvironment}.
     */
    private final Environment impl = NativeFactory.getInstance().getEnvironment();

    /**
     * The {@link Mascot} associated with this {@code MascotEnvironment}.
     */
    private final Mascot mascot;

    /**
     * The work area containing this environment's {@link Mascot}.
     * The work area typically encompasses all of a given screen except for the taskbar.
     *
     *
     * @see #refreshWorkArea()
     * @see #getWorkArea()
     * @see #getWorkArea(boolean)
     */
    private Area currentWorkArea;

    /**
     * Creates a new {@code MascotEnvironment}.
     *
     * @param mascot the {@link Mascot} associated with this {@code MascotEnvironment}
     */
    public MascotEnvironment(Mascot mascot) {
        this.mascot = mascot;
    }

    /**
     * Gets the work area containing this environment's {@link Mascot}.
     * The work area typically encompasses all of a given screen except for the taskbar.
     *
     * @return the work area containing this environment's {@link Mascot}
     * @see Environment#getWorkAreaAt(Point)
     */
    public Area getWorkArea() {
        return getWorkArea(false);
    }

    /**
     * Gets the work area containing this environment's {@link Mascot}.
     * The work area typically encompasses all of a given screen except for the taskbar.
     *
     * @param forceRefresh whether to force the current work area to be recalculated
     * @return the work area containing this environment's {@link Mascot}
     * @see Environment#getWorkAreaAt(Point)
     */
    public Area getWorkArea(boolean forceRefresh) {
        Point anchor = mascot.getAnchor();
        Area implWorkArea = null;
        if (currentWorkArea != null) {
            if (forceRefresh || Main.getInstance().getSettings().multiscreen) {
                // NOTE For Windows multi-monitor support: The Windows work area is smaller than the main screen.
                // If the current screen includes a work area and the mascot is included in the work area, give priority to the work area.
                implWorkArea = impl.getWorkAreaAt(anchor);
                if (currentWorkArea != implWorkArea && currentWorkArea.contains(implWorkArea)) {
                    if (implWorkArea.contains(anchor)) {
                        currentWorkArea = implWorkArea;
                        return currentWorkArea;
                    }
                }

                // NOTE For Windows multi-monitor support: The mascot may be included on multiple monitors at the same time,
                // in which case the current monitor takes priority.
                if (currentWorkArea.contains(anchor)) {
                    return currentWorkArea;
                }
            } else {
                return currentWorkArea;
            }
        }

        if (implWorkArea == null) {
            implWorkArea = impl.getWorkAreaAt(anchor);
        }

        // If the mascot's anchor is within the bounds of implWorkArea,
        // set the current work area to implWorkArea
        if (implWorkArea.contains(anchor)) {
            currentWorkArea = implWorkArea;
            return currentWorkArea;
        }

        // Otherwise, if the mascot's anchor is within the bounds of any of the active displays,
        // set the current work area to that display's bounds
        for (Area area : impl.getScreens()) {
            if (area.contains(anchor)) {
                currentWorkArea = area;
                return currentWorkArea;
            }
        }

        // Otherwise, set the current work area to implWorkArea
        currentWorkArea = implWorkArea;
        return currentWorkArea;
    }

    /**
     * Forces the cached work area to be reevaluated.
     * <p>
     * <b>This method is for internal use only. It should not be used in scripts.</b>
     */
    public void refreshWorkArea() {
        getWorkArea(true);
    }

    /**
     * Gets the area of the screen.
     * This area is the union of the areas of all active displays.
     *
     * @return the screen area
     * @see Environment#getScreen()
     */
    public Area getScreen() {
        return impl.getScreen();
    }

    /**
     * Gets a {@link ComplexArea} representing the areas of all active displays.
     *
     * @return a {@link ComplexArea} representing the areas of all active displays
     * @see Environment#getComplexScreen()
     */
    public ComplexArea getComplexScreen() {
        return impl.getComplexScreen();
    }

    /**
     * Gets the ceiling that this environment's {@link Mascot} is currently on.
     * This can be the ceiling of either the active window or the current work area.
     * <p>
     * Any ceilings that separate two screens will be ignored.
     *
     * @return the ceiling that this environment's {@link Mascot} is currently on, or {@link NotOnBorder#INSTANCE}
     * if the {@link Mascot} is not on any ceiling
     */
    public Border getCeiling() {
        return getCeiling(false);
    }

    /**
     * Gets the ceiling that this environment's {@link Mascot} is currently on.
     * This can be the ceiling of either the active window or the current work area.
     * <p>
     * Any ceiling that separate two screens will be ignored.
     *
     * @param ignoreSeparator whether to ignore any ceilings that separate two screens
     * @return the ceiling that this environment's {@link Mascot} is currently on, or {@link NotOnBorder#INSTANCE}
     * if the {@link Mascot} is not on any ceiling
     */
    public Border getCeiling(boolean ignoreSeparator) {
        Point anchor = mascot.getAnchor();

        Area activeIe = getActiveIE();
        Border activeIeBorder = activeIe.getBottomBorder();
        if (activeIeBorder.isOn(anchor)) {
            return activeIeBorder;
        }

        Area workArea = getWorkArea();
        Border workAreaBorder = workArea.getTopBorder();
        if (workAreaBorder.isOn(anchor)) {
            if (!ignoreSeparator || isScreenTopBottom()) {
                return workAreaBorder;
            }
        }

        return NotOnBorder.INSTANCE;
    }

    /**
     * Gets the floor that this environment's {@link Mascot} is currently on.
     * This can be the floor of either the active window or the current work area.
     * <p>
     * Any floors that separate two screens will be ignored.
     *
     * @return the floor that this environment's {@link Mascot} is currently on, or {@link NotOnBorder#INSTANCE}
     * if the {@link Mascot} is not on any floor
     */
    public Border getFloor() {
        return getFloor(false);
    }

    /**
     * Gets the floor that this environment's {@link Mascot} is currently on.
     * This can be the floor of either the active window or the current work area.
     * <p>
     * Any floors that separate two screens will be ignored.
     *
     * @param ignoreSeparator whether to ignore any floors that separate two screens
     * @return the floor that this environment's {@link Mascot} is currently on, or {@link NotOnBorder#INSTANCE}
     * if the {@link Mascot} is not on any floor
     */
    public Border getFloor(boolean ignoreSeparator) {
        Point anchor = mascot.getAnchor();

        Area activeIe = getActiveIE();
        Border activeIeBorder = activeIe.getTopBorder();
        if (activeIeBorder.isOn(anchor)) {
            return activeIeBorder;
        }

        Area workArea = getWorkArea();
        Border workAreaBorder = workArea.getBottomBorder();
        if (workAreaBorder.isOn(anchor)) {
            if (!ignoreSeparator || isScreenTopBottom()) {
                return workAreaBorder;
            }
        }

        return NotOnBorder.INSTANCE;
    }

    /**
     * Gets the wall that this environment's {@link Mascot} is currently on.
     * This can be the wall of either the active window or the current work area.
     * <p>
     * Any walls that separate two screens will be ignored.
     *
     * @return the wall that this environment's {@link Mascot} is currently on, or {@link NotOnBorder#INSTANCE}
     * if the {@link Mascot} is not on any wall
     */
    public Border getWall() {
        return getWall(false);
    }

    /**
     * Gets the wall that this environment's {@link Mascot} is currently on.
     * This can be the wall of either the active window or the current work area.
     *
     * @param ignoreSeparator whether to ignore any walls that separate two screens
     * @return the wall that this environment's {@link Mascot} is currently on, or {@link NotOnBorder#INSTANCE}
     * if the {@link Mascot} is not on any wall
     */
    public Border getWall(boolean ignoreSeparator) {
        boolean isLookRight = mascot.isLookRight();
        Point anchor = mascot.getAnchor();

        Area activeIe = getActiveIE();
        Border activeIeBorder = isLookRight ? activeIe.getLeftBorder() : activeIe.getRightBorder();
        if (activeIeBorder.isOn(anchor)) {
            return activeIeBorder;
        }

        Area workArea = getWorkArea();
        Border workAreaBorder = isLookRight ? workArea.getRightBorder() : workArea.getLeftBorder();
        if (workAreaBorder.isOn(anchor)) {
            if (!ignoreSeparator || isScreenLeftRight()) {
                return workAreaBorder;
            }
        }

        return NotOnBorder.INSTANCE;
    }

    /**
     * Gets the area of the active window.
     * If there is currently no active window, the return value is implementation-specific.
     *
     * @return the area of the active window
     * @see Environment#getActiveWindow()
     */
    public Area getActiveIE() {
        Area activeIE = impl.getActiveWindow();

        if (currentWorkArea != null && !Main.getInstance().getSettings().multiscreen && !currentWorkArea.intersects(activeIE)) {
            return new Area();
        }

        return activeIE;
    }

    /**
     * Gets the title of the active window.
     * May return {@code null}, depending on the implementation.
     *
     * @return the title of the active window
     * @see Environment#getActiveWindowTitle()
     */
    public String getActiveIETitle() {
        return impl.getActiveWindowTitle();
    }

    /**
     * Gets the ID of the active window. If there is currently no active window, returns 0.
     * <p>
     * <b>This method is for internal use only. It should not be used in scripts.</b>
     *
     * @return the ID of the active window, or 0 if there is currently no active window
     * @see Environment#getActiveWindowId()
     */
    public long getActiveWindowId() {
        return impl.getActiveWindowId();
    }

    /**
     * Repositions the active window so its top-left corner is at the specified point.
     *
     * @param point the point at which the active window's top-left corner should be after it is moved
     * @see Environment#moveActiveWindow(Point)
     */
    public void moveActiveIE(Point point) {
        impl.moveActiveWindow(point);
    }

    /**
     * Repositions the active window so its top-left corner is at the specified location {@code (x, y)}.
     *
     * @param x the x-coordinate at which the active window's left side should be after it is moved
     * @param y the y-coordinate at which the active window's top side should be after it is moved
     * @see Environment#moveActiveWindow(int, int)
     */
    public void moveActiveIE(int x, int y) {
        impl.moveActiveWindow(x, y);
    }

    /**
     * Searches for windows that have been thrown off-screen and repositions them to be on-screen.
     * <p>
     * <b>This method is for internal use only. It should not be used in scripts.</b>
     *
     * @see Environment#restoreWindows()
     */
    public void restoreIE() {
        impl.restoreWindows();
    }

    /**
     * Gets the cursor position.
     *
     * @return a {@link Location} containing the cursor position and velocity
     * @see Environment#getCursor()
     */
    public Location getCursor() {
        return impl.getCursor();
    }

    /**
     * Checks whether the mascot is on the top or bottom border of exactly one screen.
     * Returns {@code false} if the mascot is on multiple top/bottom borders (i.e., the mascot is on the border between
     * two screens).
     *
     * @return whether the mascot is on the top or bottom border of exactly one screen
     */
    private boolean isScreenTopBottom() {
        return isScreenTopBottom(mascot.getAnchor());
    }

    /**
     * Checks whether the specified point lies on the top or bottom border of exactly one screen.
     * Returns {@code false} if the point is on multiple top/bottom borders (i.e., the point is on the border between
     * two screens).
     *
     * @param location the point to check
     * @return whether the point lies on the top or bottom border of exactly one screen
     */
    private boolean isScreenTopBottom(final Point location) {
        int count = 0;

        for (Area area : impl.getScreens()) {
            if (area.getTopBorder().isOn(location)) {
                count++;
            }
            if (area.getBottomBorder().isOn(location)) {
                count++;
            }
        }

        if (count == 0) {
            for (Area area : impl.getComplexWorkArea().getAreas()) {
                if (area.getTopBorder().isOn(location)) {
                    count++;
                }
                if (area.getBottomBorder().isOn(location)) {
                    count++;
                }
            }
        }

        return count == 1;
    }

    /**
     * Checks whether the mascot is on the wall of exactly one screen.
     * Returns {@code false} if the mascot is on multiple walls (i.e., the mascot is on the border between two screens).
     *
     * @return whether the mascot is on the wall of exactly one screen
     */
    private boolean isScreenLeftRight() {
        return isScreenLeftRight(mascot.getAnchor());
    }

    /**
     * Checks whether the specified point lies on the wall of exactly one screen.
     * Returns {@code false} if the point is on multiple walls (i.e., the point is on the border between two screens).
     *
     * @param location the point to check
     * @return whether the point lies on the wall of exactly one screen
     */
    private boolean isScreenLeftRight(final Point location) {
        int count = 0;

        for (Area area : impl.getScreens()) {
            if (area.getLeftBorder().isOn(location)) {
                count++;
            }
            if (area.getRightBorder().isOn(location)) {
                count++;
            }
        }

        if (count == 0) {
            for (Area area : impl.getComplexWorkArea().getAreas()) {
                if (area.getLeftBorder().isOn(location)) {
                    count++;
                }
                if (area.getRightBorder().isOn(location)) {
                    count++;
                }
            }
        }

        return count == 1;
    }
}
