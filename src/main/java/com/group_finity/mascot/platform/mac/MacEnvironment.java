/*
 * Created by nonowarn
 * https://github.com/nonowarn/shimeji4mac
 */
package com.group_finity.mascot.platform.mac;

import com.group_finity.mascot.environment.AbstractEnvironment;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.platform.mac.jna.AXUIElementRef;
import com.group_finity.mascot.platform.mac.jna.AXValueRef;
import com.group_finity.mascot.platform.mac.jna.CarbonExtra;
import com.group_finity.mascot.platform.mac.jna.ProcessSerialNumber;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFArrayRef;
import com.sun.jna.platform.mac.CoreFoundation.CFIndex;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.platform.mac.CoreGraphics.CGPoint;
import com.sun.jna.platform.mac.CoreGraphics.CGSize;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * An implementation of {@link AbstractEnvironment} that provides access to the macOS native environment via
 * the macOS Accessibility API.
 *
 * @author nonowarn
 */
class MacEnvironment extends AbstractEnvironment {

    /**
     * The instance of the {@code Carbon} library interface, for convenience.
     */
    private static final CarbonExtra carbonEx = CarbonExtra.INSTANCE;

    /**
     * The area of the active window.
     * If there is currently no active window, this area's position will be set to {@code (-1, -1)},
     * and its dimensions will be set to {@code (0, 0)}.
     *
     * @see #frontmostWindow
     * @see #getActiveWindow()
     */
    private final Area activeWindow = new Area();

    /**
     * On macOS, it is possible to obtain the active window, so we configure Shimeji to react to it.
     * <p>
     * Therefore, within this class, we assign the alias {@code frontmostWindow} to {@link #activeWindow}.
     *
     * @see #activeWindow
     */
    private final Area frontmostWindow = activeWindow;

    /**
     * The process ID of the current process, that being the Shimeji-ee program.
     */
    private final int myPID = (int) ProcessHandle.current().pid();

    /**
     * The process ID of the active window.
     *
     * @see #getCurrentPID()
     * @see #setCurrentPID(int)
     */
    private int currentPID = myPID;

    /**
     * A set containing the process IDs of all windows that have at some point been the active window.
     */
    private final Set<Integer> touchedProcesses = new HashSet<>();

    /**
     * <a href="https://developer.apple.com/documentation/applicationservices/kaxpositionattribute">Apple docs: kAXPositionAttribute</a>
     * <h4>Discussion</h4>
     * The global screen coordinates of the top-left corner of this accessibility object. Note that the coordinates
     * {@code 0,0} represent the top-left corner of the screen that displays the menu bar. All accessibility objects
     * that have a screen position (in other words, are visible on the screen) should include this attribute.
     */
    static final CFStringRef kAXPositionAttribute = CFStringRef.createCFString("AXPosition");

    /**
     * <a href="https://developer.apple.com/documentation/applicationservices/kaxsizeattribute">Apple docs: kAXSizeAttribute</a>
     * <p>
     * The vertical and horizontal dimensions of this accessibility object. This attribute is required for all
     * accessibility objects that are visible on the screen.
     */
    static final CFStringRef kAXSizeAttribute = CFStringRef.createCFString("AXSize");

    /**
     * <a href="https://developer.apple.com/documentation/applicationservices/kaxfocusedwindowattribute">Apple docs: kAXFocusedWindowAttribute</a>
     * <p>
     * The accessibility object that represents the currently focused window of this application. This attribute is
     * recommended for all application-level accessibility objects.
     */
    static final CFStringRef kAXFocusedWindowAttribute = CFStringRef.createCFString("AXFocusedWindow");

    /**
     * <a href="https://developer.apple.com/documentation/applicationservices/kaxchildrenattribute">Apple docs: kAXChildrenAttribute</a>
     * <h4>Discussion</h4>
     * An array of the first-order accessibility objects contained by this accessibility object. An accessibility object
     * may be a member of only one {@code AXChildren} array. This attribute is required for all accessibility objects
     * that contain accessible child objects.
     */
    static final CFStringRef kAXChildrenAttribute = CFStringRef.createCFString("AXChildren");

    /** The application ID of the dock. */
    static final CFStringRef kDock = CFStringRef.createCFString("com.apple.Dock");
    /** The preference key of the tile size for the dock. */
    static final CFStringRef kTileSize = CFStringRef.createCFString("tilesize");
    /** The preference key of the orientation for the dock. */
    static final CFStringRef kOrientation = CFStringRef.createCFString("orientation");

