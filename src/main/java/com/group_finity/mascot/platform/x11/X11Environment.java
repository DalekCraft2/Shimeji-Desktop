/*
 * Created by asdfman
 * https://github.com/asdfman/linux-shimeji
 */
package com.group_finity.mascot.platform.x11;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.ComplexArea;
import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.platform.x11.X.Display;
import com.group_finity.mascot.platform.x11.X.Window;
import com.group_finity.mascot.platform.x11.X.X11Exception;
import com.sun.jna.platform.unix.X11;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Uses JNI to obtain environment information that is difficult to obtain with Java.
 *
 * @author asdfman
 */
class X11Environment extends Environment {

    /**
     * The {@link X} display.
     */
    private final Display display = new Display();

    private final HashMap<Window, Boolean> ieCache = new LinkedHashMap<>();

    /**
     * Window for jump action targeting.
     */
    public Area activeIe = new Area();

    private Window activeIeObject = null;

    /**
     * Current screen. Never changes after initial assignment.
     * {@link Environment} and {@link ComplexArea} handle detection
     * and dual monitor behavior.
     */
    public static final Area workArea = new Area();

    private String[] windowTitles = null;
    private String[] windowTitlesBlacklist = null;

    /**
     * Storage for values of certain state/type atoms on the current display.
     */
    private final Collection<Number> badStateList = new ArrayList<>();
    private final Collection<Number> badTypeList = new ArrayList<>();
    private final int maximizedVertValue;
    private final int maximizedHorzValue;
    private final int minimizedValue;
    private final int fullscreenValue;
    private final int dockValue;

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

    /**
     * Initializes a new {@code X11Environment}.
     */
    X11Environment() {
        maximizedVertValue = display.getAtom("_NET_WM_STATE_MAXIMIZED_VERT").intValue();
        maximizedHorzValue = display.getAtom("_NET_WM_STATE_MAXIMIZED_HORZ").intValue();
        minimizedValue = display.getAtom("_NET_WM_STATE_HIDDEN").intValue();
        fullscreenValue = display.getAtom("_NET_WM_STATE_FULLSCREEN").intValue();
        badStateList.add(minimizedValue);
        badStateList.add(display.getAtom("_NET_WM_STATE_MODAL").intValue());
        badStateList.add(display.getAtom("_NET_WM_STATE_ABOVE").intValue());

        dockValue = display.getAtom("_NET_WM_WINDOW_TYPE_DOCK").intValue();
        badTypeList.add(dockValue);
        badTypeList.add(display.getAtom("_NET_WM_WINDOW_TYPE_DESKTOP").intValue());
        badTypeList.add(display.getAtom("_NET_WM_WINDOW_TYPE_MENU").intValue());
        badTypeList.add(display.getAtom("_NET_WM_WINDOW_TYPE_SPLASH").intValue());
        badTypeList.add(display.getAtom("_NET_WM_WINDOW_TYPE_DIALOG").intValue());
    }

    @Override
    public void tick() {
        super.tick();

        workArea.set(getWorkAreaRect());
        final Rectangle ieRect = getWindowBounds(findActiveIE());
        activeIe.setVisible(ieRect.intersects(getScreenRect()));
        activeIe.set(ieRect);
    }

