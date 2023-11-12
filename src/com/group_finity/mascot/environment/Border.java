package com.group_finity.mascot.environment;

import java.awt.*;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public interface Border {

    boolean isOn(Point location);

    Point move(Point location);
}
