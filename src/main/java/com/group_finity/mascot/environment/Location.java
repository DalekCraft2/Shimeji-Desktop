package com.group_finity.mascot.environment;

import java.awt.*;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Location {

    private int x;
    private int y;

    private int dx;
    private int dy;

    public int getX() {
        return x;
    }

    public void setX(final int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(final int y) {
        this.y = y;
    }

    public int getDx() {
        return dx;
    }

    public void setDx(final int dx) {
        this.dx = dx;
    }

    public int getDy() {
        return dy;
    }

    public void setDy(final int dy) {
        this.dy = dy;
    }

    public void set(final Point value) {
        dx = (dx + value.x - x) / 2;
        dy = (dy + value.y - y) / 2;

        x = value.x;
        y = value.y;
    }
}
