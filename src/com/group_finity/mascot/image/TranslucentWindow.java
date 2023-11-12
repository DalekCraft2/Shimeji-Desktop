package com.group_finity.mascot.image;

import javax.swing.*;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public interface TranslucentWindow {

    JWindow asJWindow();

    void setImage(NativeImage image);

    void updateImage();
}
