package com.group_finity.mascot.win.jna;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.HRGN;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public interface User32Extra extends StdCallLibrary {
    User32Extra INSTANCE = Native.load("user32", User32Extra.class, W32APIOptions.DEFAULT_OPTIONS);

    int ERROR = 0;
    int NULLREGION = 1;
    int SIMPLEREGION = 2;
    int COMPLEXREGION = 3;

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-getwindowrgn">Microsoft docs: GetWindowRgn</a>
     * <p>
     * The GetWindowRgn function obtains a copy of the window region of a window.
     * The window region of a window is set by calling the {@link com.sun.jna.platform.win32.User32#SetWindowRgn(HWND, HRGN, boolean) SetWindowRgn} function.
     * The window region determines the area within the window where the system permits drawing. The system does not display
     * any portion of a window that lies outside of the window region.
     *
     * @param hWnd Handle to the window whose window region is to be obtained.
     * @param hRgn Handle to the region which will be modified to represent the window region.
     * @return The return value specifies the type of the region that the function obtains. It can be one of the following values.
     * <p>{@code NULLREGION} - The region is empty.
     * <p>{@code SIMPLEREGION} - The region is a single rectangle.
     * <p>{@code COMPLEXREGION} - The region is more than one rectangle.
     * <p>{@code ERROR} - The specified window does not have a region, or an error occurred while attempting to return the region.
     */
    int GetWindowRgn(HWND hWnd, HRGN hRgn);

    /**
     * <a href="https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-getwindowrgnbox">Microsoft docs: GetWindowRgnBox</a>
     * <p>
     * The GetWindowRgnBox function retrieves the dimensions of the tightest bounding rectangle for the window region of a window.
     *
     * @param hWnd Handle to the window.
     * @param lprc Pointer to a {@link RECT RECT} structure that receives the rectangle dimensions, in device units relative to the upper-left corner of the window.
     * @return The return value specifies the type of the region that the function obtains. It can be one of the following values.
     * <p>{@code COMPLEXREGION} - The region is more than one rectangle.
     * <p>{@code ERROR} - The specified window does not have a region, or an error occurred while attempting to return the region.
     * <p>{@code NULLREGION} - The region is empty.
     * <p>{@code SIMPLEREGION} - The region is a single rectangle.
     */
    int GetWindowRgnBox(HWND hWnd, RECT lprc);
}
