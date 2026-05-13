package com.group_finity.mascot.environment;

import java.awt.*;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Area {

    private boolean visible = true;

    private int left;

    private int top;

    private int right;

    private int bottom;

    private int dleft;

    private int dtop;

    private int dright;

    private int dbottom;

    private final Wall leftBorder = new Wall(this, false);

    private final FloorCeiling topBorder = new FloorCeiling(this, false);

    private final Wall rightBorder = new Wall(this, true);

    private final FloorCeiling bottomBorder = new FloorCeiling(this, true);

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(final boolean visible) {
        this.visible = visible;
    }

    public int getLeft() {
        return left;
    }

    public void setLeft(final int left) {
        this.left = left;
    }

    public int getTop() {
        return top;
    }

    public void setTop(final int top) {
        this.top = top;
    }

    public int getRight() {
        return right;
    }

    public void setRight(final int right) {
        this.right = right;
    }

    public int getBottom() {
        return bottom;
    }

    public void setBottom(final int bottom) {
        this.bottom = bottom;
    }

    public int getDleft() {
        return dleft;
    }

    public void setDleft(final int dleft) {
        this.dleft = dleft;
    }

    public int getDtop() {
        return dtop;
    }

    public void setDtop(final int dtop) {
        this.dtop = dtop;
    }

    public int getDright() {
        return dright;
    }

    public void setDright(final int dright) {
        this.dright = dright;
    }

    public int getDbottom() {
        return dbottom;
    }

    public void setDbottom(final int dbottom) {
        this.dbottom = dbottom;
    }

    public Wall getLeftBorder() {
        return leftBorder;
    }

    public FloorCeiling getTopBorder() {
        return topBorder;
    }

    public Wall getRightBorder() {
        return rightBorder;
    }

    public FloorCeiling getBottomBorder() {
        return bottomBorder;
    }

    public int getWidth() {
        return right - left;
    }

    public int getHeight() {
        return bottom - top;
    }

    public void set(final Rectangle value) {
        setRect(value.x, value.y, value.width, value.height);
    }

    public void setRect(final int x, final int y, final int width, final int height) {
        set(x, y, x + width, y + height);
    }

    public void set(final int left, final int top, final int right, final int bottom) {
        dleft = left - this.left;
        dtop = top - this.top;
        dright = right - this.right;
        dbottom = bottom - this.bottom;

        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public boolean contains(final Point point) {
        return contains(point.x, point.y);
    }

    public boolean contains(final int x, final int y) {
        if ((right - left | bottom - top) < 0) {
            // At least one of the dimensions is negative
            return false;
        }

        return left <= x && x <= right && top <= y && y <= bottom;
    }

    public boolean contains(Area a) {
        return contains(a.left, a.top, a.right, a.bottom);
    }

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

    public Rectangle toRectangle() {
        return new Rectangle(left, top, right - left, bottom - top);
    }

    @Override
    public String toString() {
        return "Area[left=" + left + ",top=" + top + ",right=" + right + ",bottom=" + bottom + "]";
    }
}