    private boolean isIE(final Window window) {
        final Boolean cachedValue = ieCache.get(window);
        if (cachedValue != null) {
            return cachedValue;
        }

        // Determine whether it is IE by the window title
        final String ieTitle = getWindowTitle(window);

        // optimisation to remove empty windows from consideration without the loop.
        if (ieTitle.isEmpty()) {
            ieCache.put(window, false);
            return false;
        }

        // blacklist takes precedence over whitelist
        boolean blacklistInUse = false;
        if (windowTitlesBlacklist == null) {
            windowTitlesBlacklist = Main.getInstance().getProperties().getProperty("InteractiveWindowsBlacklist", "").split("/");
        }
        for (String windowTitle : windowTitlesBlacklist) {
            if (!windowTitle.trim().isEmpty()) {
                blacklistInUse = true;
                if (ieTitle.contains(windowTitle)) {
                    ieCache.put(window, false);
                    return false;
                }
            }
        }

        // whitelist
        boolean whitelistInUse = false;
        if (windowTitles == null) {
            windowTitles = Main.getInstance().getProperties().getProperty("InteractiveWindows", "").split("/");
        }
        for (String windowTitle : windowTitles) {
            if (!windowTitle.trim().isEmpty()) {
                // Window is IE
                whitelistInUse = true;
                if (ieTitle.contains(windowTitle)) {
                    ieCache.put(window, true);
                    return true;
                }
            }
        }

        if (whitelistInUse || !blacklistInUse) {
            // Window is not IE
            ieCache.put(window, false);
            return false;
        } else {
            // Window is IE
            ieCache.put(window, true);
            return true;
        }
    }

