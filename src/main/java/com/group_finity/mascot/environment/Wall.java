package com.group_finity.mascot.environment;

import java.awt.*;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Wall implements Border {

    private final Area area;

    private final boolean right;

    public Wall(final Area area, final boolean right) {
        this.area = area;
        this.right = right;
    }

    public Area getArea() {
        return area;
    }

    public boolean isRight() {
        return right;
    }

    public int getX() {
        return right ? area.getRight() : area.getLeft();
    }

    public int getTop() {
        return area.getTop();
    }

    public int getBottom() {
        return area.getBottom();
    }

    public int getDX() {
        return right ? area.getDright() : area.getDleft();
    }

    public int getDTop() {
        return area.getDtop();
    }

    public int getDBottom() {
        return area.getDbottom();
    }

    public int getHeight() {
        return area.getHeight();
    }

    @Override
    public boolean isOn(final Point location) {
        return area.isVisible() && getX() == location.x && getTop() <= location.y
                && location.y <= getBottom();
    }

    @Override
    public Point move(final Point location) {
        if (!area.isVisible()) {
            return location;
        }

        final int newHeight = getBottom() - getDBottom() - (getTop() - getDTop());
        if (newHeight == 0) {
            return location;
        }

        final Point newLocation = new Point(location.x + getDX(), (location.y - (getTop() - getDTop()))
                * getHeight() / newHeight + getTop());

        if (Math.abs(newLocation.x - location.x) >= 80 || Math.abs(newLocation.y - location.y) >= 80) {
            return location;
        }

        return newLocation;
    }
}
