package com.group_finity.mascot.win.jna;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.win32.StdCallLibrary;

/**
 * Wraps up Dwmapi to get access to the new Cloaked variable.
 * <p>
 * Visit <a href="https://kilkakon.com/shimeji">kilkakon.com/shimeji</a> for updates
 *
 * @author Kilkakon
 */
public interface Dwmapi extends StdCallLibrary {
    Dwmapi INSTANCE = Native.load("Dwmapi", Dwmapi.class);

    int DWMWA_CLOAKED = 14;

    NativeLong DwmGetWindowAttribute(WinDef.HWND hwnd, int dwAttribute, LongByReference pvAttribute, int cbAttribute);
}
