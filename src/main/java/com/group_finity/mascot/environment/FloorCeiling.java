package com.group_finity.mascot.environment;

import java.awt.*;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class FloorCeiling implements Border {

    private final Area area;

    private final boolean bottom;

    public FloorCeiling(final Area area, final boolean bottom) {
        this.area = area;
        this.bottom = bottom;
    }

    public Area getArea() {
        return area;
    }

    public boolean isBottom() {
        return bottom;
    }

    public int getY() {
        return bottom ? area.getBottom() : area.getTop();
    }

    public int getLeft() {
        return area.getLeft();
    }

    public int getRight() {
        return area.getRight();
    }

    public int getDY() {
        return bottom ? area.getDbottom() : area.getDtop();
    }

    public int getDLeft() {
        return area.getDleft();
    }

    public int getDRight() {
        return area.getDright();
    }

    public int getWidth() {
        return area.getWidth();
    }

    @Override
    public boolean isOn(final Point location) {
        return area.isVisible() && getY() == location.y && getLeft() <= location.x
                && location.x <= getRight();
    }

    @Override
    public Point move(final Point location) {
        if (!area.isVisible()) {
            return location;
        }

        final int newWidth = getRight() - getDRight() - (getLeft() - getDLeft());
        if (newWidth == 0) {
            return location;
        }

        final Point newLocation = new Point((location.x - (getLeft() - getDLeft())) * getWidth() / newWidth
                + getLeft(), location.y + getDY());

        if (Math.abs(newLocation.x - location.x) >= 80 || newLocation.y - location.y > 20
                || newLocation.y - location.y < -80) {
            return location;
        }

        return newLocation;
    }
}
