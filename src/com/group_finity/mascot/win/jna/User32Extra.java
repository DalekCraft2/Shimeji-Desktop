package com.group_finity.mascot.win.jna;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;

/**
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
public interface User32Extra extends StdCallLibrary {

    User32Extra INSTANCE = Native.load("User32", User32Extra.class);

    // int SM_CXSCREEN = 0;
    // int SM_CYSCREEN = 1;

    // int GetSystemMetrics(int nIndex);

    int SPI_GETWORKAREA = 48;

    boolean SystemParametersInfoW(int uiAction, int uiParam, WinDef.RECT pvParam, int fWinIni);

    // WinDef.HWND GetForegroundWindow();

    // int GW_HWNDFIRST = 0;
    // int GW_HWNDNEXT = 2;

    // WinDef.HWND GetWindow(WinDef.HWND hWnd, int uCmd);

    // boolean IsWindow(WinDef.HWND hWnd);

    // boolean IsWindowVisible(WinDef.HWND hWnd);

    // int GWL_STYLE = -16;
    // int GWL_EXSTYLE = -20;

    long GetWindowLongW(WinDef.HWND hWnd, int nIndex);

    long SetWindowLongW(WinDef.HWND hWnd, int nIndex, long dwNewLong);

    // int WS_MAXIMIZE = 0x01000000;
    // int WS_EX_LAYERED = 0x00080000;

    boolean IsIconic(WinDef.HWND hWnd);

    boolean IsZoomed(WinDef.HWND hWnd);

    int GetWindowTextW(WinDef.HWND hWnd, char[] lpString, int nMaxCount);

    int GetClassNameW(WinDef.HWND hWnd, char[] lpClassName, int nMaxCount);

    // boolean GetWindowRect(WinDef.HWND hWnd, WinDef.RECT lpRect);

    int ERROR = 0;

    int GetWindowRgn(WinDef.HWND hWnd, WinDef.HRGN hRgn);

    // int MoveWindow(WinDef.HWND hWnd, int X, int Y, int nWidth, int nHeight, boolean bRepaint);

    // boolean BringWindowToTop(WinDef.HWND hWnd);

    // WinDef.HDC GetDC(WinDef.HWND hWnd);

    // int ReleaseDC(WinDef.HWND hWnd, WinDef.HDC hDC);

    // int ULW_ALPHA = 2;

    // boolean UpdateLayeredWindow(WinDef.HWND hWnd, WinDef.HDC hdcDst,
    //                             WinDef.POINT pptDst, WinUser.SIZE psize,
    //                             WinDef.HDC hdcSrc, WinDef.POINT pptSrc, int crKey,
    //                             WinUser.BLENDFUNCTION pblend, int dwFlags);

    // interface WNDENUMPROC extends StdCallCallback {
    //     /**
    //      * Returns whether to continue enumeration.
    //      */
    //     boolean callback(WinDef.HWND hWnd, Pointer data);
    // }

    // boolean EnumWindows(WNDENUMPROC lpEnumFunc, Pointer data);

    boolean SetProcessDPIAware();

    // boolean GetMonitorInfoA(WinUser.HMONITOR hMonitor, WinUser.MONITORINFO lpmi); // TODO look into for future patches
}
