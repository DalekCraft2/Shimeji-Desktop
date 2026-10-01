/*
 * Created by asdfman
 * https://github.com/asdfman/linux-shimeji
 */
package com.group_finity.mascot.platform.x11;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.environment.AbstractEnvironment;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.platform.x11.X.Display;
import com.group_finity.mascot.platform.x11.X.Window;
import com.group_finity.mascot.platform.x11.X.X11Exception;
import com.sun.jna.platform.unix.X11;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * An implementation of {@link AbstractEnvironment} that provides access to the X11 native environment via JNA.
 *
 * @author asdfman
 */
class X11Environment extends AbstractEnvironment {

    /**
     * The {@link X} display.
     */
    private final Display display = new Display();

    /**
     * Maps a window title to a boolean representing whether the title is valid.
     * Windows with invalid titles cannot be interactive.
     * This is cleared whenever {@link #refreshCache()} is invoked.
     *
     * @see #hasValidTitle(Window)
     * @see #refreshCache()
     */
    private final HashMap<String, Boolean> validTitleCache = new LinkedHashMap<>();

    /**
     * The area of the active window.
     * If there is currently no active window, this area's position will be set to {@code (-1, -1)},
     * and its dimensions will be set to {@code (0, 0)}.
     *
     * @see #activeWindowTitle
     * @see #activeWindowObject
     * @see #getActiveWindow()
     */
    private final Area activeWindow = new Area();

    /**
     * The title of the active window.
     * If there is currently no active window, this value will be an empty string.
     *
     * @see #activeWindow
     * @see #activeWindowObject
     * @see #getActiveWindowTitle()
     */
    private String activeWindowTitle = "";

    private Window activeWindowObject = null;

    /**
     * An array containing all whitelisted window titles.
     * <p>
     * If a window title contains any of the strings in this array, the window title may be valid.
     * However, because the {@linkplain #windowTitlesBlacklist window title blacklist} takes priority over
     * the whitelist, it is not guaranteed that the window title will be valid if it contains a string in this array.
     * <p>
     * If this array is empty, a window title can still be valid if the blacklist is not empty and does not contain
     * any substrings of the window title.
     * <p>
     * This is initialized in {@link #hasValidTitle(Window)} using the contents of the
     * {@code interactiveWindows} setting, and set to {@code null} in {@link #refreshCache()}.
     *
     * @see #windowTitlesBlacklist
     * @see #hasValidTitle(Window)
     * @see #refreshCache()
     */
    private String[] windowTitles = null;

    /**
     * An array containing all blacklisted window titles.
     * <p>
     * If a window title contains any of the strings in this array, the window title is invalid.
     * <p>
     * This is initialized in {@link #hasValidTitle(Window)} using the contents of the
     * {@code interactiveWindowsBlacklist} setting, and set to {@code null} in {@link #refreshCache()}.
     *
     * @see #windowTitles
     * @see #hasValidTitle(Window)
     * @see #refreshCache()
     */
    private String[] windowTitlesBlacklist = null;

    /**
     * Storage for values of certain state/type atoms on the current display.
     */
    private final Collection<Number> badStates;
    private final Collection<Number> badTypes;
    private final int maximizedVertValue;
    private final int maximizedHorzValue;
    private final int minimizedValue;
    private final int fullscreenValue;
    private final int dockValue;

