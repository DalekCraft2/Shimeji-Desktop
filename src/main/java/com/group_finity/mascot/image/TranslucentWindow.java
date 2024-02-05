package com.group_finity.mascot.image;

import java.awt.*;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public interface TranslucentWindow {
    Component asComponent();

    void setImage(NativeImage image);

    void updateImage();

    void dispose();

    void setAlwaysOnTop(boolean onTop);
}
