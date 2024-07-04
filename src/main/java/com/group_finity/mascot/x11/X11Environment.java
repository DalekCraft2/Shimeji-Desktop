/*
 * Created by asdfman
 * https://github.com/asdfman/linux-shimeji
 */
package com.group_finity.mascot.x11;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Manager;
import com.group_finity.mascot.environment.*;
import com.group_finity.mascot.x11.X.Display;
import com.group_finity.mascot.x11.X.Window;
import com.group_finity.mascot.x11.X.X11Exception;
import com.group_finity.mascot.x11.jna.X11Extra;
import com.sun.jna.platform.unix.X11;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Uses JNI to obtain environment information that is difficult to obtain with Java.
 *
 * @author asdfman
 */
class X11Environment extends Environment {

    /**
     * The {@link X} display.
     */
    private Display display = new Display();

    /**
     * Hashtable for storing the active windows.
     */
    public WindowContainer ieContainer = new WindowContainer();

    private HashMap<Window, Boolean> ieCache = new LinkedHashMap<>();

    /**
     * Randomly chosen window for jump action targeting.
     */
    public Area activeIe = new Area();

    private Window activeIeObject = null;

    /**
     * Current screen. Never changes after initial assignment.
     * {@link Environment} and {@link ComplexArea} handle detection
     * and dual monitor behavior.
     */
    public static final Area workArea = new Area();

    private boolean checkTitles = true;
    private boolean updateOnNext = false;

    /**
     * Counter variable used for things which should happen less
     * frequently than each tick. Initialized at 400 to
     * force an early {@link #activeIe} selection.
     */
    private int q = 400;

    /**
     * Variables for configuration options.
     */
    private int xOffset, yOffset, wMod, hMod = 0;
    private String[] windowTitles = null;


    /**
     * Random number generator for choosing a window for jump actions.
     */
    private static final Random RNG = new Random();

    /**
     * Storage for Window IDs. Only used for comparison when removing
     * user-terminated windows.
     */
    private Collection<Number> curActiveWin = new ArrayList<>();
    private List<Number> curVisibleWin = new ArrayList<>();

    /**
     * Storage for values of certain state/type atoms on the current display.
     */
    private Collection<Number> badStateList = new ArrayList<>();
    private Collection<Number> badTypeList = new ArrayList<>();
    private int maximizedVertValue;
    private int maximizedHorzValue;
    private int minimizedValue;
    private int fullscreenValue;
    private int dockValue;

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
     * Sets work area and reads configuration files.
     */
    X11Environment() {
        workArea.set(getWorkAreaRect());

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
        // TODO Change this proprietary config format to use the existing one
        /* try (InputStream fstream = Files.newInputStream(Paths.get("window.conf")); DataInputStream in = new DataInputStream(fstream); InputStreamReader inr = new InputStreamReader(in); BufferedReader br = new BufferedReader(inr)) {
            String strLine;
            int z = 0;
            while ((strLine = br.readLine()) != null) {
                z++;
                switch (z) {
                    case 1:
                        break;
                    case 2:
                        xOffset = Integer.parseInt(strLine.trim());
                        break;
                    case 3:
                        yOffset = Integer.parseInt(strLine.trim());
                        break;
                    case 4:
                        wMod = Integer.parseInt(strLine.trim());
                        break;
                    case 5:
                        hMod = Integer.parseInt(strLine.trim());
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException | NumberFormatException ignored) {
        } */
    }


    /**
     * Called every 40 milliseconds. Defined in {@link Manager}.
     */
    @Override
    public void tick() {
        super.tick();
        // TODO Figure out why this has been set to update on every 5th tick.
        /* if (q % 5 == 0 || updateOnNext) {
            update();
        }
        // New jump action target window every 1000 ticks
        if (q == 1000) {
            getRandomIE();
            q = 0;
        }
        q++; */

        // workArea.set(getWorkAreaRect());
        final Rectangle ieRect = getWindowBounds(findActiveIE());
        activeIe.setVisible(ieRect.intersects(getScreen().toRectangle()));
        activeIe.set(ieRect);
    }

