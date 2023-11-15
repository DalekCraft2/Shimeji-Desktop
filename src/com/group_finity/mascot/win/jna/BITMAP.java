package com.group_finity.mascot.win.jna;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
@Structure.FieldOrder({"bmType", "bmWidth", "bmHeight", "bmWidthBytes", "bmPlanes", "bmBitsPixel", "bmBits"})
public class BITMAP extends Structure {
    public int bmType;
    public int bmWidth;
    public int bmHeight;
    public int bmWidthBytes;
    public short bmPlanes;
    public short bmBitsPixel;
    public Pointer bmBits;
}
