package com.group_finity.mascot.environment;

import java.awt.*;

/**
 * Represents a horizontal {@link Border}, either on the top or bottom of an {@link Area}.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class FloorCeiling implements Border {

    /**
     * The parent {@link Area} of this {@code FloorCeiling}.
     */
    private final Area area;

    /**
     * Whether this {@code FloorCeiling} is the bottom border of its
     * parent {@link Area}, rather than the top border.
     * <p>
     * Note that this does not always equal whether this {@code FloorCeiling} is a floor.
     * Some usages of {@code Area} objects may treat the bottom border as a floor,
     * but others may treat it as a ceiling. The same is true for the top border.
     */
    private final boolean bottom;

    /**
     * Creates a new {@code FloorCeiling}.
     *
     * @param area the parent {@link Area} of this {@code FloorCeiling}
     * @param bottom whether this {@code FloorCeiling} is the bottom border of its
     * parent {@link Area}, rather than the top border
     */
    public FloorCeiling(final Area area, final boolean bottom) {
        this.area = area;
        this.bottom = bottom;
    }

    /**
     * Gets the parent {@link Area} of this {@code FloorCeiling}.
     *
     * @return this {@code FloorCeiling} object's parent {@code Area}
     */
    public Area getArea() {
        return area;
    }

    /**
     * Gets whether this {@code FloorCeiling} is the bottom border of its
     * parent {@link Area}, rather than the top border.
     * <p>
     * Note that the returned value does not always equal whether this {@code FloorCeiling} is a floor.
     * Some usages of {@code Area} objects may treat the bottom border as a floor,
     * but others may treat it as a ceiling. The same is true for the top border.
     *
     * @return {@code true} if this {@code FloorCeiling} is the
     * bottom border of its parent {@code Area}; {@code false} otherwise
     */
    public boolean isBottom() {
        return bottom;
    }

    /**
     * Gets the y-coordinate of this {@code FloorCeiling}.
     *
     * @return this {@code FloorCeiling} object's y-coordinate
     */
    public int getY() {
        return bottom ? area.getBottom() : area.getTop();
    }

    /**
     * Gets the x-coordinate of the leftmost point on this {@code FloorCeiling}.
     *
     * @return the x-coordinate of the leftmost point on this {@code FloorCeiling}
     */
    public int getLeft() {
        return area.getLeft();
    }

    /**
     * Gets the x-coordinate of the rightmost point on this {@code FloorCeiling}.
     *
     * @return the x-coordinate of the rightmost point on this {@code FloorCeiling}
     */
    public int getRight() {
        return area.getRight();
    }

    /**
     * Gets the delta y-coordinate of this {@code FloorCeiling}.
     * The returned value represents the distance between
     * the current y-coordinate and the previous y-coordinate.
     *
     * @return the delta y-coordinate of this {@code FloorCeiling}
     */
    public int getDY() {
        return bottom ? area.getDbottom() : area.getDtop();
    }

    /**
     * Gets the delta x-coordinate of the leftmost point on this {@code FloorCeiling}.
     * The returned value represents the distance between the x-coordinates of the current
     * and previous leftmost points.
     *
     * @return the delta x-coordinate of the leftmost point on this {@code FloorCeiling}
     */
    public int getDLeft() {
        return area.getDleft();
    }

    /**
     * Gets the delta x-coordinate of the rightmost point on this {@code FloorCeiling}.
     * The returned value represents the distance between the x-coordinates of the current
     * and previous rightmost points.
     *
     * @return the delta x-coordinate of the rightmost point on this {@code FloorCeiling}
     */
    public int getDRight() {
        return area.getDright();
    }

    /**
     * Gets the width of this {@code FloorCeiling}.
     *
     * @return the width of this {@code FloorCeiling}
     */
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
