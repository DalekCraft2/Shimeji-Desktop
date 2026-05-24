package com.group_finity.mascot.platform.win;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.environment.AbstractEnvironment;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.platform.win.jna.Dwmapi;
import com.group_finity.mascot.platform.win.jna.User32Extra;
import com.sun.jna.Pointer;
import com.sun.jna.platform.WindowUtils;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.platform.win32.WinUser.HMONITOR;
import com.sun.jna.platform.win32.WinUser.MONITORINFO;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;
import com.sun.jna.ptr.LongByReference;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Uses JNI to obtain environment information that is difficult to obtain with Java.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
class WindowsEnvironment extends AbstractEnvironment {
    private final HashMap<HWND, Boolean> interactiveCache = new LinkedHashMap<>();

    private final Area workArea = new Area(false);

    private final Area activeWindowDpiUnaware = new Area();
    private final Area activeWindow = new Area();

    private HWND activeWindowHandle = null;

    private String[] windowTitles = null;

    private String[] windowTitlesBlacklist = null;

    /**
     * Enumeration of the possible return statuses when checking whether
     * a given window is valid to be interactive at any given moment.
     *
     * @author LavenderSnek
     */
    private enum WindowStatus {
        /** The window is valid and will prevent other windows from being valid. */
        VALID,
        /** The window is invalid and will prevent other windows from being valid. */
        INVALID,
        /** The window is invalid but will not prevent other windows from being valid. */
        IGNORED,
        /**
         * The window is valid, but it is out of bounds and should be ignored.
         * It will not prevent other windows from being valid.
         */
        OUT_OF_BOUNDS
    }

    @Override
    public void tick() {
        super.tick();

        workArea.set(getWorkAreaRect(true));
        long prevWindowId = getActiveWindowId();
        // Get DPI-unaware window rectangle
        final Rectangle windowRect = getWindowRect(findActiveWindow(), false);
        activeWindowDpiUnaware.set(windowRect);

        // Calculate the DPI-aware rectangle here to avoid calling getWindowRect() a second time
        double dpiScaleInverse = 96.0 / Toolkit.getDefaultToolkit().getScreenResolution();
        if (dpiScaleInverse != 1) {
            windowRect.x = (int) Math.round(windowRect.x * dpiScaleInverse);
            windowRect.y = (int) Math.round(windowRect.y * dpiScaleInverse);
            windowRect.width = (int) Math.round(windowRect.width * dpiScaleInverse);
            windowRect.height = (int) Math.round(windowRect.height * dpiScaleInverse);
        }
        activeWindow.setVisible(getScreen().intersects(windowRect));
        activeWindow.set(windowRect);

        if (prevWindowId != getActiveWindowId()) {
            // If the active window has changed, reset the active window's deltas to 0
            activeWindow.resetDeltas();
            activeWindowDpiUnaware.resetDeltas();
        }
    }

    private boolean isInteractive(final HWND hWnd) {
        final Boolean cachedValue = interactiveCache.get(hWnd);
        if (cachedValue != null) {
            return cachedValue;
        }

        // Determine whether the window is interactive based on its title
        final String windowTitle = WindowUtils.getWindowTitle(hWnd);

        // optimisation to remove empty windows from consideration without the loop.
        if (windowTitle.isEmpty()) {
            interactiveCache.put(hWnd, false);
            return false;
        }

        // blacklist takes precedence over whitelist
        boolean blacklistInUse = false;
        if (windowTitlesBlacklist == null) {
            windowTitlesBlacklist = Main.getInstance().getSettings().interactiveWindowsBlacklist.toArray(new String[0]);
        }
        for (String title : windowTitlesBlacklist) {
            if (!title.trim().isEmpty()) {
                blacklistInUse = true;
                if (windowTitle.contains(title)) {
                    interactiveCache.put(hWnd, false);
                    return false;
                }
            }
        }

        // whitelist
        boolean whitelistInUse = false;
        if (windowTitles == null) {
            windowTitles = Main.getInstance().getSettings().interactiveWindows.toArray(new String[0]);
        }
        for (String title : windowTitles) {
            if (!title.trim().isEmpty()) {
                // Window is interactive
                whitelistInUse = true;
                if (windowTitle.contains(title)) {
                    interactiveCache.put(hWnd, true);
                    return true;
                }
            }
        }

        if (whitelistInUse || !blacklistInUse) {
            // Window is not interactive
            interactiveCache.put(hWnd, false);
            return false;
        } else {
            // Window is interactive
            interactiveCache.put(hWnd, true);
            return true;
        }
    }

    private WindowStatus getWindowStatus(HWND hWnd) {
        if (User32.INSTANCE.IsWindowVisible(hWnd)) {
            // DWMWA_CLOAKED is not supported on Windows 7 and earlier, so check that we are on at least Windows 8
            if (VersionHelpers.IsWindows8OrGreater()) {
                // metro apps can be closed or minimised and still be considered "visible" by User32
                // have to consider the new cloaked variable instead
                LongByReference flagsRef = new LongByReference();
                HRESULT result = Dwmapi.INSTANCE.DwmGetWindowAttribute(hWnd, Dwmapi.DWMWA_CLOAKED, flagsRef.getPointer(), 8);
                if (result.equals(WinError.S_OK) && flagsRef.getValue() != 0) {
                    return WindowStatus.IGNORED;
                }
            }

            if (User32Extra.INSTANCE.IsZoomed(hWnd)) {
                // Window is maximized and is therefore invalid
                return WindowStatus.INVALID;
            }

            if (isInteractive(hWnd) && !User32Extra.INSTANCE.IsIconic(hWnd)) {
                // Window is valid
                Rectangle windowRect = getWindowRect(hWnd, true);
                if (getScreen().intersects(windowRect)) {
                    return WindowStatus.VALID;
                } else {
                    // Window is out of bounds and will be ignored
                    return WindowStatus.OUT_OF_BOUNDS;
                }
            }
        }

        // Window is ignored
        return WindowStatus.IGNORED;
    }

