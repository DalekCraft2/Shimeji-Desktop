/*
 * Created by asdfman
 * https://github.com/asdfman/linux-shimeji
 */
package com.group_finity.mascot.platform.x11;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.environment.AbstractEnvironment;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.ComplexArea;
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
class X11Environment extends AbstractEnvironment {

    /**
     * The {@link X} display.
     */
    private final Display display = new Display();

    private final HashMap<Window, Boolean> interactiveCache = new LinkedHashMap<>();

    /**
     * Window for jump action targeting.
     */
    private final Area activeWindow = new Area();

    private String activeWindowTitle = "";

    private Window activeWindowObject = null;

    /**
     * Current screen. Never changes after initial assignment.
     * {@link AbstractEnvironment} and {@link ComplexArea} handle detection
     * and dual monitor behavior.
     */
    private final Area workArea = new Area(false);

    private String[] windowTitles = null;
    private String[] windowTitlesBlacklist = null;

    /**
     * Storage for values of certain state/type atoms on the current display.
     */
    private final Collection<Number> badStateList;
    private final Collection<Number> badTypeList;
    private final int maximizedVertValue;
    private final int maximizedHorzValue;
    private final int minimizedValue;
    private final int fullscreenValue;
    private final int dockValue;

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

    /**
     * Initializes a new {@code X11Environment}.
     */
    X11Environment() {
        maximizedVertValue = display.getAtom("_NET_WM_STATE_MAXIMIZED_VERT").intValue();
        maximizedHorzValue = display.getAtom("_NET_WM_STATE_MAXIMIZED_HORZ").intValue();
        minimizedValue = display.getAtom("_NET_WM_STATE_HIDDEN").intValue();
        fullscreenValue = display.getAtom("_NET_WM_STATE_FULLSCREEN").intValue();
        badStateList = List.of(
                minimizedValue,
                display.getAtom("_NET_WM_STATE_MODAL").intValue(),
                display.getAtom("_NET_WM_STATE_ABOVE").intValue());

        dockValue = display.getAtom("_NET_WM_WINDOW_TYPE_DOCK").intValue();
        badTypeList = List.of(
                dockValue,
                display.getAtom("_NET_WM_WINDOW_TYPE_DESKTOP").intValue(),
                display.getAtom("_NET_WM_WINDOW_TYPE_MENU").intValue(),
                display.getAtom("_NET_WM_WINDOW_TYPE_SPLASH").intValue(),
                display.getAtom("_NET_WM_WINDOW_TYPE_DIALOG").intValue());
    }

    @Override
    public void tick() {
        super.tick();

        workArea.set(getWorkAreaRect());
        long prevWindowId = getActiveWindowId();
        final Rectangle windowRect = getWindowBounds(findActiveWindow());
        if (windowRect == null) {
            activeWindow.setRect(-1, -1, 0, 0);
        } else {
            activeWindow.set(windowRect);
        }
        activeWindow.setVisible(activeWindow.intersects(getScreen()));

        if (prevWindowId != getActiveWindowId()) {
            // If the active window has changed, reset the active window's deltas to 0
            activeWindow.resetDeltas();
        }

        activeWindowTitle = getWindowTitle(activeWindowObject);
    }

    private boolean isInteractive(final Window window) {
        final Boolean cachedValue = interactiveCache.get(window);
        if (cachedValue != null) {
            return cachedValue;
        }

        // Determine whether the window is interactive based on its title
        final String windowTitle = getWindowTitle(window);

        // optimisation to remove empty windows from consideration without the loop.
        if (windowTitle.isEmpty()) {
            interactiveCache.put(window, false);
            return false;
        }

        // blacklist takes precedence over whitelist
        boolean blacklistInUse = false;
        if (windowTitlesBlacklist == null) {
            windowTitlesBlacklist = Main.getInstance().getSettings().interactiveWindowsBlacklist.toArray(Main.EMPTY_STRING_ARRAY);
        }
        for (String title : windowTitlesBlacklist) {
            if (!title.trim().isEmpty()) {
                blacklistInUse = true;
                if (windowTitle.contains(title)) {
                    interactiveCache.put(window, false);
                    return false;
                }
            }
        }

        // whitelist
        boolean whitelistInUse = false;
        if (windowTitles == null) {
            windowTitles = Main.getInstance().getSettings().interactiveWindows.toArray(Main.EMPTY_STRING_ARRAY);
        }
        for (String title : windowTitles) {
            if (!title.trim().isEmpty()) {
                // Window is interactive
                whitelistInUse = true;
                if (windowTitle.contains(title)) {
                    interactiveCache.put(window, true);
                    return true;
                }
            }
        }

        if (whitelistInUse || !blacklistInUse) {
            // Window is not interactive
            interactiveCache.put(window, false);
            return false;
        } else {
            // Window is interactive
            interactiveCache.put(window, true);
            return true;
        }
    }

