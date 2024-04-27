package com.group_finity.mascot.image;

import java.awt.*;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public interface NativeImage {
    Graphics2D createGraphics();

    int getWidth();

    int getHeight();

    Object getProperty(final String name);
}
