package com.group_finity.mascot.image;

import java.awt.*;

/**
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
public interface TranslucentWindow {
    Component asComponent();

    void setImage(NativeImage image);

    void updateImage();

    void dispose();

    void setAlwaysOnTop(boolean onTop);
}
