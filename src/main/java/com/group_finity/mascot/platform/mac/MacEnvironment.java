/*
 * Created by nonowarn
 * https://github.com/nonowarn/shimeji4mac
 */
package com.group_finity.mascot.platform.mac;

import com.group_finity.mascot.environment.AbstractEnvironment;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.platform.mac.jna.*;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFArrayRef;
import com.sun.jna.platform.mac.CoreFoundation.CFIndex;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Uses the Accessibility API to obtain environment information that is difficult to obtain using Java.
 *
 * @author nonowarn
 */
class MacEnvironment extends AbstractEnvironment {

    private static final CarbonExtra carbonEx = CarbonExtra.INSTANCE;

    /**
     * On Mac, you can take the active window, so mascots will react to it.
     * <p>
     * Therefore, in this class, give {@code activeWindow} an alias called {@link #frontmostWindow}.
     */
    private final Area activeWindow = new Area();
    private final Area frontmostWindow = activeWindow;

    private final int myPID = (int) ProcessHandle.current().pid();

    private int currentPID = myPID;

    private final Set<Integer> touchedProcesses = new HashSet<>();

    static final CFStringRef
            kAXPosition = CFStringRef.createCFString("AXPosition"),
            kAXSize = CFStringRef.createCFString("AXSize"),
            kAXFocusedWindow = CFStringRef.createCFString("AXFocusedWindow"),
            kDock = CFStringRef.createCFString("com.apple.Dock"),
            kTileSize = CFStringRef.createCFString("tilesize"),
            kOrientation = CFStringRef.createCFString("orientation"),
            kAXChildren = CFStringRef.createCFString("AXChildren");

    private Rectangle getFrontmostAppRect() {
        Rectangle ret;
        int pid = getCurrentPID();

        AXUIElementRef application =
                carbonEx.AXUIElementCreateApplication(pid);

        PointerByReference windowp = new PointerByReference();

        // XXX: Is error checking necessary other than here?
        if (carbonEx.AXUIElementCopyAttributeValue(
                application, kAXFocusedWindow, windowp) == CarbonExtra.kAXErrorSuccess) {
            AXUIElementRef window = new AXUIElementRef(windowp.getValue());
            ret = getRectOfWindow(window);
        } else {
            ret = null;
        }

        application.release();
        return ret;
    }

    private static int getFrontmostAppsPID() {
        ProcessSerialNumber frontProcessPsn = new ProcessSerialNumber();
        IntByReference frontProcessPidp = new IntByReference();

        carbonEx.GetFrontProcess(frontProcessPsn);
        carbonEx.GetProcessPID(frontProcessPsn, frontProcessPidp);

        return frontProcessPidp.getValue();
    }

    private static CGPoint getPositionOfWindow(AXUIElementRef window) {
        PointerByReference valuep = new PointerByReference();
        carbonEx.AXUIElementCopyAttributeValue(window, kAXPosition, valuep);

        AXValueRef axvalue = new AXValueRef(valuep.getValue());
        CGPoint position = new CGPoint();
        carbonEx.AXValueGetValue(axvalue, CarbonExtra.kAXValueCGPointType, position.getPointer());
        position.read();

        return position;
    }

    private static CGSize getSizeOfWindow(AXUIElementRef window) {
        PointerByReference valuep = new PointerByReference();
        carbonEx.AXUIElementCopyAttributeValue(window, kAXSize, valuep);

        AXValueRef axvalue = new AXValueRef(valuep.getValue());
        CGSize size = new CGSize();
        carbonEx.AXValueGetValue(axvalue, CarbonExtra.kAXValueCGSizeType, size.getPointer());
        size.read();

        return size;
    }

    private void moveFrontmostWindow(final int x, final int y) {
        AXUIElementRef application =
                carbonEx.AXUIElementCreateApplication(currentPID);

        PointerByReference windowp = new PointerByReference();

        if (carbonEx.AXUIElementCopyAttributeValue(
                application, kAXFocusedWindow, windowp) == CarbonExtra.kAXErrorSuccess) {
            AXUIElementRef window = new AXUIElementRef(windowp.getValue());
            moveWindow(window, x, y);
        }

        application.release();
    }

