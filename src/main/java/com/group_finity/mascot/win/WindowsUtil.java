package com.group_finity.mascot.win;

import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

/**
 * Utility functions for native Windows operations.
 *
 * @author DalekCraft
 */
public final class WindowsUtil {
    /* public static int GetWindowLong(HWND hWnd, int nIndex) {
        if (Platform.is64Bit()) {
            return User32.INSTANCE.GetWindowLongPtr(hWnd, nIndex).intValue();
        } else {
            // GetWindowLongPtr() does not exist on 32-bit Windows.
            return User32.INSTANCE.GetWindowLong(hWnd, nIndex);
        }
    }

    public static int SetWindowLong(HWND hWnd, int nIndex, int dwNewLong) {
        if (Platform.is64Bit()) {
            return (int) Pointer.nativeValue(User32.INSTANCE.SetWindowLongPtr(hWnd, nIndex, Pointer.createConstant(dwNewLong)));
        } else {
            // SetWindowLongPtr() does not exist on 32-bit Windows.
            return User32.INSTANCE.SetWindowLong(hWnd, nIndex, dwNewLong);
        }
    } */

    public static BaseTSD.LONG_PTR GetWindowLong(HWND hWnd, int nIndex) {
        if (Platform.is64Bit()) {
            return User32.INSTANCE.GetWindowLongPtr(hWnd, nIndex);
        } else {
            // GetWindowLongPtr() does not exist on 32-bit Windows.
            return new BaseTSD.LONG_PTR(User32.INSTANCE.GetWindowLong(hWnd, nIndex));
        }
    }

    public static Pointer SetWindowLong(HWND hWnd, int nIndex, Pointer dwNewLong) {
        if (Platform.is64Bit()) {
            return User32.INSTANCE.SetWindowLongPtr(hWnd, nIndex, dwNewLong);
        } else {
            // SetWindowLongPtr() does not exist on 32-bit Windows.
            return Pointer.createConstant(User32.INSTANCE.SetWindowLong(hWnd, nIndex, (int) Pointer.nativeValue(dwNewLong)));
        }
    }
}