    /**
     * Enumeration of the possible return statuses from {@link #getWindowStatus(Window)}.
     * Each status specifies whether a given window is interactive, whether it prevents
     * other windows from being interactive, and whether it intersects with the bounds of the screen.
     *
     * @author LavenderSnek
     * @see #getWindowStatus(Window)
     */
    private enum WindowStatus {
        /** The window is interactive and prevents other windows from being interactive. */
        INTERACTIVE,
        /** The window is not interactive, and prevents interaction with any windows that are layered beneath it. */
        OBSTRUCTIVE,
        /** The window is not interactive but does not prevent other windows from being interactive. */
        IGNORED,
        /**
         * The window meets the criteria to be interactive, but it is out of bounds and should be ignored.
         * It does not prevent other windows from being interactive.
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
        badStates = Set.of(
                minimizedValue,
                display.getAtom("_NET_WM_STATE_MODAL").intValue(),
                display.getAtom("_NET_WM_STATE_ABOVE").intValue());

        dockValue = display.getAtom("_NET_WM_WINDOW_TYPE_DOCK").intValue();
        badTypes = Set.of(
                dockValue,
                display.getAtom("_NET_WM_WINDOW_TYPE_DESKTOP").intValue(),
                display.getAtom("_NET_WM_WINDOW_TYPE_MENU").intValue(),
                display.getAtom("_NET_WM_WINDOW_TYPE_SPLASH").intValue(),
                display.getAtom("_NET_WM_WINDOW_TYPE_DIALOG").intValue());
    }

    @Override
    public void tick() {
        super.tick();

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

    /**
     * Checks whether the title of the specified window is valid. For a window to be interactive, it must
     * have a valid title.
     * <p>
     * A window's title is valid if it contains none of the entries from the {@code interactiveWindowsBlacklist}
     * setting and contains at least one entry from the {@code interactiveWindows} setting. If the
     * {@code interactiveWindows} list is empty, the title can still be valid if the {@code interactiveWindowsBlacklist}
     * list is not empty and does not contain any substrings of the window title.
     *
     * @param window the window whose title will be checked
     * @return {@code true} if the title of the specified window is valid; {@code false} otherwise
     */
    private boolean hasValidTitle(final Window window) {
        final String windowTitle = getWindowTitle(window);

        final Boolean cachedValue = validTitleCache.get(windowTitle);
        if (cachedValue != null) {
            return cachedValue;
        }

        // Optimization to remove empty window titles from consideration without the loop.
        if (windowTitle.isEmpty()) {
            validTitleCache.put(windowTitle, false);
            return false;
        }

        // blacklist takes precedence over whitelist
        if (windowTitlesBlacklist == null) {
            List<String> blacklist = Main.getInstance().getSettings().interactiveWindowsBlacklist;
            if (blacklist.isEmpty()) {
                windowTitlesBlacklist = Main.EMPTY_STRING_ARRAY;
            } else {
                // Filter out empty titles in advance so we don't have to check for them in the for-loop below
                windowTitlesBlacklist = blacklist.stream().filter(item -> !item.trim().isEmpty())
                        .toArray(String[]::new);
            }
        }
        boolean blacklistIsEmpty = windowTitlesBlacklist.length == 0;
        // If the window title contains any of the entries in the blacklist, the window title is invalid
        for (String title : windowTitlesBlacklist) {
            if (windowTitle.contains(title)) {
                validTitleCache.put(windowTitle, false);
                return false;
            }
        }

        // whitelist
        if (windowTitles == null) {
            List<String> whitelist = Main.getInstance().getSettings().interactiveWindows;
            if (whitelist.isEmpty()) {
                windowTitles = Main.EMPTY_STRING_ARRAY;
            } else {
                // Filter out empty titles in advance so we don't have to check for them in the for-loop below
                windowTitles = whitelist.stream().filter(item -> !item.trim().isEmpty())
                        .toArray(String[]::new);
            }
        }
        boolean whitelistIsEmpty = windowTitles.length == 0;
        // If the window title contains any of the entries in the whitelist, the window title is valid
        for (String title : windowTitles) {
            if (windowTitle.contains(title)) {
                validTitleCache.put(windowTitle, true);
                return true;
            }
        }

        if (whitelistIsEmpty && !blacklistIsEmpty) {
            // If the whitelist is empty and the blacklist is not,
            // the window title is valid by default
            validTitleCache.put(windowTitle, true);
            return true;
        } else {
            // Otherwise, the window title is invalid by default
            validTitleCache.put(windowTitle, false);
            return false;
        }
    }

    /**
     * Gets a {@link WindowStatus} representing whether the specified window is interactive, whether it
     * prevents other windows from being interactive, and whether it intersects with the bounds of the screen.
     *
     * @param window the window whose status will be returned
     * @return the status of the specified window
     * @see WindowStatus
     */
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
        boolean goodDesktop = desktop != null && desktop.equals(curDesktop);
        if (goodDesktop && isStateGood(state) && isTypeGood(type)) {
            if (state.contains(maximizedVertValue) && state.contains(maximizedHorzValue)) {
                // Window is maximized and prevents the windows beneath it from being interactive
                return WindowStatus.OBSTRUCTIVE;
            }

            /*
             * TODO: Find some X11 atom that is dedicated to a window being minimized,
             *  because _NET_WM_STATE_HIDDEN is used for both invisible windows and minimized windows
             */
            if (hasValidTitle(window) && !state.contains(minimizedValue)) {
                Rectangle windowRect = getWindowBounds(window);
                /*
                 * TODO: Some Linux window managers don't seem to allow windows to be moved off screen, so this check
                 *  always passes on those systems, making it impossible for a window to have the OUT_OF_BOUNDS status.
                 *  We should instead figure out how close to the edge of the screen the windows are allowed to be,
                 *  and then check for windows that are that close to the edge.
                 */
                if (getScreen().intersects(windowRect)) {
                    // Window is interactive
                    return WindowStatus.INTERACTIVE;
                } else {
                    // Window is out of bounds and will be ignored
                    return WindowStatus.OUT_OF_BOUNDS;
                }
            }
        }

        // Window is ignored
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
                case INTERACTIVE:
                    activeWindowObject = window;
                    break loop;

                case IGNORED, OUT_OF_BOUNDS:
                    continue;

                case OBSTRUCTIVE: // The window blocks all windows beneath it, so abort the search here
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

    private boolean isStateGood(Collection<Integer> state) {
        if (state == null || state.isEmpty()) {
            return false;
        }
        return state.stream().noneMatch(badStates::contains);
    }

    private boolean isTypeGood(Collection<Integer> type) {
        if (type == null || type.isEmpty()) {
            return false;
        }
        return type.stream().noneMatch(badTypes::contains);
    }

    /**
     * Gets the bounds of the work area. This area is the display area excluding the taskbar.
     *
     * @return the bounds of the work area
     */
    private Rectangle getWorkAreaRect() {
        GraphicsConfiguration config = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle rect = config.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
        rect.x += insets.left;
        rect.y += insets.top;
        rect.width -= insets.left + insets.right;
        rect.height -= insets.top + insets.bottom;
        return rect;
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
                // Out-of-bounds interactive window found

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
        validTitleCache.clear(); // Will be repopulated in the next hasValidTitle() call
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
