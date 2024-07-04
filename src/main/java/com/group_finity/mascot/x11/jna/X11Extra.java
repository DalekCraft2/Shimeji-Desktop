package com.group_finity.mascot.x11.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.unix.X11.Display;
import com.sun.jna.platform.unix.X11.Window;

public interface X11Extra extends Library {
    X11Extra INSTANCE = Native.load("X11", X11Extra.class);

    void XMoveWindow(Display display, Window w, int x, int y);
}