    private WindowStatus getWindowStatus(Window window) {
        Integer curDesktop;
        Integer desktop;
        List<Integer> state;
        List<Integer> type;
        try {
             /*
            NOTE: Because X11 window managers remove windows' desktop ID and state properties whenever those windows are
            not focused, this method has to return WindowStatus.IGNORED for most windows other than the currently focused one.
            This is because window.getDesktop() will return null in that case, so badDesktop will equal true later,
            causing the bulk of this method to be skipped.

            I don't think I can do anything about that. Sorry!
             */
            curDesktop = display.getActiveDesktopNumber();
            desktop = window.getDesktop();
            state = Arrays.asList(window.getState());
            type = Arrays.asList(window.getType());
        } catch (X11Exception e) {
            return WindowStatus.IGNORED;
        }
        // System.out.println("ID: " + window.getID() + "; Title: " + getWindowTitle(window) + "; State: " + state + "; Type: " + type);
        boolean badDesktop = desktop != null && !desktop.equals(curDesktop);
        if (!badDesktop && !checkState(state) && !checkType(type)) {
            if (state.contains(maximizedVertValue) && state.contains(maximizedHorzValue)) {
                // Window is maximized and is therefore invalid
                return WindowStatus.INVALID;
            }

            /*
             * TODO: Find some X11 atom that is dedicated to a window being minimized,
             *  because _NET_WM_STATE_HIDDEN is used for both invisible windows and minimized windows
             */
            if (isInteractive(window) && !state.contains(minimizedValue)) {
                // Window is valid
                Rectangle windowRect = getWindowBounds(window);
                /*
                 * TODO: Some Linux window managers don't seem to allow windows to be moved off screen, so this check
                 *  always passes on those systems, making it impossible for a window to have the OUT_OF_BOUNDS status.
                 *  We should instead figure out how close to the edge of the screen the windows are allowed to be,
                 *  and then check for windows that are that close to the edge.
                 */
                if (getScreen().intersects(windowRect)) {
                    return WindowStatus.VALID;
                } else {
                    // Window is out of bounds and will be ignored
                    return WindowStatus.OUT_OF_BOUNDS;
                }
            }
        }

        // Window is valid but not interactive according to user settings
        return WindowStatus.IGNORED;
    }

    private Window findActiveWindow() {
        activeWindowObject = null;

        // Retrieve all windows from the X Display
        Window[] allWindows;
        try {
            allWindows = display.getWindows();
        } catch (X11Exception e) {
            return null;
        }

        loop:
        for (Window window : allWindows) {
            switch (getWindowStatus(window)) {
                case VALID:
                    activeWindowObject = window;
                    break loop;

                case IGNORED, OUT_OF_BOUNDS:
                    continue;

                case INVALID: // The window is invalid, so abort the search here
                default:
                    activeWindowObject = null;
                    break loop;
            }
        }

        return activeWindowObject;
    }

    /**
     * Gets the given window's bounds.
     *
     * @return the window's bounds
     */
    private static Rectangle getWindowBounds(Window window) {
        if (window == null) {
            return null;
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
            title = window.getTitle();
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
    public Area getWorkArea() {
        return workArea;
    }

    @Override
    public Area getActiveWindow() {
        return activeWindow;
    }

    @Override
    public String getActiveWindowTitle() {
        return activeWindowTitle;
    }

    @Override
    public long getActiveWindowId() {
        return activeWindowObject == null ? 0 : activeWindowObject.getID();
    }

    @Override
    public void moveActiveWindow(int x, int y) {
        if (activeWindowObject != null) {
            // FIXME: Mascots will often let go of a window very shortly after they pick it up, without throwing it
            X11.INSTANCE.XMoveWindow(display.getX11Display(), activeWindowObject.getX11Window(), x, y);
        }
    }

    @Override
    public void restoreWindows() {
        // Retrieve all windows from the X Display
        Window[] allWindows;
        try {
            allWindows = display.getWindows();
        } catch (X11Exception e) {
            return;
        }

        int offset = 25;

        for (Window window : allWindows) {
            WindowStatus result = getWindowStatus(window);
            if (result == WindowStatus.OUT_OF_BOUNDS) {
                // Valid interactive window found

                // Get the work area rectangle
                final Rectangle workArea = getWorkAreaRect();
                // Get window rectangle
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
        interactiveCache.clear(); // Will be repopulated in the next isInteractive() call
        windowTitles = null;
        windowTitlesBlacklist = null;
    }

    @Override
    public void dispose() {
        super.dispose();
        display.close();
    }

    Display getDisplay() {
        return display;
    }

    int getDockValue() {
        return dockValue;
    }
}
