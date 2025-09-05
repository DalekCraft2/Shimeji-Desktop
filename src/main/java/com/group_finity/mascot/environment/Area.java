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
        dleft = value.x - left;
        dtop = value.y - top;
        dright = value.x + value.width - right;
        dbottom = value.y + value.height - bottom;

        left = value.x;
        top = value.y;
        right = value.x + value.width;
        bottom = value.y + value.height;
    }

    public boolean contains(final int x, final int y) {
        return left <= x && x <= right && top <= y && y <= bottom;
    }

    public Rectangle toRectangle() {
        return new Rectangle(left, top, right - left, bottom - top);
    }

    @Override
    public String toString() {
        return "Area [left=" + left + ", top=" + top + ", right=" + right + ", bottom=" + bottom + "]";
    }
}