    /**
     * Gets the bounds of the window that is attributed to the current frontmost app.
     *
     * @return the bounds of the window that is attributed to the current frontmost app,
     * or {@code null} if the frontmost app's window can't be accessed
     */
    private Rectangle getFrontmostAppRect() {
        Rectangle ret;
        int pid = getCurrentPID();

        AXUIElementRef application =
                carbonEx.AXUIElementCreateApplication(pid);
        try {
            PointerByReference windowp = new PointerByReference();

            // XXX: Is error checking necessary other than here?
            if (carbonEx.AXUIElementCopyAttributeValue(
                    application, kAXFocusedWindowAttribute, windowp) == CarbonExtra.kAXErrorSuccess) {
                AXUIElementRef window = new AXUIElementRef(windowp.getValue());
                ret = getRectOfWindow(window);
            } else {
                ret = null;
            }
        } finally {
            application.release();
        }

        return ret;
    }

    /**
     * Gets the process ID of the frontmost app through the macOS API.
     *
     * @return the process ID of the frontmost app
     */
    private static int getFrontmostAppPID() {
        ProcessSerialNumber frontProcessPsn = new ProcessSerialNumber();
        IntByReference frontProcessPidp = new IntByReference();

        carbonEx.GetFrontProcess(frontProcessPsn);
        carbonEx.GetProcessPID(frontProcessPsn, frontProcessPidp);

        return frontProcessPidp.getValue();
    }

    /**
     * Gets the position of the specified window.
     *
     * @param window the window whose position is to be returned
     * @return the position of the specified window
     */
    private static CGPoint getPositionOfWindow(AXUIElementRef window) {
        PointerByReference valuep = new PointerByReference();
        carbonEx.AXUIElementCopyAttributeValue(window, kAXPositionAttribute, valuep);

        AXValueRef axvalue = new AXValueRef(valuep.getValue());
        CGPoint position = new CGPoint();
        carbonEx.AXValueGetValue(axvalue, CarbonExtra.kAXValueCGPointType, position.getPointer());
        position.read();

        return position;
    }

    /**
     * Gets the size of the specified window.
     *
     * @param window the window whose size is to be returned
     * @return the size of the specified window
     */
    private static CGSize getSizeOfWindow(AXUIElementRef window) {
        PointerByReference valuep = new PointerByReference();
        carbonEx.AXUIElementCopyAttributeValue(window, kAXSizeAttribute, valuep);

        AXValueRef axvalue = new AXValueRef(valuep.getValue());
        CGSize size = new CGSize();
        carbonEx.AXValueGetValue(axvalue, CarbonExtra.kAXValueCGSizeType, size.getPointer());
        size.read();

        return size;
    }

    /**
     * Repositions the frontmost window so its top-left corner is at the specified location {@code (x, y)}.
     *
     * @param x the x-coordinate at which the frontmost window's left side should be after it is moved
     * @param y the y-coordinate at which the frontmost window's top side should be after it is moved
     */
    private void moveFrontmostWindow(final int x, final int y) {
        AXUIElementRef application =
                carbonEx.AXUIElementCreateApplication(currentPID);
        try {
            PointerByReference windowp = new PointerByReference();

            if (carbonEx.AXUIElementCopyAttributeValue(
                    application, kAXFocusedWindowAttribute, windowp) == CarbonExtra.kAXErrorSuccess) {
                AXUIElementRef window = new AXUIElementRef(windowp.getValue());
                moveWindow(window, x, y);
            }
        } finally {
            application.release();
        }
    }

    /**
     * Searches for any {@linkplain #touchedProcesses touched processes} whose windows do not overlap with the specified
     * rectangle region, and moves their windows to be at position {@code (0, 0)}.
     *
     * @param rect the region to check against the touched processes
     */
    private void restoreWindowsNotIn(final Rectangle rect) {
        if (touchedProcesses.isEmpty()) {
            return;
        }
        for (int pid : touchedProcesses) {
            AXUIElementRef application =
                    carbonEx.AXUIElementCreateApplication(pid);
            try {
                List<AXUIElementRef> windowsOfApp = getWindowsOf(application);
                if (!windowsOfApp.isEmpty()) {
                    for (AXUIElementRef window : windowsOfApp) {
                        window.retain();
                        try {
                            Rectangle windowRect = getRectOfWindow(window);
                            if (!rect.intersects(windowRect)) {
                                moveWindow(window, 0, 0);
                            }
                        } finally {
                            window.release();
                        }
                    }
                }
            } finally {
                application.release();
            }
        }
    }