    /**
     * Window handling. Called each tick.
     */
    private void update() {
        Window[] allWindows;
        int x, y, w, h, id, curDesktop;
        Rectangle r;
        Area a;
        if (curVisibleWin != null) {
            curVisibleWin.clear();
        }
        curActiveWin.clear();
        if (display == null) {
            return;
        }
        updateOnNext = false;
        try {
            // Retrieve all windows from the X Display
            // allWindows = display.getWindows();
            // allWindows = display.getRootWindow().getSubwindows();
            allWindows = display.getRootWindow().getAllSubwindows();

            try {
                curDesktop = display.getActiveDesktopNumber();
            } catch (X11Exception e) {
                curDesktop = 0;
            }
            for (Window window : allWindows) {
                // Break for-loop if the window title does not match config.
                if (!isIE(window)) {
                    // Check checkTitles after isIE() is called because isIE() sets the value of checkTitles.
                    if (checkTitles) {
                        continue;
                    }
                }
                // Get window attributes.
                id = window.getID();
                Rectangle bounds = window.getBounds();
                x = bounds.x + xOffset;
                y = bounds.y + yOffset;
                w = bounds.width + wMod;
                h = bounds.height + hMod;
                if (ieContainer.containsKey(id)) {
                    a = ieContainer.get(id);
                    int desktop;
                    try {
                        desktop = window.getDesktop();
                    } catch (X11Exception e) {
                        desktop = 0;
                    }
                    boolean badDesktop = desktop != curDesktop && desktop != -1;
                    boolean badState = checkState(Arrays.asList(window.getState()));
                    if (checkTitles) {
                        if (badDesktop || badState) {
                            ieContainer.get(id).setVisible(false);
                        } else {
                            ieContainer.get(id).setVisible(true);
                            curVisibleWin.add(id);
                        }
                    } else {
                        boolean badType = checkType(Arrays.asList(window.getType()));
                        if (badDesktop || badType || badState) {
                            ieContainer.get(id).setVisible(false);
                        } else {
                            ieContainer.get(id).setVisible(true);
                            curVisibleWin.add(id);
                        }
                    }
                    r = a.toRectangle();
                    Rectangle newRect = new Rectangle(x, y, w, h);
                    if (!r.equals(newRect)) {
                        updateOnNext = true;
                    }
                    a.set(newRect);
                    curActiveWin.add(id);
                } else {
                    r = new Rectangle(x, y, w, h);
                    a = new Area();
                    a.set(r);
                    a.setVisible(false);
                    ieContainer.put(id, a);
                    curActiveWin.add(id);
                }
            }
        } catch (X11Exception ignored) {
        }
        // Remove user-terminated windows from the container every 5th tick
        for (Map.Entry<Number, Area> entry : ieContainer.entrySet()) {
            Number i = entry.getKey();
            if (!curActiveWin.contains(i)) {
                entry.getValue().setVisible(false);
                ieContainer.remove(i);
                break;
            }
        }
    }

    private boolean isIE(final Window window) {
        /* String titleBar = getWindowTitle(window)

        if (windowTitles == null) {
            windowTitles = new ArrayList<>();
            windowTitles.addAll(Arrays.asList(Main.getInstance().getProperties().getProperty("InteractiveWindows", "").split("/")));
            for (int i = 0; i < windowTitles.size(); i++) {
                if (windowTitles.get(i).trim().isEmpty()) {
                    windowTitles.remove(i);
                    i--;
                }
            }
            checkTitles = !windowTitles.isEmpty();
        }

        return windowTitles.stream().anyMatch(s -> titleBar.toLowerCase().contains(s)); */

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

        if (windowTitles == null) {
            windowTitles = Main.getInstance().getProperties().getProperty("InteractiveWindows", "").split("/");
        }

        for (String windowTitle : windowTitles) {
            if (!windowTitle.trim().isEmpty() && ieTitle.contains(windowTitle)) {
                // Window is IE
                ieCache.put(window, true);
                return true;
            }
        }

        // Window is not IE
        ieCache.put(window, false);
        return false;
    }