    private void restoreWindowsNotIn(final Rectangle rect) {
        if (touchedProcesses.isEmpty()) {
            return;
        }
        for (int pid : touchedProcesses) {
            AXUIElementRef application =
                    carbonEx.AXUIElementCreateApplication(pid);

            List<AXUIElementRef> windowsOfApp = getWindowsOf(application);
            if (!windowsOfApp.isEmpty()) {
                for (AXUIElementRef window : windowsOfApp) {
                    window.retain();
                    Rectangle windowRect = getRectOfWindow(window);
                    if (!rect.intersects(windowRect)) {
                        moveWindow(window, 0, 0);
                    }
                    window.release();
                }
            }

            application.release();
        }
    }

    private static List<AXUIElementRef> getWindowsOf(AXUIElementRef application) {
        PointerByReference axWindowsp = new PointerByReference();

        carbonEx.AXUIElementCopyAttributeValue(application, kAXChildren, axWindowsp);

        if (axWindowsp.getValue() == Pointer.NULL) {
            return List.of();
        }

        CFArrayRef cfWindows = new CFArrayRef(axWindowsp.getValue());

        return IntStream.range(0, cfWindows.getCount()).mapToObj(cfWindows::getValueAtIndex).map(AXUIElementRef::new).collect(Collectors.toList());
    }

    private static Rectangle getRectOfWindow(AXUIElementRef window) {
        CGPoint pos = getPositionOfWindow(window);
        CGSize size = getSizeOfWindow(window);
        return new Rectangle(pos.getX(), pos.getY(), size.getWidth(), size.getHeight());
    }

    private static void moveWindow(AXUIElementRef window, int x, int y) {
        CGPoint position = new CGPoint(x, y);
        position.write();
        AXValueRef axvalue = carbonEx.AXValueCreate(
                CarbonExtra.kAXValueCGPointType, position.getPointer());
        carbonEx.AXUIElementSetAttributeValue(window, kAXPosition, axvalue);
    }

    /**
     * Returns the range that will not be pushed back even if the window is moved within the screen as a Rectangle.
     * On Mac, if you try to move the window completely off the screen,
     * the window gets pushed back into the screen.
     */
    private Rectangle getWindowVisibleArea() {
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

    private static String getDockOrientation() {
        CFTypeRef orientationRef =
                carbonEx.CFPreferencesCopyValue(
                        kOrientation, kDock, CarbonExtra.kCFPreferencesCurrentUser, CarbonExtra.kCFPreferencesAnyHost);

        // There are environments where CFPreferencesCopyValue returns null
        if (orientationRef == null) {
            return "null";
        }

        // Cast the property to a string ref
        CFStringRef orientationStringRef = new CFStringRef(orientationRef.getPointer());

        final int bufSize = 64;
        try (Memory buf = new Memory(bufSize)) {
            CoreFoundation.INSTANCE.CFStringGetCString(
                    orientationStringRef, buf, new CFIndex(bufSize), carbonEx.CFStringGetSystemEncoding());
            orientationStringRef.release();
            return buf.getString(0);
        }
    }

    private static int getDockTileSize() {
        /*
         * Since there is no efficient way to monitor the height of the Dock,
         * we will return a constant larger than the maximum size of the Dock for now.
         *
         * The value obtained by CFPreferencesCopyValue is different from the value obtained by AppleScript,
         * and AppleScript is the correct value.
         *
         * If you get the PID and use the Accessibility API, you can get the correct value,
         * but if you do a killall Dock, it will SEGV.
         * In order to avoid SEGV, it is necessary to reset the pid every time,
         * but I can't find any other way other than going through the list of processes.
         * Considering how often it is called, I don't want to use AppleScript.
         * We will consider this trade-off later.
         */
        return 100;
    }

    private static void refreshDockState() {
        carbonEx.CFPreferencesAppSynchronize(kDock);
    }

    private int getCurrentPID() {
        return currentPID;
    }

    private void setCurrentPID(int newPID) {
        if (newPID != myPID) {
            currentPID = newPID;
            touchedProcesses.add(newPID);
        }
    }

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

    private void updateFrontmostApp() {
        int newPID = getFrontmostAppsPID();
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

    @Override
    public void moveActiveWindow(int x, int y) {
        /*
         * As mentioned above, if you try to move completely off-screen, you will be pushed back,
         * so if you specify such a position, switch to moving as far as possible.
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

        // Wrapping in Y direction
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
