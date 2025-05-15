package com.group_finity.mascot.environment;

import lombok.NoArgsConstructor;

import java.awt.*;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
@NoArgsConstructor
public class NotOnBorder implements Border {
    public static final NotOnBorder INSTANCE = new NotOnBorder();

    @Override
    public boolean isOn(final Point location) {
        return false;
    }

    @Override
    public Point move(final Point location) {
        return location;
    }
}