    private IeStatus getIeStatus(Window window) {
        int curDesktop;
        int desktop;
        // int[] state;
        // int[] type;
        List<Integer> state;
        List<Integer> type;
        try {
            curDesktop = display.getActiveDesktopNumber();
            desktop = window.getDesktop();
            state = Arrays.asList(window.getState());
            type = Arrays.asList(window.getType());
        } catch (X11Exception e) {
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

            // TODO Find some X11 atom which is dedicated to a window being minimized,
            // because _NET_WM_STATE_HIDDEN is used for both invisible windows and minimized windows
            if (isIE(window) && /* (flags & User32.WS_MINIMIZE) == 0 */ !state.contains(minimizedValue)) {
                // IE found
                Rectangle ieRect = getWindowBounds(window);
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
        // Window.Geometry geometry = window.getGeometry();
        // return new Rectangle(geometry.x + geometry.borderWidth, geometry.y + geometry.borderWidth, geometry.width - 2 * geometry.borderWidth, geometry.height - 2 * geometry.borderWidth);
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
        if (checkTitles) {
            return state.contains(minimizedValue);
        } else {
            return state.stream().anyMatch(value -> badStateList.contains(value));
        }
    }

    private boolean checkType(Collection<Integer> type) {
        if (type == null || type.isEmpty()) {
            return true;
        }
        if (checkTitles) {
            return false;
        } else {
            return type.stream().anyMatch(value -> badTypeList.contains(value));
        }
    }

    private Rectangle getWorkAreaRect() {
        return getScreen().toRectangle();
    }

    /**
     * Assigns a new randomly selected window to {@link #activeIe}
     * for jump action targeting.
     */
    private void getRandomIE() {
        if (curVisibleWin == null) {
            return;
        }
        ArrayList<Area> visibleWin = curVisibleWin.stream().filter(Objects::nonNull).map(n -> ieContainer.get(n)).collect(Collectors.toCollection(ArrayList::new));
        if (visibleWin.isEmpty()) {
            return;
        }
        activeIe = visibleWin.get(RNG.nextInt(visibleWin.size()));
    }

    public int getDockValue() {
        return dockValue;
    }

    public List<Number> getVisible() {
        return curVisibleWin;
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

    // TODO Implement the three below methods
    @Override
    public void moveActiveIE(Point point) {
        if (activeIeObject != null) {
            // FIXME Mascots will often let go of a window very shortly after they pick it up, without throwing it
            X11Extra.INSTANCE.XMoveWindow(display.getX11Display(), activeIeObject.getX11Window(), point.x, point.y);
        }
    }

    @Override
    public void restoreIE() {
        // FIXME The implementation below crashes the program.

        /* // Retrieve all windows from the X Display
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
                X11Extra.INSTANCE.XMoveWindow(display.getX11Display(), window.getX11Window(), rect.x, rect.y);
                // TODO Bring windows to front
                // User32.INSTANCE.BringWindowToTop(window);

                offset += 25;
            }
        } */
    }

    @Override
    public void refreshCache() {
        ieContainer.clear(); // will be repopulated next isIE call
        ieCache.clear();
        windowTitles = null;
        curActiveWin.clear();
        curVisibleWin.clear();
    }

    @Override
    public void dispose() {
    }

    @Override
    public Area getWorkArea() {
        return workArea;
    }

    /**
     * Hashtable extension storing {@link Window} IDs and {@link Area} objects
     * representing the window dimensions. Methods called by
     * mascots (via {@link MascotEnvironment}) when choosing new actions
     * to check whether they're on a border of any kind.
     */
    public static class WindowContainer extends Hashtable<Number, Area> {

        /**
         * onBorder, getBorder methods - Called by mascots when
         * determining the next action. Iterate through all windows,
         * get the specific border type and call its isOn() to
         * check whether the mascot anchor is on the border. One
         * method for plain boolean checks, one for getting the border
         * when needing to decide movement destinations based on it.
         */
        public boolean onLeft(final Point p) {
            return values().stream().map(area -> new Wall(area, false)).anyMatch(w -> w.isOn(p));
        }

        public boolean onRight(final Point p) {
            return values().stream().map(area -> new Wall(area, true)).anyMatch(w -> w.isOn(p));
        }

        public boolean onTop(final Point p) {
            return values().stream().map(area -> new FloorCeiling(area, false)).anyMatch(w -> w.isOn(p));
        }

        public boolean onBottom(final Point p) {
            return values().stream().map(area -> new FloorCeiling(area, true)).anyMatch(w -> w.isOn(p));
        }

        public Wall getLeft(final Point p) {
            return values().stream().map(area -> new Wall(area, false)).filter(w -> w.isOn(p)).findFirst().orElse(null);
        }

        public Wall getRight(final Point p) {
            return values().stream().map(area -> new Wall(area, true)).filter(w -> w.isOn(p)).findFirst().orElse(null);
        }

        public FloorCeiling getTop(final Point p) {
            return values().stream().map(area -> new FloorCeiling(area, false)).filter(w -> w.isOn(p)).findFirst().orElse(null);
        }

        public FloorCeiling getBottom(final Point p) {
            return values().stream().map(area -> new FloorCeiling(area, true)).filter(w -> w.isOn(p)).findFirst().orElse(null);
        }
    }
}
