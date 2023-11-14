package com.group_finity.mascot.win.jna;

import com.sun.jna.Structure;

/**
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
public class RECT extends Structure {

    public int left;
    public int top;
    public int right;
    public int bottom;

    public int Width() {
        return right - left;
    }

    public int Height() {
        return bottom - top;
    }

    public void OffsetRect(final int dx, final int dy) {
        left += dx;
        right += dx;
        top += dy;
        bottom += dy;
    }
}
