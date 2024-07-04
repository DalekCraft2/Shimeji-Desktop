package com.group_finity.mascot.mac;

import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.mac.jna.*;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFArrayRef;
import com.sun.jna.platform.mac.CoreFoundation.CFIndex;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

import java.awt.*;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Uses the Accessibility API to obtain environment information that is difficult to obtain using Java.
 *
 * @author nonowarn
 */
class MacEnvironment extends Environment {

    /**
     * On Mac, you can take the active window, so Shimeji will react to it.
     * <p>
     * Therefore, in this class, give {@code activeIE} an alias called {@link #frontmostWindow}.
     */
    private static Area activeIE = new Area();
    private static Area frontmostWindow = activeIE;

    private static final int screenWidth =
            (int) Math.round(Toolkit.getDefaultToolkit().getScreenSize().getWidth());
    private static final int screenHeight =
            (int) Math.round(Toolkit.getDefaultToolkit().getScreenSize().getHeight());

    private static CarbonExtra carbonEx = CarbonExtra.INSTANCE;

    // On Mac, ManagementFactory.getRuntimeMXBean().getName()
    // returns the "PID@machine name" string
    private static long myPID =
            Long.parseLong(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);

    private static long currentPID = myPID;

    private static Set<Long> touchedProcesses = new HashSet<>();

    static final CFStringRef
            kAXPosition = createCFString("AXPosition"),
            kAXSize = createCFString("AXSize"),
            kAXFocusedWindow = createCFString("AXFocusedWindow"),
            kDock = createCFString("com.apple.Dock"),
            kTileSize = createCFString("tilesize"),
            kOrientation = createCFString("orientation"),
            kAXChildren = createCFString("AXChildren");

    private static Rectangle getFrontmostAppRect() {
        Rectangle ret;
        long pid = getCurrentPID();

        AXUIElementRef application =
                carbonEx.AXUIElementCreateApplication(pid);

        PointerByReference windowp = new PointerByReference();

        // XXX: Is error checking necessary other than here?
        if (carbonEx.AXUIElementCopyAttributeValue(
                application, kAXFocusedWindow, windowp) == carbonEx.kAXErrorSuccess) {
            AXUIElementRef window = new AXUIElementRef();
            window.setPointer(windowp.getValue());
            ret = getRectOfWindow(window);
        } else {
            ret = null;
        }

        application.release();
        return ret;
    }

    private static long getFrontmostAppsPID() {
        ProcessSerialNumber frontProcessPsn = new ProcessSerialNumber();
        LongByReference frontProcessPidp = new LongByReference();

        carbonEx.GetFrontProcess(frontProcessPsn);
        carbonEx.GetProcessPID(frontProcessPsn, frontProcessPidp);

        return frontProcessPidp.getValue();
    }

    private static CGPoint getPositionOfWindow(AXUIElementRef window) {
        CGPoint position = new CGPoint();
        AXValueRef axvalue = new AXValueRef();
        PointerByReference valuep = new PointerByReference();

        carbonEx.AXUIElementCopyAttributeValue(window, kAXPosition, valuep);
        axvalue.setPointer(valuep.getValue());
        carbonEx.AXValueGetValue(axvalue, carbonEx.kAXValueCGPointType, position.getPointer());
        position.read();

        return position;
    }

    private static CGSize getSizeOfWindow(AXUIElementRef window) {
        CGSize size = new CGSize();
        AXValueRef axvalue = new AXValueRef();
        PointerByReference valuep = new PointerByReference();

        carbonEx.AXUIElementCopyAttributeValue(window, kAXSize, valuep);
        axvalue.setPointer(valuep.getValue());
        carbonEx.AXValueGetValue(axvalue, carbonEx.kAXValueCGSizeType, size.getPointer());
        size.read();

        return size;
    }

    private static void moveFrontmostWindow(final Point point) {
        AXUIElementRef application =
                carbonEx.AXUIElementCreateApplication(currentPID);

        PointerByReference windowp = new PointerByReference();

        if (carbonEx.AXUIElementCopyAttributeValue(
                application, kAXFocusedWindow, windowp) == carbonEx.kAXErrorSuccess) {
            AXUIElementRef window = new AXUIElementRef();
            window.setPointer(windowp.getValue());
            moveWindow(window, point.x, point.y);
        }

        application.release();
    }

    private static void restoreWindowsNotIn(final Rectangle rect) {
        Rectangle visibleArea = getWindowVisibleArea();
        for (long pid : getTouchedProcesses()) {
            AXUIElementRef application =
                    carbonEx.AXUIElementCreateApplication(pid);

            for (AXUIElementRef window : getWindowsOf(application)) {
                window.retain();
                Rectangle windowRect = getRectOfWindow(window);
                if (!visibleArea.intersects(windowRect)) {
                    moveWindow(window, 0, 0);
                }
                window.release();
            }

            application.release();
        }
    }

