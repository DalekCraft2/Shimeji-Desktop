package com.group_finity.mascot.environment;

import java.awt.*;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public interface Border {

    boolean isOn(Point location);

    Point move(Point location);
}
