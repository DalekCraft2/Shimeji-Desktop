package com.group_finity.mascot.environment;

import java.awt.*;

/**
 * Represents a point in space, and the distance it has traveled from its previous position.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Location {

    /**
     * The x-coordinate of this {@code Location}.
     *
     * @see #getX()
     * @see #setX(int)
     */
    private int x;

    /**
     * The y-coordinate of this {@code Location}.
     *
     * @see #getY()
     * @see #setY(int)
     */
    private int y;

    /**
     * The delta x-coordinate of this {@code Location}.
     * Represents the distance between the current x-coordinate and the previous x-coordinate.
     *
     * @see #getDx()
     * @see #setDx(int)
     */
    private int dx;

    /**
     * The delta y-coordinate of this {@code Location}.
     * Represents the distance between the current y-coordinate and the previous y-coordinate.
     *
     * @see #getDy()
     * @see #setDy(int)
     */
    private int dy;

    /**
     * Gets the x-coordinate of this {@code Location}.
     *
     * @return the x-coordinate of this {@code Location}
     * @see #setX(int)
     */
    public int getX() {
        return x;
    }

    /**
     * Sets the x-coordinate of this {@code Location}.
     *
     * @param x the new x-coordinate
     * @see #getX()
     */
    public void setX(final int x) {
        this.x = x;
    }

    /**
     * Gets the y-coordinate of this {@code Location}.
     *
     * @return the y-coordinate of this {@code Location}
     * @see #setY(int)
     */
    public int getY() {
        return y;
    }

    /**
     * Sets the y-coordinate of this {@code Location}.
     *
     * @param y the new y-coordinate
     * @see #getY()
     */
    public void setY(final int y) {
        this.y = y;
    }

    /**
     * Gets the delta x-coordinate of this {@code Location}.
     * The returned value represents the distance between
     * the current x-coordinate and the previous x-coordinate.
     *
     * @return the delta x-coordinate of this {@code Location}
     * @see #setDx(int)
     */
    public int getDx() {
        return dx;
    }

    /**
     * Sets the delta x-coordinate of this {@code Location}.
     *
     * @param dx the new delta x-coordinate
     * @see #getDx()
     */
    public void setDx(final int dx) {
        this.dx = dx;
    }

    /**
     * Gets the delta y-coordinate of this {@code Location}.
     * The returned value represents the distance between
     * the current y-coordinate and the previous y-coordinate.
     *
     * @return the delta y-coordinate of this {@code Location}
     * @see #setDy(int)
     */
    public int getDy() {
        return dy;
    }

    /**
     * Sets the delta y-coordinate of this {@code Location}.
     *
     * @param dy the new delta y-coordinate
     * @see #getDy()
     */
    public void setDy(final int dy) {
        this.dy = dy;
    }

    /**
     * Sets the coordinates of this {@code Location} to match the specified {@link Point}.
     * This will also update the delta values of this {@code Location}.
     *
     * @param p the specified {@code Point}
     */
    public void set(final Point p) {
        set(p.x, p.y);
    }

    /**
     * Sets the coordinates of this {@code Location} to the specified {@code x} and {@code y} values.
     * This will also update the delta values of this {@code Location}.
     *
     * @param x the new x-coordinate
     * @param y the new y-coordinate
     */
    public void set(final int x, final int y) {
        /*
        Rather than calculate the delta values properly, we calculate the average between the stored delta values
        (dx and dy) and the actual delta values ((x - this.x) and (y - this.y)) so they don't return to 0 the moment
        the Location stops moving. This allows the dx and dy values to be more representative of the momentum of the
        cursor.

        This is useful for mascot scripts that use the delta values of the cursor to set their velocity when being
        thrown. If we did not do this and the cursor was stationary for just two ticks, the mascots would have a chance
        of having 0 velocity when the cursor releases them, even if the cursor had been moving before then.
        */
        dx = (dx + x - this.x) / 2;
        dy = (dy + y - this.y) / 2;

        this.x = x;
        this.y = y;
    }
}