    private HWND findActiveWindow() {
        activeWindowHandle = null;

        User32.INSTANCE.EnumWindows((hWnd, data) -> switch (getWindowStatus(hWnd)) {
            case VALID -> {
                activeWindowHandle = hWnd;
                yield false;
            }
            case IGNORED, OUT_OF_BOUNDS -> true;
            default -> { // The window is invalid, so abort the search here
                activeWindowHandle = null;
                yield false;
            }
        }, null);

        return activeWindowHandle;
    }

    /**
     * Gets the given window's area.
     *
     * @return the window's area
     */
    private static Rectangle getWindowRect(HWND hWnd, boolean dpiAware) {
        if (hWnd == null) {
            return new Rectangle();
        }
        // Get and return window rectangle
        final Rectangle rect;
        try {
            rect = WindowUtils.getWindowLocationAndSize(hWnd);
        } catch (Win32Exception e) {
            if (e.getHR().intValue() != WinError.E_HANDLE) {
                // The exception was not due to the window handle being invalid, so rethrow the exception
                throw e;
            }
            return new Rectangle();
        }
        if (dpiAware) {
            double dpiScaleInverse = 96.0 / Toolkit.getDefaultToolkit().getScreenResolution();
            if (dpiScaleInverse != 1) {
                rect.x = (int) Math.round(rect.x * dpiScaleInverse);
                rect.y = (int) Math.round(rect.y * dpiScaleInverse);
                rect.width = (int) Math.round(rect.width * dpiScaleInverse);
                rect.height = (int) Math.round(rect.height * dpiScaleInverse);
            }
        }
        return rect;
    }

    /**
     * Gets the work area. This area is the display area excluding the taskbar.
     *
     * @return work area
     */
    private static Rectangle getWorkAreaRect(boolean dpiAware) {
        if (dpiAware) {
            GraphicsConfiguration config = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            Rectangle rect = config.getBounds();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
            rect.x += insets.left;
            rect.y += insets.top;
            rect.width -= insets.right;
            rect.height -= insets.bottom;
            return rect;
        } else {
            // Get the primary display monitor handle
            final HMONITOR monitor = User32.INSTANCE.MonitorFromPoint(new POINT.ByValue(0, 0), User32.MONITOR_DEFAULTTOPRIMARY);

            final MONITORINFO monitorInfo = new MONITORINFO();
            User32.INSTANCE.GetMonitorInfo(monitor, monitorInfo); // TODO: Look into this method for future patches

            return monitorInfo.rcWork.toRectangle();
        }
    }

    @Override
    public Area getWorkArea() {
        return workArea;
    }

    @Override
    public Area getActiveWindow() {
        return activeWindow;
    }

    @Override
    public String getActiveWindowTitle() {
        return WindowUtils.getWindowTitle(activeWindowHandle);
    }

    @Override
    public long getActiveWindowId() {
        return activeWindowHandle == null ? 0 : activeWindowHandle.hashCode();
    }

    @Override
    public void moveActiveWindow(final Point point) {
        if (activeWindowHandle == null) {
            return;
        }

        double dpiScale = Toolkit.getDefaultToolkit().getScreenResolution() / 96.0;
        if (dpiScale != 1) {
            point.x = (int) Math.round(point.x * dpiScale);
            point.y = (int) Math.round(point.y * dpiScale);
        }

        User32.INSTANCE.MoveWindow(activeWindowHandle, point.x, point.y, activeWindowDpiUnaware.getWidth(),
                activeWindowDpiUnaware.getHeight(), true);
    }

    @Override
    public void restoreWindows() {
        User32.INSTANCE.EnumWindows(new WNDENUMPROC() {
            int offset = 25;
            boolean firstCallback = true;

            @Override
            public boolean callback(HWND hWnd, Pointer data) {
                WindowStatus result = getWindowStatus(hWnd);
                if (result == WindowStatus.OUT_OF_BOUNDS) {
                    // Valid interactive window found

                    // Get the work area rectangle
                    final Rectangle workArea = getWorkAreaRect(false);
                    // Get window rectangle
                    final Rectangle rect;
                    try {
                        rect = WindowUtils.getWindowLocationAndSize(hWnd);
                    } catch (Win32Exception e) {
                        if (e.getHR().intValue() != WinError.E_HANDLE) {
                            // The exception was not due to the window handle being invalid, so rethrow the exception
                            throw e;
                        }
                        return true;
                    }

                    double dpiScaleInverse = 96.0 / Toolkit.getDefaultToolkit().getScreenResolution();
                    if (firstCallback) {
                        if (dpiScaleInverse != 1) {
                            offset = (int) Math.round(offset * dpiScaleInverse);
                        }
                        firstCallback = false;
                    }
                    // Move the window to be on-screen
                    rect.setLocation(workArea.x + offset, workArea.y + offset);
                    User32.INSTANCE.MoveWindow(hWnd, rect.x, rect.y, rect.width, rect.height, true);
                    User32.INSTANCE.BringWindowToTop(hWnd);

                    if (dpiScaleInverse == 1) {
                        offset += 25;
                    } else {
                        offset = (int) Math.round(offset + 25 * dpiScaleInverse);
                    }
                }

                return true;
            }
        }, null);
    }

    @Override
    public void refreshCache() {
        interactiveCache.clear(); // Will be repopulated in the next isInteractive() call
        windowTitles = null;
        windowTitlesBlacklist = null;
    }

    @Override
    public void dispose() {
    }
}
