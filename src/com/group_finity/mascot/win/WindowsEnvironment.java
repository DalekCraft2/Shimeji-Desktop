package com.group_finity.mascot.win;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.win.jna.Dwmapi;
import com.group_finity.mascot.win.jna.GDI32Extra;
import com.group_finity.mascot.win.jna.User32Extra;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.platform.WindowUtils;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.LongByReference;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

/**
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
class WindowsEnvironment extends Environment {
    private static final HashMap<WinDef.HWND, Boolean> ieCache = new LinkedHashMap<>();

    public static Area workArea = new Area();

    public static Area activeIE = new Area();

    private static WinDef.HWND activeIEobject = null;

    private static String[] windowTitles = null;

    private enum IEResult {
        INVALID,
        NOT_IE,
        IE_OUT_OF_BOUNDS,
        IE
    }

    private static final Logger log = Logger.getLogger(WindowsEnvironment.class.getName());

    private static boolean isIE(final WinDef.HWND ie) {
        final Boolean cachedValue = ieCache.get(ie);
        if (cachedValue != null) {
            return cachedValue;
        }

        final String ieTitle = WindowUtils.getWindowTitle(ie);

        // optimisation to remove empty windows from consideration without the loop.
        // Program Manager hard coded exception as there's issues if we mess with it
        if (ieTitle.isEmpty() || ieTitle.equals("Program Manager")) {
            ieCache.put(ie, false);
            return false;
        }

        if (windowTitles == null) {
            windowTitles = Main.getInstance().getProperties().getProperty("InteractiveWindows", "").split("/");
        }

        for (String windowTitle : windowTitles) {
            if (!windowTitle.trim().isEmpty() && ieTitle.contains(windowTitle)) {
                // log.log(Level.INFO, "Value {0} is IE", ieTitle);
                ieCache.put(ie, true);
                return true;
            }
        }

        // log.log(Level.INFO, "Value {0} is not IE", ieTitle);
        ieCache.put(ie, false);
        return false;
    }

    private static IEResult isViableIE(WinDef.HWND ie) {
        if (User32.INSTANCE.IsWindowVisible(ie)) {
            // metro apps can be closed or minimised and still be considered "visible" by User32
            // have to consider the new cloaked variable instead
            LongByReference flagsRef = new LongByReference();
            NativeLong result = Dwmapi.INSTANCE.DwmGetWindowAttribute(ie, Dwmapi.DWMWA_CLOAKED, flagsRef, 8);
            if (result.longValue() != WinError.E_INVALIDARG && (result.longValue() != 0 || flagsRef.getValue() != 0)) // unsupported on 7 so skip the check
            {
                return IEResult.NOT_IE;
            }

            int flags = WindowsUtil.GetWindowLong(ie, User32.GWL_STYLE).intValue();

            if ((flags & User32.WS_MAXIMIZE) != 0) {
                return IEResult.INVALID;
            }

            if (isIE(ie) && (flags & User32.WS_MINIMIZE) == 0) {
                Rectangle ieRect = getIERect(ie);
                if (ieRect.intersects(getScreenRect())) {
                    return IEResult.IE;
                } else {
                    return IEResult.IE_OUT_OF_BOUNDS;
                }
            }
        }

        return IEResult.NOT_IE;
    }

    private static WinDef.HWND findActiveIE() {
        activeIEobject = null;

        User32.INSTANCE.EnumWindows((ie, data) -> {
            switch (isViableIE(ie)) {
                case IE:
                    activeIEobject = ie;
                    return false;

                case IE_OUT_OF_BOUNDS:
                case NOT_IE: // Valid window but not interactive according to user settings
                    return true;

                case INVALID: // Something invalid is the foreground object
                default:
                    activeIEobject = null;
                    return false;
            }
        }, null);

        return activeIEobject;

        /* WinDef.HWND ie = User32.INSTANCE.GetWindow(User32.INSTANCE.GetForegroundWindow(), new WinDef.DWORD(User32.GW_HWNDFIRST));
        boolean continueFlag = true;

        while (continueFlag && User32.INSTANCE.IsWindow(ie)) {
            switch (isViableIE(ie)) {
                case IE:
                    return ie;

                case IE_OUT_OF_BOUNDS:
                case NOT_IE: // Valid window but not interactive according to user settings
                    ie = User32.INSTANCE.GetWindow(ie, new WinDef.DWORD(User32.GW_HWNDNEXT));
                    break;

                case INVALID: // Something invalid is the foreground object
                    continueFlag = false;
                    break;
            }
        }

        return null; */
    }

    private static Rectangle getIERect(WinDef.HWND ie) {
        if (ie == null) {
            return new Rectangle();
        }
        final Rectangle out = WindowUtils.getWindowLocationAndSize(ie);
        Rectangle in;
        try {
            in = getWindowRgnBox(ie);
        } catch (Win32Exception e) {
            in = new Rectangle(out.width, out.height);
        }
        return new Rectangle(out.x + in.x, out.y + in.y, in.width, in.height);
    }

    private static Rectangle getWindowRgnBox(final WinDef.HWND window) {
        final WinDef.RECT rect = new WinDef.RECT();
        if (getWindowRgnBoxImpl(window, rect) == User32Extra.ERROR) {
            throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
        }
        return rect.toRectangle();
    }

    private static int getWindowRgnBoxImpl(final WinDef.HWND window, final WinDef.RECT rect) {
        WinDef.HRGN hRgn = GDI32.INSTANCE.CreateRectRgn(0, 0, 0, 0);
        try {
            if (User32Extra.INSTANCE.GetWindowRgn(window, hRgn) == User32Extra.ERROR) {
                return User32Extra.ERROR;
            }
            GDI32Extra.INSTANCE.GetRgnBox(hRgn, rect);
            return 1;
        } finally {
            GDI32.INSTANCE.DeleteObject(hRgn);
        }
    }

    private static boolean moveIE(final WinDef.HWND ie, final Rectangle rect) {
        if (ie == null) {
            return false;
        }

        final Rectangle out = WindowUtils.getWindowLocationAndSize(ie);
        Rectangle in;
        try {
            in = getWindowRgnBox(ie);
        } catch (Win32Exception e) {
            in = new Rectangle(out.width, out.height);
        }

        User32.INSTANCE.MoveWindow(ie, rect.x - in.x, rect.y - in.y, rect.width + out.width - in.width,
                rect.height + out.height - in.height, true);

        return true;
    }

    private static void restoreAllIEs() {
        User32.INSTANCE.EnumWindows(new User32.WNDENUMPROC() {
            int offset = 25;

            @Override
            public boolean callback(WinDef.HWND hWnd, Pointer data) {
                IEResult result = isViableIE(hWnd);
                if (result == IEResult.IE_OUT_OF_BOUNDS) {
                    final Rectangle workArea = getWorkAreaRect();
                    final Rectangle rect = WindowUtils.getWindowLocationAndSize(hWnd);

                    rect.add(workArea.x + offset - rect.x, workArea.y + offset - rect.y);
                    User32.INSTANCE.MoveWindow(hWnd, rect.x, rect.y, rect.width, rect.height, true);
                    User32.INSTANCE.BringWindowToTop(hWnd);

                    offset += 25;
                }

                return true;
            }
        }, null);
    }

    @Override
    public void tick() {
        super.tick();
        workArea.set(getWorkAreaRect());
        final Rectangle ieRect = getIERect(findActiveIE());
        activeIE.setVisible(ieRect.intersects(getScreen().toRectangle()));
        activeIE.set(ieRect);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void moveActiveIE(final Point point) {
        moveIE(findActiveIE(), new Rectangle(point.x, point.y, activeIE.getWidth(), activeIE.getHeight()));
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
        return activeIE;
    }

    @Override
    public String getActiveIETitle() {
        return WindowUtils.getWindowTitle(findActiveIE());
    }

    private static Rectangle getWorkAreaRect() {
        final WinDef.RECT rect = new WinDef.RECT();
        User32Extra.INSTANCE.SystemParametersInfoW(User32Extra.SPI_GETWORKAREA, 0, rect, 0);
        return rect.toRectangle();
    }

    @Override
    public void refreshCache() {
        ieCache.clear(); // will be repopulated next isIE call
        windowTitles = null;
    }

    /* private void dumpWindowInformation() {
        final StringBuilder text = new StringBuilder();
        User32.INSTANCE.EnumWindows((ie, data) -> {
            String ieTitle = WindowUtils.getWindowTitle(ie);

            text.append(ieTitle).append(" ").append(isViableIE(ie)).append(System.lineSeparator());
            return true;
        }, null);

        try (PrintWriter out = new PrintWriter("window-debug-information.txt")) {
            out.println(text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    } */
}
