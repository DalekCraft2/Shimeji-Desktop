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
     * The {@linkplain X.Display X display} used to access the X11 environment.
     *
     * @see #getDisplay()
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

    /**
     * An {@linkplain X.Window X window} representing the native handle of the active window.
     * If there is currently no active window, this value will be {@code null}.
     *
     * @see #activeWindow
     * @see #activeWindowTitle
     * @see #getActiveWindowId()
     * @see #findActiveWindow()
     */
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
     * A collection of "bad" window state atoms. If any of these are present on a window,
     * that window is prevented from becoming the active window.
     *
     * @see #isStateGood(Collection)
     */
    private final Collection<Number> badStates;

    /**
     * A collection of "bad" window type atoms. If any of these are present on a window,
     * that window is prevented from becoming the active window.
     *
     * @see #isStateGood(Collection)
     */
    private final Collection<Number> badTypes;

    /** The atom representing the window state of being maximized vertically. */
    private final int maximizedVertValue;
    /** The atom representing the window state of being maximized horizontally. */
    private final int maximizedHorzValue;
    /** The atom representing the window state of being hidden. This applies to both minimized and invisible windows. */
    private final int minimizedValue;
    /** The atom representing the window state of being in fullscreen mode. */
    private final int fullscreenValue;

    /**
     * The atom representing the window type used for dock/panel features.
     *
     * @see #getDockValue()
     */
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
     * Gets a {@link WindowStatus WindowStatus} representing whether the specified window is interactive, whether it
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

    /**
     * Searches all top-level windows for one that meets the criteria to be the active window.
     * For a window to be the active window, its {@link WindowStatus WindowStatus} must
     * be {@link WindowStatus#INTERACTIVE INTERACTIVE}.
     *
     * @return an {@linkplain X.Window X window} representing the native handle of the new active window,
     * or {@code null} if no windows met the criteria to be the active window
     * @see #getWindowStatus(Window)
     */
    private Window findActiveWindow() {
        activeWindowObject = null;

        // Retrieve all windows from the X Display
        Window[] allWindows;
        try {
            allWindows = display.getWindowsLayerOrdered();
        } catch (X11Exception e) {
            return null;
        }

        loop:
        // Iterate in reverse because the array is ordered from bottommost window to topmost window
        for (int i = allWindows.length - 1; i >= 0; i--) {
            Window window = allWindows[i];
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
     * Gets the bounds of the specified window.
     *
     * @param window the window whose bounds are to be returned
     * @return the bounds of the window
     */
    private static Rectangle getWindowBounds(Window window) {
        if (window == null) {
            return null;
        }
        Rectangle rawBounds = window.getBounds();
        Integer[] extents;
        try {
            extents = window.getExtents();
        } catch (X11Exception e) {
            return rawBounds;
        }
        /* The window.getBounds() method returns the area of the window minus the extents. Using those bounds as-is
        prevents mascots from interacting with the title bar of the window, so we need to create a new rectangle that
        takes the window frame extents into consideration.

        XMoveWindow(), which is used to move the active window, interprets the X and Y coordinates passed to it as the
        top-left corner of the window with the extents taken into consideration. Taking the extents into consideration
        here prevents XMoveWindow() from making the active window jump downward suddenly when a mascot tries to move it,
        which previously caused the mascot to immediately let go of the window and fall. */
        return new Rectangle(rawBounds.x - extents[0], rawBounds.y - extents[2],
                rawBounds.width + extents[0] + extents[1], rawBounds.height + extents[2] + extents[3]);
    }

    /**
     * Gets the title of the specified window.
     *
     * @param window the window whose title is to be returned
     * @return the title of the window
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

    /**
     * Checks whether the specified collection of window states is good.
     * A collection of window states is good if it is not {@code null}, is not empty, and does not contain any
     * {@linkplain #badStates bad states}.
     * If a window's state is bad, it cannot be set as the active window.
     *
     * @param state the window state collection to check
     * @return {@code true} if the window state collection is good; {@code false} otherwise
     */
    private boolean isStateGood(Collection<Integer> state) {
        if (state == null || state.isEmpty()) {
            return false;
        }
        return state.stream().noneMatch(badStates::contains);
    }

    /**
     * Checks whether the specified collection of window types is good.
     * A collection of window types is good if it is not {@code null}, is not empty, and does not contain any
     * {@linkplain #badTypes bad types}.
     * If a window's type is bad, it cannot be set as the active window.
     *
     * @param type the window type collection to check
     * @return {@code true} if the window type collection is good; {@code false} otherwise
     */
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
            X11.INSTANCE.XMoveWindow(display.getX11Display(), activeWindowObject.getX11Window(), x, y);

            /* The XMoveWindow request does not take effect immediately, so we need to process all pending events so the
            window's position is where we want it to be.
            This fixes the issue where mascots sometimes think they are no longer holding onto the window when
            carrying it due to the XMoveWindow request not having been processed yet. */
            X11.INSTANCE.XSync(display.getX11Display(), false);
        }
    }

    @Override
    public void restoreWindows() {
        // Retrieve all windows from the X Display
        Window[] allWindows;
        try {
            allWindows = display.getWindowsLayerOrdered();
        } catch (X11Exception e) {
            return;
        }

        int offset = 25;

        // Iterate in reverse because the array is ordered from bottommost window to topmost window
        for (int i = allWindows.length - 1; i >= 0; i--) {
            Window window = allWindows[i];
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

    /**
     * Gets the {@linkplain X.Display X display} used to access the X11 environment.
     *
     * @return the X display
     */
    Display getDisplay() {
        return display;
    }

    /**
     * Gets the atom representing the window type used for dock/panel features.
     *
     * @return the atom representing the dock window type
     */
    int getDockValue() {
        return dockValue;
    }
}