    private IeStatus getIeStatus(Window window) {
        int curDesktop;
        int desktop;
        List<Integer> state;
        List<Integer> type;
        try {
            curDesktop = display.getActiveDesktopNumber();
            desktop = window.getDesktop();
            state = Arrays.asList(window.getState());
            type = Arrays.asList(window.getType());
        } catch (X11Exception e) {
            /*
            BUG: Because X11 window managers remove windows' desktop ID and state properties whenever those windows are
            not focused, this method returns IeStatus.IGNORED for most windows other than the currently focused one.

            I don't think I can do anything about that. Sorry!
             */
            return IeStatus.IGNORED;
        }
        boolean badDesktop = desktop != curDesktop && desktop != -1;
        // System.out.println("ID: " + window.getID() + "; Title: " + getWindowTitle(window) + "; State: " + state + " (" + X11.INSTANCE.XGetAtomName(display.getX11Display(), new X11.Atom(state)) + ")" + "; Type: " + type);
        if (!badDesktop && !checkState(state) && !checkType(type)) {
            // metro apps can be closed or minimised and still be considered "visible" by User32
            // have to consider the new cloaked variable instead
            // LongByReference flagsRef = new LongByReference();
            // WinNT.HRESULT result = Dwmapi.INSTANCE.DwmGetWindowAttribute(window, Dwmapi.DWMWA_CLOAKED, flagsRef.getPointer(), 8);
            // if (result.equals(WinError.S_OK) && flagsRef.getValue() != 0) // unsupported on 7 so skip the check
            // {
            //     return IeStatus.IGNORED;
            // }

            // int flags = WindowsUtil.GetWindowLong(window, User32.GWL_STYLE).intValue();

            if (/* (flags & User32.WS_MAXIMIZE) != 0 */ state.contains(maximizedVertValue) && state.contains(maximizedHorzValue)) {
                // Aborted because a maximized window was found
                return IeStatus.INVALID;
            }

            /*
             * TODO: Find some X11 atom that is dedicated to a window being minimized,
             *  because _NET_WM_STATE_HIDDEN is used for both invisible windows and minimized windows
             */
            if (isIE(window) && /* (flags & User32.WS_MINIMIZE) == 0 */ !state.contains(minimizedValue)) {
                // IE found
                Rectangle ieRect = getWindowBounds(window);
                /*
                 * TODO: Some Linux window managers don't seem to allow windows to be moved off screen, so this check
                 *  always passes on those systems, making it impossible for a window to have the OUT_OF_BOUNDS status.
                 *  We should instead figure out how close to the edge of the screen the windows are allowed to be,
                 *  and then check for windows that are that close to the edge.
                 */
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

    private Window findActiveIE() {
        activeIeObject = null;

        // Retrieve all windows from the X Display
        Window[] allWindows;
        try {
            // allWindows = display.getWindows();
            // allWindows = display.getRootWindow().getSubwindows();
            // allWindows = display.getRootWindow().getAllSubwindows();

            // Because this support is so badly optimized, we will only check the currently focused window for now.
            allWindows = new Window[]{display.getActiveWindow()};
        } catch (X11Exception e) {
            return null;
        }

        loop:
        for (Window window : allWindows) {
            switch (getIeStatus(window)) {
                case VALID:
                    activeIeObject = window;
                    break loop;

                case OUT_OF_BOUNDS:
                case IGNORED: // Valid window but not interactive according to user settings
                    continue;

                case INVALID: // Something invalid is the foreground object
                default:
                    activeIeObject = null;
                    break loop;
            }
        }

        return activeIeObject;
    }

    /**
     * Gets the given window's bounds.
     *
     * @return the window's bounds
     */
    private static Rectangle getWindowBounds(Window window) {
        if (window == null) {
            return new Rectangle();
        }
        return window.getBounds();
    }

    /**
     * Gets the given window's title.
     *
     * @return the window's title
     */
    private static String getWindowTitle(Window window) {
        if (window == null) {
            return "";
        }
        String title;
        try {
            title = window.getID() == 0 ? "" : window.getTitle();
        } catch (X11Exception e) {
            title = "";
        }
        return title;
    }

    private boolean checkState(Collection<Integer> state) {
        if (state == null || state.isEmpty()) {
            return true;
        }
        return state.stream().anyMatch(badStateList::contains);
    }

    private boolean checkType(Collection<Integer> type) {
        if (type == null || type.isEmpty()) {
            return true;
        }
        return type.stream().anyMatch(badTypeList::contains);
    }

    private Rectangle getWorkAreaRect() {
        GraphicsConfiguration config = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle rect = config.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
        rect.x += insets.left;
        rect.y += insets.top;
        rect.width -= insets.right;
        rect.height -= insets.bottom;
        return rect;
    }

    @Override
    public Area getActiveIE() {
        return activeIe;
    }

    @Override
    public String getActiveIETitle() {
        return getWindowTitle(activeIeObject);
    }

    @Override
    public long getActiveWindowId() {
        return activeIeObject == null ? 0 : activeIeObject.getID();
    }

    @Override
    public void moveActiveIE(Point point) {
        if (activeIeObject != null) {
            // FIXME: Mascots will often let go of a window very shortly after they pick it up, without throwing it
            X11.INSTANCE.XMoveWindow(display.getX11Display(), activeIeObject.getX11Window(), point.x, point.y);
        }
    }

    @Override
    public void restoreIE() {
        // Retrieve all windows from the X Display
        Window[] allWindows;
        try {
            // allWindows = display.getWindows();
            // allWindows = display.getRootWindow().getSubwindows();
            allWindows = display.getRootWindow().getAllSubwindows();
            // allWindows = new Window[]{display.getActiveWindow()};
        } catch (X11Exception e) {
            return;
        }

        int offset = 25;

        for (Window window : allWindows) {
            IeStatus result = getIeStatus(window);
            if (result == IeStatus.OUT_OF_BOUNDS) {
                // IE found

                // Get the work area rectangle
                final Rectangle workArea = getWorkAreaRect();
                // Get IE rectangle
                final Rectangle rect = getWindowBounds(window);

                // Move the window to be on-screen
                rect.setLocation(workArea.x + offset, workArea.y + offset);
                X11.INSTANCE.XMoveWindow(display.getX11Display(), window.getX11Window(), rect.x, rect.y);
                X11.INSTANCE.XRaiseWindow(display.getX11Display(), window.getX11Window());

                offset += 25;
            }
        }
    }

    @Override
    public void refreshCache() {
        ieCache.clear(); // will be repopulated next isIE call
        windowTitles = null;
        windowTitlesBlacklist = null;
    }

    @Override
    public void dispose() {
    }

    @Override
    public Area getWorkArea() {
        return workArea;
    }
}
