package com.group_finity.mascot.environment;

import java.awt.*;

/**
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
public interface Border {

    boolean isOn(Point location);

    Point move(Point location);
}
