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
import java.util.List;

/**
 * An implementation of {@link AbstractEnvironment} that provides access to the Windows native environment via JNA.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
class WindowsEnvironment extends AbstractEnvironment {
    /**
     * Maps a window title to a boolean representing whether the title is valid.
     * Windows with invalid titles cannot be interactive.
     * This is cleared whenever {@link #refreshCache()} is invoked.
     *
     * @see #hasValidTitle(HWND)
     * @see #refreshCache()
     */
    private final HashMap<String, Boolean> validTitleCache = new LinkedHashMap<>();

    /**
     * The area of the active window.
     * If there is currently no active window, this area's position will be set to {@code (-1, -1)},
     * and its dimensions will be set to {@code (0, 0)}.
     *
     * @see #activeWindowTitle
     * @see #activeWindowHandle
     * @see #getActiveWindow()
     */
    private final Area activeWindow = new Area();

    /**
     * The title of the active window.
     * If there is currently no active window, this value will be an empty string.
     *
     * @see #activeWindow
     * @see #activeWindowHandle
     * @see #getActiveWindowTitle()
     */
    private String activeWindowTitle = "";

    /**
     * The native handle of the active window.
     * If there is currently no active window, this value will be {@code null}.
     *
     * @see #activeWindow
     * @see #activeWindowTitle
     * @see #getActiveWindowId()
     * @see #findActiveWindow()
     */
    private HWND activeWindowHandle = null;

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
     * This is initialized in {@link #hasValidTitle(HWND)} using the contents of the
     * {@code interactiveWindows} setting, and set to {@code null} in {@link #refreshCache()}.
     *
     * @see #windowTitlesBlacklist
     * @see #hasValidTitle(HWND)
     * @see #refreshCache()
     */
    private String[] windowTitles = null;

    /**
     * An array containing all blacklisted window titles.
     * <p>
     * If a window title contains any of the strings in this array, the window title is invalid.
     * <p>
     * This is initialized in {@link #hasValidTitle(HWND)} using the contents of the
     * {@code interactiveWindowsBlacklist} setting, and set to {@code null} in {@link #refreshCache()}.
     *
     * @see #windowTitles
     * @see #hasValidTitle(HWND)
     * @see #refreshCache()
     */
    private String[] windowTitlesBlacklist = null;

    /**
     * Enumeration of the possible return statuses from {@link #getWindowStatus(HWND)}.
     * Each status specifies whether a given window is interactive, whether it prevents
     * other windows from being interactive, and whether it intersects with the bounds of the screen.
     *
     * @author LavenderSnek
     * @see #getWindowStatus(HWND)
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

    @Override
    public void tick() {
        super.tick();

        long prevWindowId = getActiveWindowId();
        // Get DPI-unaware window rectangle
        final Rectangle windowRect = getWindowRect(findActiveWindow(), true);
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

        activeWindowTitle = WindowUtils.getWindowTitle(activeWindowHandle);
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
     * @param hWnd the window whose title will be checked
     * @return {@code true} if the title of the specified window is valid; {@code false} otherwise
     */
    private boolean hasValidTitle(final HWND hWnd) {
        final String windowTitle = WindowUtils.getWindowTitle(hWnd);

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
     * @param hWnd the window whose status will be returned
     * @return the status of the specified window
     * @see WindowStatus
     */
    private WindowStatus getWindowStatus(HWND hWnd) {
        if (User32.INSTANCE.IsWindowVisible(hWnd)) {
            // DWMWA_CLOAKED is not supported on Windows 7 and earlier, so check that we are on at least Windows 8
            if (VersionHelpers.IsWindows8OrGreater()) {
                // metro apps can be closed or minimised and still be considered "visible" by User32
                // have to consider the cloaked variable instead
                LongByReference flagsRef = new LongByReference();
                HRESULT result = Dwmapi.INSTANCE.DwmGetWindowAttribute(hWnd, Dwmapi.DWMWA_CLOAKED, flagsRef.getPointer(), 8);
                if (result.equals(WinError.S_OK) && flagsRef.getValue() != 0) {
                    return WindowStatus.IGNORED;
                }
            }

            if (User32Extra.INSTANCE.IsZoomed(hWnd)) {
                // Window is maximized and prevents the windows beneath it from being interactive
                return WindowStatus.OBSTRUCTIVE;
            }

            if (hasValidTitle(hWnd) && !User32Extra.INSTANCE.IsIconic(hWnd)) {
                Rectangle windowRect = getWindowRect(hWnd, true);
                if (windowRect != null && getScreen().intersects(windowRect)) {
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
     * @return the native handle of the new active window, or {@code null} if no windows met the criteria
     * to be the active window
     * @see #getWindowStatus(HWND)
     */
    private HWND findActiveWindow() {
        activeWindowHandle = null;

        User32.INSTANCE.EnumWindows((hWnd, data) -> switch (getWindowStatus(hWnd)) {
            case INTERACTIVE -> {
                activeWindowHandle = hWnd;
                yield false;
            }
            case IGNORED, OUT_OF_BOUNDS -> true;
            default -> { // The window blocks all windows beneath it, so abort the search here
                activeWindowHandle = null;
                yield false;
            }
        }, null);

        return activeWindowHandle;
    }

    /**
     * Gets the bounds of the specified window.
     *
     * @param hWnd the window whose bounds are to be returned
     * @param dpiAware {@code true} if the bounds should be scaled to match the DPI of the graphics environment;
     * {@code false} if the bounds should not be scaled
     * @return the bounds of the window
     */
    private static Rectangle getWindowRect(HWND hWnd, boolean dpiAware) {
        if (hWnd == null) {
            return null;
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
            return null;
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
     * Gets the bounds of the work area. This area is the display area excluding the taskbar.
     *
     * @param dpiAware {@code true} if the bounds should be scaled to match the DPI of the graphics environment;
     * {@code false} if the bounds should not be scaled
     * @return the bounds of the work area
     */
    private static Rectangle getWorkAreaRect(boolean dpiAware) {
        if (dpiAware) {
            // Swing scales the bounds already, so we can use it here to make things simpler
            GraphicsConfiguration config = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            Rectangle rect = config.getBounds();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
            rect.x += insets.left;
            rect.y += insets.top;
            rect.width -= insets.left + insets.right;
            rect.height -= insets.top + insets.bottom;
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
    public Area getActiveWindow() {
        return activeWindow;
    }

    @Override
    public String getActiveWindowTitle() {
        return activeWindowTitle;
    }

    @Override
    public long getActiveWindowId() {
        return activeWindowHandle == null ? 0 : activeWindowHandle.hashCode();
    }

    @Override
    public void moveActiveWindow(int x, int y) {
        if (activeWindowHandle == null) {
            return;
        }

        double dpiScale = Toolkit.getDefaultToolkit().getScreenResolution() / 96.0;
        if (dpiScale != 1) {
            x = (int) Math.round(x * dpiScale);
            y = (int) Math.round(y * dpiScale);
        }

        /* Use SetWindowPos() instead of MoveWindow() so we don't have to
        pass the previous dimensions of the window to the function */
        User32.INSTANCE.SetWindowPos(activeWindowHandle, null, x, y,
                0, 0, User32.SWP_NOSIZE);
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
                    // Out-of-bounds interactive window found

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
        validTitleCache.clear(); // Will be repopulated in the next hasValidTitle() call
        windowTitles = null;
        windowTitlesBlacklist = null;
    }
}
