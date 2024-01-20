package com.group_finity.mascot.win.jna;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.StdCallLibrary;

/**
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
public interface GDI32Extra extends StdCallLibrary {

    GDI32Extra INSTANCE = Native.load("Gdi32", GDI32Extra.class);

    // WinDef.HDC CreateCompatibleDC(WinDef.HDC hdc);

    // WinNT.HANDLE SelectObject(WinDef.HDC hdc, WinNT.HANDLE h);

    // boolean DeleteDC(WinDef.HDC hdc);

    // int DIB_RGB_COLORS = 0;

    // WinDef.HBITMAP CreateDIBSection(WinDef.HDC hdc, WinGDI.BITMAPINFO pbmi, int usage, Pointer ppvBits, WinNT.HANDLE hSection, int offset);

    int GetObjectW(WinNT.HANDLE hgdiobj, int cbBuffer, WinGDI.BITMAP lpvObject);

    // boolean DeleteObject(WinNT.HANDLE hObject);

    // WinDef.HRGN CreateRectRgn(
    //         int nLeftRect,
    //         int nTopRect,
    //         int nRightRect,
    //         int nBottomRect
    // );

    int GetRgnBox(WinDef.HRGN hrgn, WinDef.RECT lprc);
}