    /**
     * Gets a list of the windows that are attributed to the specified application.
     *
     * @param application the application whose attributed windows are to be returned
     * @return a list of the windows that are attributed to the specified application
     */
    private static List<AXUIElementRef> getWindowsOf(AXUIElementRef application) {
        PointerByReference axWindowsp = new PointerByReference();

        carbonEx.AXUIElementCopyAttributeValue(application, kAXChildrenAttribute, axWindowsp);

        if (axWindowsp.getValue() == Pointer.NULL) {
            return List.of();
        }

        CFArrayRef cfWindows = new CFArrayRef(axWindowsp.getValue());

        return IntStream.range(0, cfWindows.getCount()).mapToObj(cfWindows::getValueAtIndex).map(AXUIElementRef::new).collect(Collectors.toList());
    }

    /**
     * Gets the bounds of the specified window.
     *
     * @param window the window whose bounds are to be returned
     * @return the bounds of the specified window
     */
    private static Rectangle getRectOfWindow(AXUIElementRef window) {
        CGPoint pos = getPositionOfWindow(window);
        CGSize size = getSizeOfWindow(window);
        return new Rectangle(
                (int) Math.round(pos.x), (int) Math.round(pos.y),
                (int) Math.round(size.width), (int) Math.round(size.height)
        );
    }

    /**
     * Repositions the specified window so its top-left corner is at the specified location {@code (x, y)}.
     *
     * @param window the window to reposition
     * @param x the x-coordinate at which the window's left side should be after it is moved
     * @param y the y-coordinate at which the window's top side should be after it is moved
     */
    private static void moveWindow(AXUIElementRef window, int x, int y) {
        CGPoint position = new CGPoint();
        position.x = x;
        position.y = y;
        position.write();
        AXValueRef axvalue = carbonEx.AXValueCreate(
                CarbonExtra.kAXValueCGPointType, position.getPointer());
        carbonEx.AXUIElementSetAttributeValue(window, kAXPositionAttribute, axvalue);
    }

    /**
     * Gets a {@link Rectangle} representing the area within the screen where a window can be moved without being
     * pushed back onto the screen.
     * On macOS, if one attempts to move a window completely off-screen, it is pushed back onto the screen.
     *
     * @return the area within the screen where a window can be moved without being pushed back onto the screen
     */
    private Rectangle getWindowVisibleArea() {
        // TODO: Get the menu bar height programmatically
        final int menuBarHeight = 22;
        int x = 1, y = menuBarHeight,
                width = getScreen().getWidth() - 2, // Because it's 0-origin
                height = getScreen().getHeight() - menuBarHeight;

        refreshDockState();
        final String orientation = getDockOrientation();
        final int tileSize = getDockTileSize();

        switch (orientation) {
            case "bottom" -> height -= tileSize;
            case "right" -> width -= tileSize;
            case "left" -> {
                x += tileSize;
                width -= tileSize;
            }
            case null, default -> {
                // We don't know the direction of the Dock, so we want it to be in either direction.
                x += tileSize;
                width -= 2 * tileSize;
            }
        }

        return new Rectangle(x, y, width, height);
    }

    /**
     * Gets a string representation of the orientation of the dock for the current user.
     * The possible orientation values are "bottom", "left", and "right".
     *
     * @return one of "bottom", "left", or "right"; or "null" if the orientation value could not be read
     */
    private static String getDockOrientation() {
        CFTypeRef orientationRef =
                carbonEx.CFPreferencesCopyValue(
                        kOrientation, kDock, CarbonExtra.kCFPreferencesCurrentUser, CarbonExtra.kCFPreferencesAnyHost);

        // There are environments where CFPreferencesCopyValue returns null
        if (orientationRef == null) {
            return "null";
        }

        try {
            // Cast the property to a string ref
            CFStringRef orientationStringRef = new CFStringRef(orientationRef.getPointer());

            try (Memory buf = new Memory(64)) {
                CoreFoundation.INSTANCE.CFStringGetCString(
                        orientationStringRef, buf, new CFIndex(buf.size()), carbonEx.CFStringGetSystemEncoding());
                return buf.getString(0);
            }
        } finally {
            orientationRef.release();
        }
    }

