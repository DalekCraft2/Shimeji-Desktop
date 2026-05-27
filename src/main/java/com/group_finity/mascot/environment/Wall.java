package com.group_finity.mascot.environment;

import java.awt.*;

/**
 * Represents a vertical {@link Border}, either on the left or right side of an {@link Area}.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Wall implements Border {

    /**
     * The parent {@link Area} of this {@code Wall}.
     */
    private final Area area;

    /**
     * Whether this {@code Wall} is the right border of its
     * parent {@link Area}, rather than the left border.
     */
    private final boolean right;

    /**
     * Creates a new {@code Wall}.
     *
     * @param area the parent {@link Area} of this {@code Wall}
     * @param right whether this {@code Wall} is a right wall instead of a left wall
     */
    public Wall(final Area area, final boolean right) {
        this.area = area;
        this.right = right;
    }

    /**
     * Gets the parent {@link Area} of this {@code Wall}.
     *
     * @return this {@code Wall} object's parent {@code Area}
     */
    public Area getArea() {
        return area;
    }

    /**
     * Gets whether this {@code Wall} is the right border of its
     * parent {@link Area}, rather than the left border.
     *
     * @return {@code true} if this {@code Wall} is the
     * right border of its parent {@code Area}; {@code false} otherwise
     */
    public boolean isRight() {
        return right;
    }

    /**
     * Gets the x-coordinate of this {@code Wall}.
     *
     * @return this {@code Wall} object's x-coordinate
     */
    public int getX() {
        return right ? area.getRight() : area.getLeft();
    }

    /**
     * Gets the y-coordinate of the topmost point on this {@code Wall}.
     *
     * @return the y-coordinate of the topmost point on this {@code Wall}
     */
    public int getTop() {
        return area.getTop();
    }

    /**
     * Gets the y-coordinate of the bottommost point on this {@code Wall}.
     *
     * @return the y-coordinate of the bottommost point on this {@code Wall}
     */
    public int getBottom() {
        return area.getBottom();
    }

    /**
     * Gets the delta x-coordinate of this {@code Wall}.
     * The returned value represents the distance between
     * the current x-coordinate and the previous x-coordinate.
     *
     * @return the delta x-coordinate of this {@code Wall}
     */
    public int getDX() {
        return right ? area.getDright() : area.getDleft();
    }

    /**
     * Gets the delta y-coordinate of the topmost point on this {@code Wall}.
     * The returned value represents the distance between the y-coordinates of the current
     * and previous topmost points.
     *
     * @return the delta y-coordinate of the topmost point on this {@code Wall}
     */
    public int getDTop() {
        return area.getDtop();
    }

    /**
     * Gets the delta y-coordinate of the bottommost point on this {@code Wall}.
     * The returned value represents the distance between the y-coordinates of the current
     * and previous bottommost points.
     *
     * @return the delta y-coordinate of the topmost point on this {@code Wall}
     */
    public int getDBottom() {
        return area.getDbottom();
    }

    /**
     * Gets the height of this {@code Wall}.
     *
     * @return the height of this {@code Wall}
     */
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

        // Return the location as is if the border hasn't moved
        if (getDTop() == 0 && getDBottom() == 0 && getDX() == 0) {
            return location;
        }

        final int prevTop = getTop() - getDTop();
        final int prevHeight = getBottom() - getDBottom() - prevTop;
        // Return the location as is if the height of the border was previously 0, to avoid division by 0
        if (prevHeight == 0) {
            return location;
        }

        final Point newLocation = new Point(
                location.x + getDX(),
                (location.y - prevTop) * getHeight() / prevHeight + getTop()
        );

        if (Math.abs(newLocation.x - location.x) >= 80 || Math.abs(newLocation.y - location.y) >= 80) {
            return location;
        }

        return newLocation;
    }
}
