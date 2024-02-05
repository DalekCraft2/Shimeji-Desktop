package com.group_finity.mascot.environment;

import java.awt.*;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
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
        return getRight() - getLeft();
    }

    public int getHeight() {
        return getBottom() - getTop();
    }

    public void set(final Rectangle value) {

        setDleft(value.x - getLeft());
        setDtop(value.y - getTop());
        setDright(value.x + value.width - getRight());
        setDbottom(value.y + value.height - getBottom());

        setLeft(value.x);
        setTop(value.y);
        setRight(value.x + value.width);
        setBottom(value.y + value.height);
    }

    public boolean contains(final int x, final int y) {

        return getLeft() <= x && x <= getRight() && getTop() <= y && y <= getBottom();
    }

    public Rectangle toRectangle() {
        return new Rectangle(left, top, right - left, bottom - top);
    }

    @Override
    public String toString() {
        return "Area [left=" + left + ", top=" + top + ", right=" + right + ", bottom=" + bottom + "]";
    }

}
