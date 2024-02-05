package com.group_finity.mascot.environment;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.NativeFactory;

import java.awt.*;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class MascotEnvironment {
    private final Environment impl;

    private final Mascot mascot;

    private Area currentWorkArea;

    public MascotEnvironment(Mascot mascot) {
        this.mascot = mascot;
        impl = NativeFactory.getInstance().getEnvironment();
        impl.init();
    }

    /**
     * Gets the screen containing this environment's {@link Mascot}.
     *
     * @return the screen containing this environment's {@link Mascot}
     */
    public Area getWorkArea() {
        return getWorkArea(false);
    }

    /**
     * Gets the screen containing this environment's {@link Mascot}.
     *
     * @param ignoreSettings whether to force the current work area to be recalculated
     * @return the screen containing this environment's {@link Mascot}
     */
    public Area getWorkArea(Boolean ignoreSettings) {
        if (currentWorkArea != null) {
            if (ignoreSettings || Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Multiscreen", "true"))) {
                // NOTE For Windows multi-monitor support: The Windows work area is smaller than the main screen.
                // If the current screen includes a work area and the mascot is included in the work area, give priority to the work area.
                if (currentWorkArea != impl.getWorkArea() && currentWorkArea.toRectangle().contains(impl.getWorkArea().toRectangle())) {
                    if (impl.getWorkArea().contains(mascot.getAnchor().x, mascot.getAnchor().y)) {
                        currentWorkArea = impl.getWorkArea();
                        return currentWorkArea;
                    }
                }

                // NOTE For Windows multi-monitor support:  The mascot may be included on multiple monitors at the same time,
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

        if (currentWorkArea != null && !Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Multiscreen", "true")) && !currentWorkArea.toRectangle().intersects(activeIE.toRectangle())) {
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

    public Border getCeiling() {
        return getCeiling(false);
    }

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

    public Location getCursor() {
        return impl.getCursor();
    }

    public Border getFloor() {
        return getFloor(false);
    }

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

    public Area getScreen() {
        return impl.getScreen();
    }

    public Border getWall() {
        return getWall(false);
    }

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

    private boolean isScreenTopBottom() {
        return impl.isScreenTopBottom(mascot.getAnchor());
    }

    private boolean isScreenLeftRight() {
        return impl.isScreenLeftRight(mascot.getAnchor());
    }
}
