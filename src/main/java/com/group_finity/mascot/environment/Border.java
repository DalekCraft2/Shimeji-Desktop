package com.group_finity.mascot.environment;

import java.awt.*;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public interface Border {

    boolean isOn(Point location);

    Point move(Point location);
}