    private static Iterable<AXUIElementRef> getWindowsOf(AXUIElementRef application) {
        PointerByReference axWindowsp = new PointerByReference();
        Collection<AXUIElementRef> ret = new ArrayList<>();

        carbonEx.AXUIElementCopyAttributeValue(application, kAXChildren, axWindowsp);

        if (axWindowsp.getValue() == Pointer.NULL) {
            return ret;
        }

        CFArrayRef cfWindows = new CFArrayRef(axWindowsp.getValue());

        for (int i = 0, count = cfWindows.getCount(); i < count; i++) {
            Pointer p = cfWindows.getValueAtIndex(i);
            AXUIElementRef el = new AXUIElementRef();
            el.setPointer(p);
            ret.add(el);
        }

        return ret;
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
                carbonEx.kAXValueCGPointType, position.getPointer());
        carbonEx.AXUIElementSetAttributeValue(window, kAXPosition, axvalue);
    }

    private static CFStringRef createCFString(String s) {
        return CFStringRef.createCFString(s);
    }

    private static int getScreenWidth() {
        return screenWidth;
    }

    private static int getScreenHeight() {
        return screenHeight;
    }

    /**
     * If {@code min < max}, return {@code a} if {@code min <= a <= max}.
     * If {@code a < min}, return {@code min}.
     * If {@code a > max}, return {@code max}.
     */
    private static double betweenOrLimit(double a, double min, double max) {
        return Math.min(Math.max(a, min), max);
    }

    /**
     * Returns the range that will not be pushed back even if the window is moved within the screen as a Rectangle.
     * On Mac, if you try to move the window completely off the screen,
     * the window gets pushed back into the screen.
     */
    private static Rectangle getWindowVisibleArea() {
        final int menuBarHeight = 22;
        int x = 1, y = menuBarHeight,
                width = getScreenWidth() - 2, // Because it's 0-origin
                height = getScreenHeight() - menuBarHeight;

        refreshDockState();
        final String orientation = getDockOrientation();
        final int tilesize = getDockTileSize();

        if ("bottom".equals(orientation)) {
            height -= tilesize;
        } else if ("right".equals(orientation)) {
            width -= tilesize;
        } else if ("left".equals(orientation)) {
            x += tilesize;
            width -= tilesize;
        } else /* if ("null".equals(orientation)) */ {
            // We don't know the direction of the Dock, so we want it to be in either direction.
            x += tilesize;
            width -= 2 * tilesize;
        }

        return new Rectangle(x, y, width, height);
    }

    private static String getDockOrientation() {
        CFTypeRef orientationRef =
                carbonEx.CFPreferencesCopyValue(
                        kOrientation, kDock, carbonEx.kCurrentUser, carbonEx.kAnyHost);

        // There are environments where CFPreferencesCopyValue returns null
        if (orientationRef == null) {
            return "null";
        }

        // Cast the property to a string ref
        CFStringRef orientationStringRef = new CFStringRef(orientationRef.getPointer());

        final int bufsize = 64;
        Memory buf = new Memory(64);
        CoreFoundation.INSTANCE.CFStringGetCString(
                orientationStringRef, buf, new CFIndex(bufsize), carbonEx.CFStringGetSystemEncoding());
        orientationStringRef.release();
        String ret = buf.getString(0);
        buf.clear();
        return ret;
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

    private static long getCurrentPID() {
        return currentPID;
    }

    private static void setCurrentPID(long newPID) {
        if (newPID != myPID) {
            currentPID = newPID;
            getTouchedProcesses().add(newPID);
        }
    }

    private static Set<Long> getTouchedProcesses() {
        return touchedProcesses;
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
        frontmostWindow.set(
                frontmostWindowRect == null ? new Rectangle(-1, -1, 0, 0) : frontmostWindowRect);
    }

    private static void updateFrontmostApp() {
        long newPID = getFrontmostAppsPID();
        setCurrentPID(newPID);
    }

    @Override
    public void tick() {
        super.tick();
        updateFrontmostApp();
        updateFrontmostWindow();
    }

    @Override
    public void moveActiveIE(final Point point) {
        /*
         * As mentioned above, if you try to move completely off-screen, you will be pushed back,
         * so if you specify such a position, switch to moving as far as possible.
         */
        final Rectangle
                visibleRect = getWindowVisibleArea(),
                windowRect = getFrontmostAppRect();

        final double
                minX = visibleRect.getMinX() - windowRect.getWidth(), // Left direction wrap coordinate
                maxX = visibleRect.getMaxX(), // Right direction wrap coordinate
                minY = visibleRect.getMinY(), // Upward wrap coordinate
                // (Cannot move above the menu bar)
                maxY = visibleRect.getMaxY(); // Downward wrap coordinate

        double
                pX = point.getX(),
                pY = point.getY();

        // Wrapping in the X direction
        pX = betweenOrLimit(pX, minX, maxX);

        // Wrapping in Y direction
        pY = betweenOrLimit(pY, minY, maxY);

        point.setLocation(pX, pY);
        moveFrontmostWindow(point);
    }

    @Override
    public void restoreIE() {
        final Rectangle visibleRect = getWindowVisibleArea();
        restoreWindowsNotIn(visibleRect);
        getTouchedProcesses().clear();
    }

    @Override
    public Area getWorkArea() {
        return getScreen();
    }

    @Override
    public Area getActiveIE() {
        return activeIE;
    }

    @Override
    public String getActiveIETitle() {
        return null;
    }

    @Override
    public long getActiveWindowId() {
        return currentPID;
    }

    @Override
    public void refreshCache() {
    }

    @Override
    public void dispose() {
    }
}
