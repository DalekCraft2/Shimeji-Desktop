package com.group_finity.mascot.environment;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.platform.NativeFactory;

import java.awt.*;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class MascotEnvironment {
    private final Environment impl;

    private final Mascot mascot;

    private Area currentWorkArea;

    public MascotEnvironment(Mascot mascot) {
        this.mascot = mascot;
        impl = NativeFactory.getInstance().getEnvironment();
    }

    /**
     * Gets the work area containing this environment's {@link Mascot}.
     * The work area typically encompasses all of a given screen except for the taskbar.
     *
     * @return the work area containing this environment's {@link Mascot}
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
     */
    public Area getWorkArea(boolean forceRefresh) {
        if (currentWorkArea != null) {
            if (forceRefresh || Main.getInstance().getSettings().multiscreen) {
                // NOTE For Windows multi-monitor support: The Windows work area is smaller than the main screen.
                // If the current screen includes a work area and the mascot is included in the work area, give priority to the work area.
                if (currentWorkArea != impl.getWorkArea() && currentWorkArea.toRectangle().contains(impl.getWorkArea().toRectangle())) {
                    if (impl.getWorkArea().contains(mascot.getAnchor().x, mascot.getAnchor().y)) {
                        currentWorkArea = impl.getWorkArea();
                        return currentWorkArea;
                    }
                }

                // NOTE For Windows multi-monitor support: The mascot may be included on multiple monitors at the same time,
                // in which case the current monitor takes priority.
                if (currentWorkArea.contains(mascot.getAnchor().x, mascot.getAnchor().y)) {
                    return currentWorkArea;
                }
            } else {
                return currentWorkArea;
            }
        }

        // First check whether the mascot is included in the work area
        if (impl.getWorkArea().contains(mascot.getAnchor().x, mascot.getAnchor().y)) {
            currentWorkArea = impl.getWorkArea();
            return currentWorkArea;
        }

        // Check whether any monitor contains the mascot
        for (Area area : impl.getScreens()) {
            if (area.contains(mascot.getAnchor().x, mascot.getAnchor().y)) {
                currentWorkArea = area;
                return currentWorkArea;
            }
        }

        currentWorkArea = impl.getWorkArea();
        return currentWorkArea;
    }

    public Area getActiveIE() {
        Area activeIE = impl.getActiveIE();

        if (currentWorkArea != null && !Main.getInstance().getSettings().multiscreen && !currentWorkArea.toRectangle().intersects(activeIE.toRectangle())) {
            return new Area();
        }

        return activeIE;
    }

    public String getActiveIETitle() {
        return impl.getActiveIETitle();
    }

    public long getActiveWindowId() {
        return impl.getActiveWindowId();
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
        if (getActiveIE().getBottomBorder().isOn(mascot.getAnchor())) {
            return getActiveIE().getBottomBorder();
        }
        if (getWorkArea().getTopBorder().isOn(mascot.getAnchor())) {
            if (!ignoreSeparator || isScreenTopBottom()) {
                return getWorkArea().getTopBorder();
            }
        }
        return NotOnBorder.INSTANCE;
    }

    public ComplexArea getComplexScreen() {
        return impl.getComplexScreen();
    }

    /**
     * Gets the cursor position.
     *
     * @return a {@link Location} containing the cursor position and velocity
     */
    public Location getCursor() {
        return impl.getCursor();
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
        if (getActiveIE().getTopBorder().isOn(mascot.getAnchor())) {
            return getActiveIE().getTopBorder();
        }
        if (getWorkArea().getBottomBorder().isOn(mascot.getAnchor())) {
            if (!ignoreSeparator || isScreenTopBottom()) {
                return getWorkArea().getBottomBorder();
            }
        }
        return NotOnBorder.INSTANCE;
    }

    /**
     * Gets the area of the screen. This area includes everything from the top left to the bottom right of the display.
     *
     * @return screen area
     */
    public Area getScreen() {
        return impl.getScreen();
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
        if (mascot.isLookRight()) {
            if (getActiveIE().getLeftBorder().isOn(mascot.getAnchor())) {
                return getActiveIE().getLeftBorder();
            }

            if (getWorkArea().getRightBorder().isOn(mascot.getAnchor())) {
                if (!ignoreSeparator || isScreenLeftRight()) {
                    return getWorkArea().getRightBorder();
                }
            }
        } else {
            if (getActiveIE().getRightBorder().isOn(mascot.getAnchor())) {
                return getActiveIE().getRightBorder();
            }

            if (getWorkArea().getLeftBorder().isOn(mascot.getAnchor())) {
                if (!ignoreSeparator || isScreenLeftRight()) {
                    return getWorkArea().getLeftBorder();
                }
            }
        }

        return NotOnBorder.INSTANCE;
    }

    public void moveActiveIE(Point point) {
        impl.moveActiveIE(point);
    }

    public void restoreIE() {
        impl.restoreIE();
    }

    public void refreshWorkArea() {
        getWorkArea(true);
    }

    /**
     * Gets whether the mascot is on the floor or ceiling of exactly one screen.
     * Returns {@code false} if the mascot is on multiple floors/ceilings (i.e., the mascot is on the border between
     * two screens).
     *
     * @return whether the mascot is on the floor or ceiling of exactly one screen
     */
    private boolean isScreenTopBottom() {
        return impl.isScreenTopBottom(mascot.getAnchor());
    }

    /**
     * Gets whether the mascot is on the wall of exactly one screen.
     * Returns {@code false} if the mascot is on multiple walls (i.e., the mascot is on the border between two screens).
     *
     * @return whether the mascot is on the wall of exactly one screen
     */
    private boolean isScreenLeftRight() {
        return impl.isScreenLeftRight(mascot.getAnchor());
    }
}
