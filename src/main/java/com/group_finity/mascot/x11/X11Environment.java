/*
 * Created by asdfman, Ygarr, and Pro-Prietary
 * https://github.com/asdfman/linux-shimeji
 * https://github.com/Ygarr/linux-shimeji
 * https://github.com/Pro-Prietary/sayori-shimeji-linux
 */
package com.group_finity.mascot.x11;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Manager;
import com.group_finity.mascot.environment.*;
import com.group_finity.mascot.x11.X.Display;
import com.group_finity.mascot.x11.X.Window;
import com.group_finity.mascot.x11.X.X11Exception;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

class X11Environment extends Environment {

    /**
     *  The {@link X} display.
     */
    private Display display = new Display();

    /**
     * Hashtable for storing the active windows.
     */
    public WindowContainer IE = new WindowContainer();

    /**
     * Randomly chosen window for jump action targeting.
     */
    public Area activeIE = new Area();

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
     * force an early {@link #activeIE} selection.
     */
    private int q = 400;

    /**
     * Variables for configuration options.
     */
    private int xoffset, yoffset, wmod, hmod = 0;
    private List<String> windowTitles = new ArrayList<>();


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
    private int minimizedValue;
    private int dockValue;

    /**
     * Initializes a new {@code X11Environment}.
     * Sets work area and reads configuration files.
     */
    X11Environment() {
        workArea.set(getWorkAreaRect());
        badStateList.add(Integer.decode(display.getAtom("_NET_WM_STATE_MODAL").toString()));
        badStateList.add(Integer.decode(display.getAtom("_NET_WM_STATE_HIDDEN").toString()));
        minimizedValue = Integer.decode(display.getAtom("_NET_WM_STATE_HIDDEN").toString());
        badStateList.add(Integer.decode(display.getAtom("_NET_WM_STATE_ABOVE").toString()));
        badTypeList.add(Integer.decode(display.getAtom("_NET_WM_WINDOW_TYPE_DOCK").toString()));
        dockValue = Integer.decode(display.getAtom("_NET_WM_WINDOW_TYPE_DOCK").toString());
        badTypeList.add(Integer.decode(display.getAtom("_NET_WM_WINDOW_TYPE_MENU").toString()));
        badTypeList.add(Integer.decode(display.getAtom("_NET_WM_WINDOW_TYPE_SPLASH").toString()));
        badTypeList.add(Integer.decode(display.getAtom("_NET_WM_WINDOW_TYPE_DIALOG").toString()));
        badTypeList.add(Integer.decode(display.getAtom("_NET_WM_WINDOW_TYPE_DESKTOP").toString()));
        try (InputStream fstream = Files.newInputStream(Paths.get("window.conf")); DataInputStream in = new DataInputStream(fstream); InputStreamReader inr = new InputStreamReader(in); BufferedReader br = new BufferedReader(inr)) {
            String strLine;
            int z = 0;
            while ((strLine = br.readLine()) != null) {
                z++;
                switch (z) {
                    case 1:
                        break;
                    case 2:
                        xoffset = Integer.parseInt(strLine.trim());
                        break;
                    case 3:
                        yoffset = Integer.parseInt(strLine.trim());
                        break;
                    case 4:
                        wmod = Integer.parseInt(strLine.trim());
                        break;
                    case 5:
                        hmod = Integer.parseInt(strLine.trim());
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException | NumberFormatException ignored) {
        }
        windowTitles.addAll(Arrays.asList(Main.getInstance().getProperties().getProperty("InteractiveWindows", "").split("/")));
        for (int i = 0; i < windowTitles.size(); i++) {
            if (windowTitles.get(i).trim().isEmpty()) {
                windowTitles.remove(i);
                i--;
            }
        }
        if (windowTitles.isEmpty()) {
            checkTitles = false;
        }
    }


    /**
     * Called every 40 milliseconds. Defined in {@link Manager}.
     */
    @Override
    public void tick() {
        super.tick();
        if (q % 5 == 0 || updateOnNext) {
            update();
        }
        // New jump action target window every 1000 ticks
        if (q == 1000) {
            getRandomIE();
            q = 0;
        }
        q++;
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
        curActiveWin = new ArrayList<>();
        if (display == null) {
            return;
        }
        updateOnNext = false;
        try {
            // Retrieve all windows from the X Display
            allWindows = display.getWindows();
            curDesktop = display.getActiveDesktopNumber();
            for (Window allWindow : allWindows) {
                // Break for-loop if the window title does not match config.
                if (checkTitles) {
                    if (!isIE(allWindow.getTitle())) {
                        continue;
                    }
                }
                // Get window attributes.
                id = allWindow.getID();
                w = allWindow.getGeometry().width + wmod;
                h = allWindow.getGeometry().height + hmod;
                x = allWindow.getBounds().x + xoffset;
                y = allWindow.getBounds().y + yoffset;
                if (IE.containsKey(id)) {
                    a = IE.get(id);
                    int desktop = allWindow.getDesktop();
                    boolean badDesktop = desktop != curDesktop && desktop != -1;
                    boolean badState = checkState(allWindow.getState());
                    if (checkTitles) {
                        if (badDesktop || badState) {
                            IE.get(id).setVisible(false);
                        } else {
                            IE.get(id).setVisible(true);
                            curVisibleWin.add(id);
                        }
                    } else {
                        boolean badType = checkType(allWindow.getType());
                        if (badDesktop || badType || badState) {
                            IE.get(id).setVisible(false);
                        } else {
                            IE.get(id).setVisible(true);
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
                    IE.put(id, a);
                    curActiveWin.add(id);
                }
            }
        } catch (X11Exception ignored) {
        }
        // Remove user-terminated windows from the container every 5th tick
        for (Number i : IE.keySet()) {
            if (!curActiveWin.contains(i)) {
                IE.get(i).setVisible(false);
                IE.remove(i);
                break;
            }
        }
    }

    private boolean isIE(String titleBar) {
        return windowTitles.stream().anyMatch(s -> titleBar.toLowerCase().contains(s));
    }

    private boolean checkState(int state) {
        if (checkTitles) {
            return state == minimizedValue;
        } else {
            return badStateList.contains(state);
        }
    }

    private boolean checkType(int type) {
        return badTypeList.contains(type);
    }

    private Rectangle getWorkAreaRect() {
        return getScreen().toRectangle();
    }

    /**
     * Assigns a new randomly selected window to {@link #activeIE}
     * for jump action targeting.
     */
    private void getRandomIE() {
        ArrayList<Area> visibleWin;
        if (curVisibleWin == null) {
            return;
        }
        visibleWin = curVisibleWin.stream().filter(Objects::nonNull).map(n -> IE.get(n)).collect(Collectors.toCollection(ArrayList::new));
        if (visibleWin.isEmpty()) {
            return;
        }
        activeIE = visibleWin.get(RNG.nextInt(visibleWin.size()));
    }

    public int getDockValue() {
        return dockValue;
    }

    public List<Number> getVisible() {
        return curVisibleWin;
    }

    @Override
    public Area getActiveIE() {
        return activeIE;
    }

    // TODO Implement the five below methods
    @Override
    public String getActiveIETitle() {
        return null;
    }

    @Override
    public void moveActiveIE(Point point) {

    }

    @Override
    public void restoreIE() {

    }

    @Override
    public void refreshCache() {

    }

    @Override
    public void dispose() {
    }

    // @Override
    public WindowContainer getIE() {
        return IE;
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