    /**
     * Gets the tile size of the dock.
     *
     * @return the tile size of the dock
     */
    private static int getDockTileSize() {
        /*
         * Since I cannot find an efficient way to monitor the Dock's height,
         * I am returning a constant value larger than the Dock's maximum size for the time being.
         *
         * The value obtained via CFPreferencesCopyValue differs from the value obtained via AppleScript,
         * and the AppleScript value is the correct one.
         *
         * While the correct value can be retrieved by obtaining the PID and using the Accessibility API,
         * a segmentation fault occurs if `killall Dock` is executed.
         * To avoid the SEGV, the PID must be re-fetched each time,
         * but I cannot find a method to do this other than traversing the process list.
         * I want to avoid using AppleScript given the frequency of calls.
         * I will consider this trade-off later.
         */
        return 100;
    }

    /**
     * Writes to permanent storage all pending changes to the preference data for the dock application.
     */
    private static void refreshDockState() {
        carbonEx.CFPreferencesAppSynchronize(kDock);
    }

    /**
     * Gets the process ID of the active window.
     *
     * @return the process ID of the active window
     * @see #setCurrentPID(int)
     */
    private int getCurrentPID() {
        return currentPID;
    }

    /**
     * Sets the process ID of the active window.
     *
     * @param newPID the new process ID of the active window
     */
    private void setCurrentPID(int newPID) {
        if (newPID != myPID) {
            currentPID = newPID;
            touchedProcesses.add(newPID);
        }
    }

    /**
     * Updates the area of the active window.
     */
    private void updateFrontmostWindow() {
        final Rectangle
                frontmostWindowRect = getFrontmostAppRect(),
                windowVisibleArea = getWindowVisibleArea();

        frontmostWindow.setVisible(
                frontmostWindowRect != null
                        && frontmostWindowRect.intersects(windowVisibleArea)
                        && !frontmostWindowRect.contains(windowVisibleArea) // Exclude desktop
        );
        if (frontmostWindowRect == null) {
            frontmostWindow.setRect(-1, -1, 0, 0);
        } else {
            frontmostWindow.set(frontmostWindowRect);
        }
    }

    /**
     * Updates the process ID of the active window.
     *
     * @see #getFrontmostAppPID()
     * @see #setCurrentPID(int)
     */
    private void updateFrontmostApp() {
        int newPID = getFrontmostAppPID();
        setCurrentPID(newPID);
    }

    @Override
    public void tick() {
        super.tick();
        long prevWindowId = getActiveWindowId();
        updateFrontmostApp();
        updateFrontmostWindow();

        if (prevWindowId != getActiveWindowId()) {
            // If the active window has changed, reset the active window's deltas to 0
            frontmostWindow.resetDeltas();
        }
    }

    @Override
    public Area getActiveWindow() {
        return activeWindow;
    }

    @Override
    public String getActiveWindowTitle() {
        return null;
    }

    @Override
    public long getActiveWindowId() {
        return currentPID;
    }

    /**
     * Repositions the active window so its top-left corner is at the specified location {@code (x, y)}.
     * If necessary, the specified position will be adjusted such that the window does not completely leave the screen.
     * <p>
     * On macOS, if one attempts to move a window completely off-screen, it is pushed back onto the screen.
     * Therefore, if the specified position puts the window completely off-screen, it will be adjusted to move the
     * window as far off-screen as possible without being pushed back onto the screen.
     *
     * @param x the x-coordinate at which the active window's left side should be after it is moved
     * @param y the y-coordinate at which the active window's top side should be after it is moved
     */
    @Override
    public void moveActiveWindow(int x, int y) {
        /*
         * As mentioned in getWindowVisibleArea(), attempting to move completely off-screen results in being pushed back
         * onto the screen; therefore, for such positioning requests, the movement is adjusted to go as far as possible.
         */
        final Rectangle
                visibleRect = getWindowVisibleArea(),
                windowRect = getFrontmostAppRect();

        if (windowRect == null)
            return;

        final double
                minX = visibleRect.getMinX() - windowRect.getWidth(), // Left direction wrap coordinate
                maxX = visibleRect.getMaxX(), // Right direction wrap coordinate
                minY = visibleRect.getMinY(), // Upward wrap coordinate
                // (Cannot move above the menu bar)
                maxY = visibleRect.getMaxY(); // Downward wrap coordinate

        // Wrapping in the X direction
        x = (int) Math.clamp(x, minX, maxX);

        // Wrapping in the Y direction
        y = (int) Math.clamp(y, minY, maxY);

        moveFrontmostWindow(x, y);
    }

    @Override
    public void restoreWindows() {
        final Rectangle visibleRect = getWindowVisibleArea();
        restoreWindowsNotIn(visibleRect);
        touchedProcesses.clear();
    }

    @Override
    public void refreshCache() {
    }
}
