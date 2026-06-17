package com.group_finity.mascot.environment;

import java.awt.*;

/**
 * Represents a region of space with which {@link com.group_finity.mascot.Mascot Mascots} can interact.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Area {

    /**
     * Whether this {@code Area} should calculate the delta values of {@link #left}, {@link #top}, {@link #right}, and
     * {@link #bottom} whenever {@link #set} is called. If {@code false}, the getters for the delta values will always
     * return 0.
     */
    private final boolean calcDeltas;

    /**
     * Whether this {@code Area} is visible.
     * Generally, if this is {@code false}, {@link com.group_finity.mascot.Mascot Mascot} objects
     * should not be able to interact with this {@code Area}.
     *
     * @see #isVisible()
     * @see #setVisible(boolean)
     */
    private boolean visible = true;

    /**
     * The x-coordinate of the left border of this {@code Area}.
     *
     * @see #getLeft()
     * @see #setLeft(int)
     */
    private int left;

    /**
     * The y-coordinate of the top border of this {@code Area}.
     *
     * @see #getTop()
     * @see #setTop(int)
     */
    private int top;

    /**
     * The x-coordinate of the right border of this {@code Area}.
     *
     * @see #getRight()
     * @see #setRight(int)
     */
    private int right;

    /**
     * The y-coordinate of the bottom border of this {@code Area}.
     *
     * @see #getBottom()
     * @see #setBottom(int)
     */
    private int bottom;

    /**
     * The delta x-coordinate of the left border of this {@code Area}.
     * Represents the distance between the current x-coordinate and the previous x-coordinate.
     *
     * @see #getDleft()
     * @see #setDleft(int)
     */
    private int dleft;

    /**
     * The delta y-coordinate of the top border of this {@code Area}.
     * Represents the distance between the current y-coordinate and the previous y-coordinate.
     *
     * @see #getDtop()
     * @see #setDtop(int)
     */
    private int dtop;

    /**
     * The delta x-coordinate of the right border of this {@code Area}.
     * Represents the distance between the current x-coordinate and the previous x-coordinate.
     *
     * @see #getDright()
     * @see #setDright(int)
     */
    private int dright;

    /**
     * The delta y-coordinate of the bottom border of this {@code Area}.
     * Represents the distance between the current y-coordinate and the previous y-coordinate.
     *
     * @see #getDbottom()
     * @see #setDbottom(int)
     */
    private int dbottom;

    /**
     * The left {@link Wall} border of this {@code Area}.
     *
     * @see #getLeftBorder()
     */
    private final Wall leftBorder = new Wall(this, false);

    /**
     * The top {@link FloorCeiling} border of this {@code Area}.
     *
     * @see #getTopBorder()
     */
    private final FloorCeiling topBorder = new FloorCeiling(this, false);

    /**
     * The right {@link Wall} border of this {@code Area}.
     *
     * @see #getRightBorder()
     */
    private final Wall rightBorder = new Wall(this, true);

    /**
     * The bottom {@link FloorCeiling} border of this {@code Area}.
     *
     * @see #getBottomBorder()
     */
    private final FloorCeiling bottomBorder = new FloorCeiling(this, true);

    /**
     * Creates a new {@code Area}. Calculating delta values is enabled by default.
     */
    public Area() {
        calcDeltas = true;
    }

    /**
     * Creates a new {@code Area}.
     *
     * @param calcDeltas Whether this {@code Area} should calculate the delta values of {@link #left}, {@link #top},
     * {@link #right}, and {@link #bottom} whenever {@link #set} is called.
     * If {@code false}, the getters for the delta values will always return 0.
     */
    public Area(boolean calcDeltas) {
        this.calcDeltas = calcDeltas;
    }

    /**
     * Gets whether this {@code Area} is visible.
     * Generally, if this returns {@code false}, {@link com.group_finity.mascot.Mascot Mascot} objects
     * should not be able to interact with this {@code Area}.
     *
     * @return {@code true} if this {@code Area} is visible; {@code false} otherwise
     * @see #setVisible(boolean)
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Sets whether this {@code Area} is visible.
     *
     * @param visible {@code true} to make this {@code Area} visible; {@code false} to make it invisible
     * @see #isVisible()
     */
    public void setVisible(final boolean visible) {
        this.visible = visible;
    }

    /**
     * Gets the x-coordinate of the left border of this {@code Area}.
     *
     * @return the x-coordinate of the left border of this {@code Area}
     * @see #setLeft(int)
     */
    public int getLeft() {
        return left;
    }

    /**
     * Sets the x-coordinate of the left border of this {@code Area}.
     *
     * @param left the new x-coordinate of the left border
     * @see #getLeft()
     */
    public void setLeft(final int left) {
        this.left = left;
    }

    /**
     * Gets the y-coordinate of the top border of this {@code Area}.
     *
     * @return the y-coordinate of the top border of this {@code Area}
     * @see #setTop(int)
     */
    public int getTop() {
        return top;
    }

    /**
     * Sets the y-coordinate of the top border of this {@code Area}.
     *
     * @param top the new y-coordinate of the top border
     * @see #getTop()
     */
    public void setTop(final int top) {
        this.top = top;
    }

    /**
     * Gets the x-coordinate of the right border of this {@code Area}.
     *
     * @return the x-coordinate of the right border of this {@code Area}
     * @see #setRight(int)
     */
    public int getRight() {
        return right;
    }

    /**
     * Sets the x-coordinate of the right border of this {@code Area}.
     *
     * @param right the new x-coordinate of the right border
     * @see #getRight()
     */
    public void setRight(final int right) {
        this.right = right;
    }

    /**
     * Gets the y-coordinate of the bottom border of this {@code Area}.
     *
     * @return the y-coordinate of the bottom border of this {@code Area}
     * @see #setBottom(int)
     */
    public int getBottom() {
        return bottom;
    }

    /**
     * Sets the y-coordinate of the bottom border of this {@code Area}.
     *
     * @param bottom the new y-coordinate of the bottom border
     * @see #getBottom()
     */
    public void setBottom(final int bottom) {
        this.bottom = bottom;
    }

    /**
     * Gets the delta x-coordinate of the left border of this {@code Area}.
     * The returned value represents the distance between
     * the current x-coordinate and the previous x-coordinate.
     *
     * @return the delta x-coordinate of the left border of this {@code Area}
     * @see #setDleft(int)
     */
    public int getDleft() {
        return calcDeltas ? dleft : 0;
    }

    /**
     * Sets the delta x-coordinate of the left border of this {@code Area}.
     *
     * @param dleft the new delta x-coordinate of the left border
     * @see #getDleft()
     */
    public void setDleft(final int dleft) {
        if (calcDeltas) {
            this.dleft = dleft;
        }
    }

    /**
     * Gets the delta y-coordinate of the top border of this {@code Area}.
     * The returned value represents the distance between
     * the current y-coordinate and the previous y-coordinate.
     *
     * @return the delta x-coordinate of the top border of this {@code Area}
     * @see #setDtop(int)
     */
    public int getDtop() {
        return calcDeltas ? dtop : 0;
    }

    /**
     * Sets the delta y-coordinate of the top border of this {@code Area}.
     *
     * @param dtop the new delta y-coordinate of the top border
     * @see #getDtop()
     */
    public void setDtop(final int dtop) {
        if (calcDeltas) {
            this.dtop = dtop;
        }
    }

    /**
     * Gets the delta x-coordinate of the right border of this {@code Area}.
     * The returned value represents the distance between
     * the current x-coordinate and the previous x-coordinate.
     *
     * @return the delta x-coordinate of the right border of this {@code Area}
     * @see #setDright(int)
     */
    public int getDright() {
        return calcDeltas ? dright : 0;
    }

    /**
     * Sets the delta x-coordinate of the right border of this {@code Area}.
     *
     * @param dright the new delta x-coordinate of the right border
     * @see #getDright()
     */
    public void setDright(final int dright) {
        if (calcDeltas) {
            this.dright = dright;
        }
    }

    /**
     * Gets the delta y-coordinate of the bottom border of this {@code Area}.
     * The returned value represents the distance between
     * the current y-coordinate and the previous y-coordinate.
     *
     * @return the delta x-coordinate of the bottom border of this {@code Area}
     * @see #setDbottom(int)
     */
    public int getDbottom() {
        return calcDeltas ? dbottom : 0;
    }

    /**
     * Sets the delta y-coordinate of the bottom border of this {@code Area}.
     *
     * @param dbottom the new delta y-coordinate of the bottom border
     * @see #getDbottom()
     */
    public void setDbottom(final int dbottom) {
        if (calcDeltas) {
            this.dbottom = dbottom;
        }
    }

    /**
     * Gets the left {@link Wall} border of this {@code Area}.
     *
     * @return the left {@code Wall} border of this {@code Area}
     */
    public Wall getLeftBorder() {
        return leftBorder;
    }

    /**
     * Gets the top {@link FloorCeiling} border of this {@code Area}.
     *
     * @return the top {@code FloorCeiling} border of this {@code Area}
     */
    public FloorCeiling getTopBorder() {
        return topBorder;
    }

    /**
     * Gets the right {@link Wall} border of this {@code Area}.
     *
     * @return the right {@code Wall} border of this {@code Area}
     */
    public Wall getRightBorder() {
        return rightBorder;
    }

    /**
     * Gets the bottom {@link FloorCeiling} border of this {@code Area}.
     *
     * @return the bottom {@code FloorCeiling} border of this {@code Area}
     */
    public FloorCeiling getBottomBorder() {
        return bottomBorder;
    }

    /**
     * Gets the width of this {@code Area}.
     *
     * @return the width of this {@code Area}
     */
    public int getWidth() {
        return right - left;
    }

    /**
     * Gets the height of this {@code Area}.
     *
     * @return the height of this {@code Area}
     */
    public int getHeight() {
        return bottom - top;
    }

    /**
     * Sets the bounds of this {@code Area} to match the specified {@link Rectangle}.
     * This will also update the delta values of this {@code Area}.
     *
     * @param r the specified {@code Rectangle}
     */
    public void set(final Rectangle r) {
        setRect(r.x, r.y, r.width, r.height);
    }

    /**
     * Sets the bounds of this {@code Area} to the specified
     * {@code x}, {@code y}, {@code width}, and {@code height} values.
     * This will also update the delta values of this {@code Area}.
     *
     * @param x the new x-coordinate for the upper-left corner of this {@code Area}
     * @param y the new y-coordinate for the upper-left corner of this {@code Area}
     * @param width the new width of this {@code Area}
     * @param height the new height of this {@code Area}
     */
    public void setRect(final int x, final int y, final int width, final int height) {
        set(x, y, x + width, y + height);
    }

    /**
     * Sets the bounds of this {@code Area} to the specified
     * {@code left}, {@code top}, {@code right}, and {@code bottom} values.
     * This will also update the delta values of this {@code Area}.
     *
     * @param left the new x-coordinate for the left border of this {@code Area}
     * @param top the new y-coordinate for the top border of this {@code Area}
     * @param right the new x-coordinate for the right border of this {@code Area}
     * @param bottom the new y-coordinate for the bottom border of this {@code Area}
     */
    public void set(final int left, final int top, final int right, final int bottom) {
        if (calcDeltas) {
            dleft = left - this.left;
            dtop = top - this.top;
            dright = right - this.right;
            dbottom = bottom - this.bottom;
        }

        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    /**
     * Resets all delta values in this {@code Area} to 0.
     */
    public void resetDeltas() {
        if (calcDeltas) {
            dleft = 0;
            dtop = 0;
            dright = 0;
            dbottom = 0;
        }
    }

    /**
     * Checks whether this {@code Area} contains the specified {@link Point}.
     *
     * @param point the {@code Point} to test
     * @return {@code true} if the specified {@code Point}
     * is inside this {@code Area}; {@code false} otherwise
     * @see Rectangle#contains(Point)
     */
    public boolean contains(final Point point) {
        return contains(point.x, point.y);
    }

    /**
     * Checks whether this {@code Area} contains the point at the specified location {@code (x, y)}.
     *
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     * @return {@code true} if the specified point
     * is inside this {@code Area}; {@code false} otherwise
     * @see Rectangle#contains(int, int)
     */
    public boolean contains(final int x, final int y) {
        if ((right - left | bottom - top) < 0) {
            // At least one of the dimensions is negative
            return false;
        }

        return left <= x && x <= right && top <= y && y <= bottom;
    }

    /**
     * Checks whether this {@code Area} entirely contains the specified {@code Area}.
     *
     * @param a the specified {@code Area}
     * @return {@code true} if the specified {@code Area}
     * is contained entirely inside this {@code Area}; {@code false} otherwise
     * @see Rectangle#contains(Rectangle)
     */
    public boolean contains(Area a) {
        return contains(a.left, a.top, a.right, a.bottom);
    }

    /**
     * Checks whether this {@code Area} entirely contains the {@code Area} bounded by the specified
     * x-coordinates {@code left} and {@code right}, and the specified y-coordinates {@code top} and {@code bottom}.
     *
     * @param left the x-coordinate for the left border of the {@code Area}
     * @param top the y-coordinate for the top border of the {@code Area}
     * @param right the x-coordinate for the right border of the {@code Area}
     * @param bottom the y-coordinate for the bottom border of the {@code Area}
     * @return {@code true} if the {@code Area} specified by {@code (left, top, right, bottom)}
     * is contained entirely inside this {@code Area}; {@code false} otherwise
     * @see Rectangle#contains(int, int, int, int)
     */
    // Adapted from Rectangle.contains(int, int, int, int)
    public boolean contains(int left, int top, int right, int bottom) {
        if ((this.right - this.left | this.bottom - this.top | right - left | bottom - top) < 0) {
            // At least one of the dimensions is negative
            return false;
        }

        // Note: If either dimension is zero, tests below must return false
        if (left < this.left || top < this.top) {
            return false;
        }

        if (right <= left) {
            if (this.right >= this.left || right > this.right) return false;
        } else {
            if (this.right >= this.left && right > this.right) return false;
        }

        if (bottom <= top) {
            if (this.bottom >= this.top || bottom > this.bottom) return false;
        } else {
            if (this.bottom >= this.top && bottom > this.bottom) return false;
        }
        return true;
    }

    /**
     * Determines whether this {@code Area} and the specified
     * {@code Rectangle} intersect. An area and a rectangle intersect if
     * their intersection is nonempty.
     *
     * @param r the specified {@code Rectangle}
     * @return    {@code true} if the specified {@code Rectangle}
     *            and this {@code Area} intersect;
     *            {@code false} otherwise
     * @see Rectangle#intersects(Rectangle)
     */
    // Adapted from Rectangle.intersects(Rectangle)
    public boolean intersects(Rectangle r) {
        int tw = right - left;
        int th = bottom - top;
        int rw = r.width;
        int rh = r.height;
        if (rw <= 0 || rh <= 0 || tw <= 0 || th <= 0) {
            return false;
        }
        int tx = left;
        int ty = top;
        int rx = r.x;
        int ry = r.y;
        tw = right;
        th = bottom;
        rw += rx;
        rh += ry;
        //      overflow || intersect
        return (rw < rx || rw > tx) &&
                (rh < ry || rh > ty) &&
                (tw < tx || tw > rx) &&
                (th < ty || th > ry);
    }

    /**
     * Determines whether this {@code Area} and the specified
     * {@code Area} intersect. Two areas intersect if
     * their intersection is nonempty.
     *
     * @param a the specified {@code Area}
     * @return    {@code true} if the specified {@code Area}
     *            and this {@code Area} intersect;
     *            {@code false} otherwise
     * @see Rectangle#intersects(Rectangle)
     */
    // Adapted from Rectangle.intersects(Rectangle)
    public boolean intersects(Area a) {
        int tw = right - left;
        int th = bottom - top;
        int aw = a.right - a.left;
        int ah = a.bottom - a.top;
        if (aw <= 0 || ah <= 0 || tw <= 0 || th <= 0) {
            return false;
        }
        int tx = left;
        int ty = top;
        int ax = a.left;
        int ay = a.top;
        tw = right;
        th = bottom;
        aw = a.right;
        ah = a.bottom;
        //      overflow || intersect
        return (aw < ax || aw > tx) &&
                (ah < ay || ah > ty) &&
                (tw < tx || tw > ax) &&
                (th < ty || th > ay);
    }

    /**
     * Creates a {@link Rectangle} with the same bounds as this {@code Area}.
     *
     * @return a {@code Rectangle} with the same bounds as this {@code Area}
     */
    public Rectangle toRectangle() {
        return new Rectangle(left, top, right - left, bottom - top);
    }

    @Override
    public String toString() {
        return "Area[left=" + left + ",top=" + top + ",right=" + right + ",bottom=" + bottom + ']';
    }
}
