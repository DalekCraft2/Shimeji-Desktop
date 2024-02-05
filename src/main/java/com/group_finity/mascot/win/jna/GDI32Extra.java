package com.group_finity.mascot.win.jna;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.StdCallLibrary;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public interface GDI32Extra extends StdCallLibrary {

    GDI32Extra INSTANCE = Native.load("Gdi32", GDI32Extra.class);

    int GetObjectW(WinNT.HANDLE hgdiobj, int cbBuffer, WinGDI.BITMAP lpvObject);

    int GetRgnBox(WinDef.HRGN hrgn, WinDef.RECT lprc);
}
