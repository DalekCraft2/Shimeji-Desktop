package com.group_finity.mascot.environment;

import lombok.Data;

import java.awt.*;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
@Data
public class Location {
    private int x;
    private int y;

    private int dx;
    private int dy;

    public void set(final Point value) {
        setDx((getDx() + value.x - getX()) / 2);
        setDy((getDy() + value.y - getY()) / 2);

        setX(value.x);
        setY(value.y);
    }
}
