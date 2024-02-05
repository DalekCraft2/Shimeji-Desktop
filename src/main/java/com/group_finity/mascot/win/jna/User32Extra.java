package com.group_finity.mascot.win.jna;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public interface User32Extra extends StdCallLibrary {

    User32Extra INSTANCE = Native.load("User32", User32Extra.class);

    int SPI_GETWORKAREA = 48;

    boolean SystemParametersInfoW(int uiAction, int uiParam, WinDef.RECT pvParam, int fWinIni);

    long GetWindowLongW(WinDef.HWND hWnd, int nIndex);

    long SetWindowLongW(WinDef.HWND hWnd, int nIndex, long dwNewLong);

    boolean IsIconic(WinDef.HWND hWnd);

    boolean IsZoomed(WinDef.HWND hWnd);

    int GetWindowTextW(WinDef.HWND hWnd, char[] lpString, int nMaxCount);

    int GetClassNameW(WinDef.HWND hWnd, char[] lpClassName, int nMaxCount);

    int ERROR = 0;

    int GetWindowRgn(WinDef.HWND hWnd, WinDef.HRGN hRgn);

    int GetWindowRgnBox(WinDef.HWND hWnd, WinDef.RECT lprc);

    boolean SetProcessDPIAware();
}
