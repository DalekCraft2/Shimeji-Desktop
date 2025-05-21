package com.group_finity.mascot.environment;

import lombok.Getter;
import lombok.Setter;

import java.awt.*;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
@Getter
public class Area {
    @Setter
    private boolean visible = true;

    @Setter private int left;

    @Setter private int top;

    @Setter private int right;

    @Setter private int bottom;

    @Setter private int dleft;

    @Setter private int dtop;

    @Setter private int dright;

    @Setter private int dbottom;

    private final Wall leftBorder = new Wall(this, false);

    private final FloorCeiling topBorder = new FloorCeiling(this, false);

    private final Wall rightBorder = new Wall(this, true);

    private final FloorCeiling bottomBorder = new FloorCeiling(this, true);

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
