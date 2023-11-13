package com.group_finity.mascot.image;

import javax.swing.*;

/**
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
public interface TranslucentWindow {

    JWindow asJWindow();

    void setImage(NativeImage image);

    void updateImage();
}
