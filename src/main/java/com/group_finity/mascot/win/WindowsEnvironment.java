package com.group_finity.mascot.win;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.win.jna.Dwmapi;
import com.sun.jna.Pointer;
import com.sun.jna.platform.WindowUtils;
import com.sun.jna.platform.win32.User32;
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
import java.util.logging.Logger;

/**
 * Uses JNI to obtain environment information that is difficult to obtain with Java.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
// FIXME This environment feels slower than it used to be whenever a lot of Shimejis are moving on-screen.
class WindowsEnvironment extends Environment {
    private static final HashMap<HWND, Boolean> ieCache = new LinkedHashMap<>();

    public static Area workArea = new Area();

    public static Area activeIeDpiUnaware = new Area();
    public static Area activeIe = new Area();

    private static HWND activeIeObject = null;

    private static String[] windowTitles = null;

    private enum IeStatus {
        /** The IE is valid. */
        VALID,
        /** The IE is invalid and blocks any other valid IEs. */
        INVALID,
        /** The IE is invalid but does not prevent other IEs from being valid. */
        IGNORED,
        /** The IE is out of bounds and does not prevent other IEs from being valid. */
        OUT_OF_BOUNDS
    }

    private static final Logger log = Logger.getLogger(WindowsEnvironment.class.getName());

    private static boolean isIE(final HWND hWnd) {
        final Boolean cachedValue = ieCache.get(hWnd);
        if (cachedValue != null) {
            return cachedValue;
        }

        // Determine whether it is IE by the window title
        final String ieTitle = WindowUtils.getWindowTitle(hWnd);

        // optimisation to remove empty windows from consideration without the loop.
        // Program Manager hard coded exception as there's issues if we mess with it
        if (ieTitle.isEmpty() || ieTitle.equals("Program Manager")) {
            ieCache.put(hWnd, false);
            return false;
        }

        if (windowTitles == null) {
            windowTitles = Main.getInstance().getProperties().getProperty("InteractiveWindows", "").split("/");
        }

        for (String windowTitle : windowTitles) {
            if (!windowTitle.trim().isEmpty() && ieTitle.contains(windowTitle)) {
                // Window is IE
                ieCache.put(hWnd, true);
                return true;
            }
        }

        // Window is not IE
        ieCache.put(hWnd, false);
        return false;
    }

    private static IeStatus getIeStatus(HWND hWnd) {
        if (User32.INSTANCE.IsWindowVisible(hWnd)) {
            // metro apps can be closed or minimised and still be considered "visible" by User32
            // have to consider the new cloaked variable instead
            LongByReference flagsRef = new LongByReference();
            HRESULT result = Dwmapi.INSTANCE.DwmGetWindowAttribute(hWnd, Dwmapi.DWMWA_CLOAKED, flagsRef.getPointer(), 8);
            if (result.equals(WinError.S_OK) && flagsRef.getValue() != 0) // unsupported on 7 so skip the check
            {
                return IeStatus.IGNORED;
            }

            int flags = WindowsUtil.GetWindowLong(hWnd, User32.GWL_STYLE).intValue();

            if ((flags & User32.WS_MAXIMIZE) != 0) {
                // Aborted because a maximized window was found
                return IeStatus.INVALID;
            }

            if (isIE(hWnd) && (flags & User32.WS_MINIMIZE) == 0) {
                // IE found
                Rectangle ieRect = getIERect(hWnd, true);
                if (ieRect.intersects(getScreenRect())) {
                    return IeStatus.VALID;
                } else {
                    return IeStatus.OUT_OF_BOUNDS;
                }
            }
        }

        // Not found
        return IeStatus.IGNORED;
    }

    private static HWND findActiveIE() {
        activeIeObject = null;

        User32.INSTANCE.EnumWindows((hWnd, data) -> {
            switch (getIeStatus(hWnd)) {
                case VALID:
                    activeIeObject = hWnd;
                    return false;

                case OUT_OF_BOUNDS:
                case IGNORED: // Valid window but not interactive according to user settings
                    return true;

                case INVALID: // Something invalid is the foreground object
                default:
                    activeIeObject = null;
                    return false;
            }
        }, null);

        return activeIeObject;
    }

    /**
     * Gets the given window's area.
     *
     * @return the window's area
     */
    private static Rectangle getIERect(HWND ie, boolean dpiAware) {
        if (ie == null) {
            return new Rectangle();
        }
        // Get and return IE rectangle
        final Rectangle rect = WindowUtils.getWindowLocationAndSize(ie);
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

    private static boolean moveIE(final HWND ie, final Point point, final Area area) {
        if (ie == null) {
            return false;
        }

        double dpiScale = Toolkit.getDefaultToolkit().getScreenResolution() / 96.0;
        if (dpiScale != 1) {
            point.x = (int) Math.round(point.x * dpiScale);
            point.y = (int) Math.round(point.y * dpiScale);
        }

        User32.INSTANCE.MoveWindow(ie, point.x, point.y, area.getWidth(),
                area.getHeight(), true);

        return true;
    }

    private static void restoreAllIEs() {
        User32.INSTANCE.EnumWindows(new WNDENUMPROC() {
            int offset = 25;
            boolean firstCallback = true;

            @Override
            public boolean callback(HWND hWnd, Pointer data) {
                IeStatus result = getIeStatus(hWnd);
                if (result == IeStatus.OUT_OF_BOUNDS) {
                    // IE found

                    // Get the work area rectangle
                    final Rectangle workArea = getWorkAreaRect(false);
                    // Get IE rectangle
                    final Rectangle rect = WindowUtils.getWindowLocationAndSize(hWnd);

                    double dpiScaleInverse = 96.0 / Toolkit.getDefaultToolkit().getScreenResolution();
                    if (firstCallback && dpiScaleInverse != 1) {
                        offset = (int) Math.round(offset * dpiScaleInverse);
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
    public void tick() {
        super.tick();
        workArea.set(getWorkAreaRect(true));
        final Rectangle ieRectDpiUnaware = getIERect(findActiveIE(), false);
        // Calculate the DPI-aware rectangle here to avoid calling getIERect() a second time
        final Rectangle ieRect = new Rectangle(ieRectDpiUnaware);
        double dpiScaleInverse = 96.0 / Toolkit.getDefaultToolkit().getScreenResolution();
        if (dpiScaleInverse != 1) {
            ieRect.x = (int) Math.round(ieRect.x * dpiScaleInverse);
            ieRect.y = (int) Math.round(ieRect.y * dpiScaleInverse);
            ieRect.width = (int) Math.round(ieRect.width * dpiScaleInverse);
            ieRect.height = (int) Math.round(ieRect.height * dpiScaleInverse);
        }
        activeIe.setVisible(ieRect.intersects(getScreen().toRectangle()));
        activeIe.set(ieRect);
        activeIeDpiUnaware.set(ieRectDpiUnaware);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void moveActiveIE(final Point point) {
        moveIE(findActiveIE(), point, activeIeDpiUnaware);
    }

    @Override
    public void restoreIE() {
        restoreAllIEs();
    }

    @Override
    public Area getWorkArea() {
        return workArea;
    }

    @Override
    public Area getActiveIE() {
        return activeIe;
    }

    @Override
    public String getActiveIETitle() {
        return WindowUtils.getWindowTitle(findActiveIE());
    }

    @Override
    public long getActiveWindowId() {
        return activeIeObject == null ? 0 : activeIeObject.hashCode();
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
            User32.INSTANCE.GetMonitorInfo(monitor, monitorInfo); // TODO Look into this method for future patches

            return monitorInfo.rcWork.toRectangle();
        }
    }

    @Override
    public void refreshCache() {
        ieCache.clear(); // will be repopulated next isIE call
        windowTitles = null;
    }
}
