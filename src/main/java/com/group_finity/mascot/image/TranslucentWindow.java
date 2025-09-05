package com.group_finity.mascot.image;

import java.awt.*;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public interface TranslucentWindow {
    Component asComponent();

    void setImage(NativeImage image);

    void updateImage();

    void dispose();

    void setAlwaysOnTop(boolean onTop);
}
