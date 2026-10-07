/* Copyright (c) 2008 Stefan Endrullis, All Rights Reserved
 *
 * The contents of this file is dual-licensed under 2
 * alternative Open Source/Free licenses: LGPL 2.1 or later and
 * Apache License 2.0. (starting with JNA version 4.0.0).
 *
 * You can freely decide which license you want to apply to
 * the project.
 *
 * You may obtain a copy of the LGPL License at:
 *
 * http://www.gnu.org/licenses/licenses.html
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "LGPL2.1".
 *
 * You may obtain a copy of the Apache License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "AL2.0".
 */
package com.group_finity.mascot.platform.x11;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.platform.unix.X11.Atom;
import com.sun.jna.platform.unix.X11.WindowByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Object-oriented X window system.
 * <p>
 * Some code segments in this class are based on the code of the program
 * wmctrl (licensed under GPLv2) written by Tomas Styblo &lt;tripie@cpan.org&gt;.
 * Thanks a lot, Tomas!
 *
 * @author Stefan Endrullis
 */
public class X {
    /** Remove/unset property. */
    public static final int _NET_WM_STATE_REMOVE = 0;
    /** Add/set property. */
    public static final int _NET_WM_STATE_ADD = 1;
    /** Toggle property. */
    public static final int _NET_WM_STATE_TOGGLE = 2;
    /** Maximal property value length. */
    public static final int MAX_PROPERTY_VALUE_LEN = 4096;

    private static final X11 x11 = X11.INSTANCE;

    private static int bytesToInt(byte[] prop) {
        return (prop[3] & 0xff) << 24
                | (prop[2] & 0xff) << 16
                | (prop[1] & 0xff) << 8
                | prop[0] & 0xff;
    }

    private static int bytesToInt(byte[] prop, int offset) {
        return (prop[3 + offset] & 0xff) << 24
                | (prop[2 + offset] & 0xff) << 16
                | (prop[1 + offset] & 0xff) << 8
                | prop[offset] & 0xff;
    }

    private static int bytesToInt(byte b1, byte b2, byte b3, byte b4) {
        return (b4 & 0xff) << 24
                | (b3 & 0xff) << 16
                | (b2 & 0xff) << 8
                | b1 & 0xff;
    }

    private static int bytesToInt(byte b1, byte b2, byte b3, byte b4, int offset) {
        return ((b4 + offset) & 0xff) << 24
                | ((b3 + offset) & 0xff) << 16
                | ((b2 + offset) & 0xff) << 8
                | (b1 + offset) & 0xff;
    }


    /**
     * X Display.
     */
    public static class Display {
        /**
         * The native X11 display.
         */
        private final X11.Display x11Display;
        /**
         * Map used for caching atoms.
         */
        private final HashMap<String, Atom> atomsHash = new HashMap<>();

        /**
         * Creates a new Display using the default native X11 display.
         *
         * @throws RuntimeException if the default X11 display fails to be opened
         */
        public Display() {
            x11Display = x11.XOpenDisplay(null);

            if (x11Display == null) {
                throw new RuntimeException("Cannot open default X11 display");
            }
        }

        /**
         * Creates a new Display using the specified native X11 display.
         *
         * @param x11Display the native X11 display
         * @throws IllegalArgumentException if {@code x11Display} is {@code null}
         */
        public Display(X11.Display x11Display) {
            if (x11Display == null) {
                throw new IllegalArgumentException("x11Display cannot be null");
            }

            this.x11Display = x11Display;
        }

        /**
         * Closes this display.
         */
        public void close() {
            x11.XCloseDisplay(x11Display);
        }

        /**
         * Flushes the output buffer / event queue.
         */
        public void flush() {
            x11.XFlush(x11Display);
        }

        /**
         * Gets the native X11 display.
         *
         * @return the native X11 display
         */
        public X11.Display getX11Display() {
            return x11Display;
        }

        /**
         * Gets the internal atom with the specified name.
         *
         * @param name the name of the atom
         * @return the atom with the specified name
         */
        public X11.Atom getAtom(String name) {
            X11.Atom atom = atomsHash.get(name);
            if (atom == null) {
                atom = x11.XInternAtom(x11Display, name, false);
                atomsHash.put(name, atom);
            }
            return atom;
        }

        /**
         * Gets the window manager information as a window.
         *
         * @return the window manager information as a window
         * @throws X11Exception if an X11 window error occurred
         */
        public Window getWindowManagerInfo() throws X11Exception {
            Window rootWindow = getRootWindow();

            try {
                return rootWindow.getWindowProperty(X11.XA_WINDOW, "_NET_SUPPORTING_WM_CHECK");
            } catch (X11Exception e) {
                try {
                    return rootWindow.getWindowProperty(X11.XA_CARDINAL, "_WIN_SUPPORTING_WM_CHECK");
                } catch (X11Exception e1) {
                    throw new X11Exception("Cannot get window manager info properties. (_NET_SUPPORTING_WM_CHECK or _WIN_SUPPORTING_WM_CHECK)", e1);
                }
            }
        }

        /**
         * Gets the root window.
         *
         * @return the root window
         */
        public Window getRootWindow() {
            return new Window(this, x11.XDefaultRootWindow(x11Display));
        }

        /**
         * Gets the current active window.
         *
         * @return the current active window
         * @throws X11Exception if an X11 window error occurred
         */
        public Window getActiveWindow() throws X11Exception {
            return getRootWindow().getWindowProperty(X11.XA_WINDOW, "_NET_ACTIVE_WINDOW");
        }

        /**
         * Gets all windows managed by the window manager, ordered by age from oldest to newest.
         *
         * @return all windows managed by the window manager, ordered by age from oldest to newest
         * @throws X11Exception if an X11 window error occurred
         */
        public Window[] getWindows() throws X11Exception {
            byte[] bytes;
            Window rootWindow = getRootWindow();

            try {
                bytes = rootWindow.getProperty(X11.XA_WINDOW, "_NET_CLIENT_LIST");
            } catch (X11Exception e) {
                try {
                    bytes = rootWindow.getProperty(X11.XA_CARDINAL, "_WIN_CLIENT_LIST");
                } catch (X11Exception e1) {
                    throw new X11Exception("Cannot get client list properties (_NET_CLIENT_LIST or _WIN_CLIENT_LIST)", e1);
                }
            }

            Window[] windows = new Window[bytes.length / X11.Window.SIZE];

            for (int i = 0; i < windows.length; i++) {
                windows[i] = new Window(this, new X11.Window(bytesToInt(bytes, X11.XID.SIZE * i)));
            }

            return windows;
        }

        /**
         * Gets all windows managed by the window manager, ordered by layer from bottom to top.
         *
         * @return all windows managed by the window manager, ordered by layer from bottom to top
         * @throws X11Exception if an X11 window error occurred
         */
        public Window[] getWindowsLayerOrdered() throws X11Exception {
            byte[] bytes;
            Window rootWindow = getRootWindow();

            bytes = rootWindow.getProperty(X11.XA_WINDOW, "_NET_CLIENT_LIST_STACKING");

            Window[] windows = new Window[bytes.length / X11.Window.SIZE];

            for (int i = 0; i < windows.length; i++) {
                windows[i] = new Window(this, new X11.Window(bytesToInt(bytes, X11.XID.SIZE * i)));
            }

            return windows;
        }

        /**
         * Gets the number of desktops.
         *
         * @return the number of desktops
         * @throws X11Exception if an X11 window error occurred
         */
        public Integer getDesktopCount() throws X11Exception {
            Window root = getRootWindow();

            try {
                return root.getIntProperty(X11.XA_CARDINAL, "_NET_NUMBER_OF_DESKTOPS");
            } catch (X11Exception e) {
                try {
                    return root.getIntProperty(X11.XA_CARDINAL, "_WIN_WORKSPACE_COUNT");
                } catch (X11Exception e1) {
                    throw new X11Exception("Cannot get number of desktops properties (_NET_NUMBER_OF_DESKTOPS or _WIN_WORKSPACE_COUNT)", e1);
                }
            }
        }

        /**
         * Gets the index of the active desktop.
         *
         * @return the index of the active desktop
         * @throws X11Exception if an X11 window error occurred
         */
        public Integer getActiveDesktopNumber() throws X11Exception {
            Window root = getRootWindow();

            try {
                return root.getIntProperty(X11.XA_CARDINAL, "_NET_CURRENT_DESKTOP");
            } catch (X11Exception e) {
                try {
                    return root.getIntProperty(X11.XA_CARDINAL, "_WIN_WORKSPACE");
                } catch (X11Exception e1) {
                    throw new X11Exception("Cannot get current desktop properties (_NET_CURRENT_DESKTOP or _WIN_WORKSPACE property)", e1);
                }
            }
        }

        /**
         * Gets the available desktops.
         *
         * @return the available desktops
         * @throws X11Exception if an X11 window error occurred
         */
        public Desktop[] getDesktops() throws X11Exception {
            Window root = getRootWindow();
            String[] desktopNames;
            try {
                desktopNames = root.getUtf8StringArrayProperty(getAtom("UTF8_STRING"), "_NET_DESKTOP_NAMES");
            } catch (X11Exception e) {
                try {
                    desktopNames = root.getStringArrayProperty(X11.XA_STRING, "_WIN_WORKSPACE_NAMES");
                } catch (X11Exception e1) {
                    throw new X11Exception("Cannot get desktop names properties (_NET_DESKTOP_NAMES or _WIN_WORKSPACE_NAMES)", e1);
                }
            }

            Desktop[] desktops = new Desktop[getDesktopCount()];
            for (int i = 0; i < desktops.length; i++) {
                desktops[i] = new Desktop(this, i, desktopNames[i]);
            }

            return desktops;
        }

        /**
         * Switches to the specified desktop.
         *
         * @param index the index of the desktop to which to switch
         * @throws X11Exception if an X11 window error occurred
         */
        public void switchDesktop(int index) throws X11Exception {
            getRootWindow().clientMsg("_NET_CURRENT_DESKTOP", index, 0, 0, 0, 0);
        }

        /**
         * Sets the "showing the desktop" state.
         *
         * @param state {@code true} if the desktop should be shown; {@code false} otherwise
         * @throws X11Exception if an X11 window error occurred
         */
        public void showingDesktop(boolean state) throws X11Exception {
            getRootWindow().clientMsg("_NET_SHOWING_DESKTOP", state ? 1 : 0, 0, 0, 0, 0);
        }

        /**
         * Enables / disables the auto-repeat of pressed keys.
         *
         * @param on {@code true} if auto-repeat should be enabled; {@code false} otherwise
         */
        public void setKeyAutoRepeat(boolean on) {
            if (on) {
                x11.XAutoRepeatOn(x11Display);
            } else {
                x11.XAutoRepeatOff(x11Display);
            }
        }

        /**
         * Gets the key symbol corresponding to the specified key name.
         *
         * @param keyName the name of the key
         * @return the key symbol corresponding to the key name
         */
        public X11.KeySym getKeySym(String keyName) {
            return x11.XStringToKeysym(keyName);
        }

        /**
         * Gets the key symbol corresponding to the specified keycode and the specified index in the keycode vector.
         *
         * @param keyCode the keycode
         * @param index the index in the keycode vector
         * @return the key symbol corresponding to the keycode and the index in the keycode vector
         */
        public X11.KeySym getKeySym(byte keyCode, int index) {
            return x11.XKeycodeToKeysym(x11Display, keyCode, index);
        }

        /**
         * Gets the keycode corresponding to the specified key symbol.
         *
         * @param keySym the key symbol
         * @return the keycode corresponding to the key symbol
         */
        public byte getKeyCode(X11.KeySym keySym) {
            return x11.XKeysymToKeycode(x11Display, keySym);
        }

        /**
         * Gets the keycode corresponding to the specified key name.
         *
         * @param keyName the name of the key
         * @return the keycode corresponding to the key name
         */
        public byte getKeyCode(String keyName) {
            return x11.XKeysymToKeycode(x11Display, getKeySym(keyName));
        }

        /**
         * Gets the key name corresponding to the specified key symbol.
         *
         * @param keySym the key symbol
         * @return the key name corresponding to the key symbol
         */
        public String getKeyName(X11.KeySym keySym) {
            return x11.XKeysymToString(keySym);
        }

        /**
         * Gets the key name corresponding to the specified keycode and the specified index in the keycode vector.
         *
         * @param keyCode the keycode
         * @param index the index in the keycode vector
         * @return the key name corresponding to the keycode and the index in the keycode vector
         */
        public String getKeyName(byte keyCode, int index) {
            return getKeyName(getKeySym(keyCode, index));
        }

        /**
         * Gets the modifier keymap.
         *
         * @return modifier keymap
         */
        public ModifierKeymap getModifierKeymap() {
            X11.XModifierKeymapRef xModifierKeymapRef = x11.XGetModifierMapping(x11Display);
            try {
                ModifierKeymap modifierKeymap = new ModifierKeymap(xModifierKeymapRef);
                return modifierKeymap;
            } finally {
                x11.XFreeModifiermap(xModifierKeymapRef);
            }
        }

        /**
         * Sets the modifier keymap.
         *
         * @param modifierKeymap modifier keymap
         */
        public void setModifierKeymap(ModifierKeymap modifierKeymap) {
            X11.XModifierKeymapRef xModifierKeymapRef = modifierKeymap.toXModifierKeymap();
            x11.XSetModifierMapping(x11Display, xModifierKeymapRef);
        }
    }


    /**
     * Modifier keymap. The lists {@link #shift}, {@link #lock}, {@link #control},
     * {@link #mod1}, {@link #mod2}, {@link #mod3}, {@link #mod4}, and {@link #mod5}
     * contain the keycodes as Byte objects. You can directly access these lists to
     * read, replace, remove, or insert new keycodes to these modifiers.
     * To apply a new modifier keymap, call
     * {@link X.Display#setModifierKeymap(ModifierKeymap)}.
     */
    public static class ModifierKeymap {
        /** Shift modifier as a list of bytes. */
        public ArrayList<Byte> shift = new ArrayList<>(4);
        /** Lock modifier as a list of bytes. */
        public ArrayList<Byte> lock = new ArrayList<>(4);
        /** Control modifier as a list of bytes. */
        public ArrayList<Byte> control = new ArrayList<>(4);
        /** Mod1 modifier as a list of bytes. */
        public ArrayList<Byte> mod1 = new ArrayList<>(4);
        /** Mod2 modifier as a list of bytes. */
        public ArrayList<Byte> mod2 = new ArrayList<>(4);
        /** Mod3 modifier as a list of bytes. */
        public ArrayList<Byte> mod3 = new ArrayList<>(4);
        /** Mod4 modifier as a list of bytes. */
        public ArrayList<Byte> mod4 = new ArrayList<>(4);
        /** Mod5 modifier as a list of bytes. */
        public ArrayList<Byte> mod5 = new ArrayList<>(4);

        /**
         * Creates an empty modifier keymap.
         */
        public ModifierKeymap() {
        }

        /**
         * Creates a modifier keymap and reads the modifiers from the specified {@code XModifierKeymap}.
         *
         * @param xModifierKeymapRef the {@code XModifierKeymap} from which to read the modifiers
         */
        public ModifierKeymap(X11.XModifierKeymapRef xModifierKeymapRef) {
            fromXModifierKeymap(xModifierKeymapRef);
        }

        /**
         * Reads all modifiers from the specified {@code XModifierKeymap}.
         *
         * @param xModifierKeymapRef the {@code XModifierKeymap} from which to read the modifiers
         */
        public void fromXModifierKeymap(X11.XModifierKeymapRef xModifierKeymapRef) {
            int count = xModifierKeymapRef.max_keypermod;
            byte[] keys = xModifierKeymapRef.modifiermap.getByteArray(0, 8 * count);

            ArrayList<Byte>[] allModifiers = getAllModifiers();

            for (int modNr = 0; modNr < 8; modNr++) {
                List<Byte> modifier = allModifiers[modNr];
                modifier.clear();

                for (int keyNr = 0; keyNr < count; keyNr++) {
                    byte key = keys[modNr * count + keyNr];
                    if (key != 0) {
                        modifier.add(key);
                    }
                }
            }
        }

        /**
         * Gets an {@code XModifierKeymap} corresponding to this object.
         *
         * @return XModifierKeymap
         */
        public X11.XModifierKeymapRef toXModifierKeymap() {
            ArrayList<Byte>[] allModifiers = getAllModifiers();

            // determine max list size
            int count = 0;
            for (List<Byte> allModifier : allModifiers) {
                count = Math.max(count, allModifier.size());
            }

            byte[] keys = new byte[8 * count];
            for (int modNr = 0; modNr < 8; modNr++) {
                List<Byte> modifier = allModifiers[modNr];

                for (int keyNr = 0; keyNr < modifier.size(); keyNr++) {
                    keys[modNr * count + keyNr] = modifier.get(keyNr);
                }
            }

            X11.XModifierKeymapRef xModifierKeymapRef = new X11.XModifierKeymapRef();
            xModifierKeymapRef.max_keypermod = count;
            xModifierKeymapRef.modifiermap = new Memory(keys.length);
            xModifierKeymapRef.modifiermap.write(0, keys, 0, keys.length);

            return xModifierKeymapRef;
        }

        /**
         * Gets an array containing all modifier lists.
         *
         * @return an array of modifier lists
         */
        @SuppressWarnings("unchecked")
        public ArrayList<Byte>[] getAllModifiers() {
            return new ArrayList[]{
                    shift, lock, control, mod1, mod2, mod3, mod4, mod5
            };
        }
    }

    /**
     * X Desktop.
     */
    public static class Desktop {
        public X.Display display;
        public int number;
        public String name;

        public Desktop(Display display, int number, String name) {
            this.display = display;
            this.number = number;
            this.name = name;
        }
    }


    /**
     * X Window.
     */
    public static class Window {
        private final X.Display display;
        private final X11.Window x11Window;

        /**
         * Gets the native X11 window.
         *
         * @return the native X11 window
         */
        public X11.Window getX11Window() {
            return x11Window;
        }

        /**
         * Gets the ID of this window.
         *
         * @return the window ID
         */
        public int getID() {
            return x11Window.intValue();
        }

        /**
         * Creates a new window.
         *
         * @param display the display where this window is allocated
         * @param x11Window the native X11 window
         */
        public Window(X.Display display, X11.Window x11Window) {
            this.display = display;
            this.x11Window = x11Window;
        }

        /**
         * Gets the title of this window.
         *
         * @return the title of this window
         * @throws X11Exception if an X11 window error occurred
         */
        public String getTitle() throws X11Exception {
            try {
                return getUtf8StringProperty(display.getAtom("UTF8_STRING"), "_NET_WM_NAME");
            } catch (X11Exception e) {
                return getUtf8StringProperty(X11.XA_STRING, X11.XA_WM_NAME);
            }
        }

        /**
         * Gets the state of this window.
         *
         * @return the state of this window
         * @throws X11Exception if an X11 window error occurred
         */
        public int[] getState() throws X11Exception {
            return getIntArrayProperty(X11.XA_ATOM, "_NET_WM_STATE");
        }

        /**
         * Gets the type of this window.
         *
         * @return the type of this window
         * @throws X11Exception if an X11 window error occurred
         */
        public int[] getType() throws X11Exception {
            return getIntArrayProperty(X11.XA_ATOM, "_NET_WM_WINDOW_TYPE");
        }

        /**
         * Returns the window frame extents of this window, formatted as {@code [left, right, top, bottom]}.
         *
         * @return the window frame extents of this window
         * @throws X11Exception if an X11 window error occurred
         */
        public int[] getFrameExtents() throws X11Exception {
            return getIntArrayProperty(X11.XA_CARDINAL, "_NET_FRAME_EXTENTS");
        }

        /**
         * Gets the class of this window.
         *
         * @return the class of this window
         * @throws X11Exception if an X11 window error occurred
         */
        public String getWindowClass() throws X11Exception {
            return getUtf8StringProperty(X11.XA_STRING, X11.XA_WM_CLASS);
        }

        /**
         * Gets the PID of this window.
         *
         * @return the PID of this window
         * @throws X11Exception if an X11 window error occurred
         */
        public Integer getPID() throws X11Exception {
            return getIntProperty(X11.XA_CARDINAL, "_NET_WM_PID");
        }

        /**
         * Gets the desktop ID of this window.
         *
         * @return the desktop ID of this window
         * @throws X11Exception if an X11 window error occurred
         */
        public Integer getDesktop() throws X11Exception {
            try {
                return getIntProperty(X11.XA_CARDINAL, "_NET_WM_DESKTOP");
            } catch (X11Exception e) {
                return getIntProperty(X11.XA_CARDINAL, "_WIN_WORKSPACE");
            }
        }

        /**
         * Gets the client machine name of this window.
         *
         * @return the client machine name of this window
         * @throws X11Exception if an X11 window error occurred
         */
        public String getMachine() throws X11Exception {
            return getStringProperty(X11.XA_STRING, "WM_CLIENT_MACHINE");
        }

        /**
         * Gets the {@code XWindowAttributes} of this window.
         *
         * @return the {@code XWindowAttributes} of this window
         */
        public X11.XWindowAttributes getXWindowAttributes() {
            X11.XWindowAttributes xwa = new X11.XWindowAttributes();
            x11.XGetWindowAttributes(display.x11Display, x11Window, xwa);

            return xwa;
        }

        /**
         * Gets the geometry of this window.
         *
         * @return the geometry of this window
         */
        public Geometry getGeometry() {
            WindowByReference junkRoot = new WindowByReference();
            IntByReference junkX = new IntByReference();
            IntByReference junkY = new IntByReference();
            IntByReference x = new IntByReference();
            IntByReference y = new IntByReference();
            IntByReference width = new IntByReference();
            IntByReference height = new IntByReference();
            IntByReference borderWidth = new IntByReference();
            IntByReference depth = new IntByReference();

            x11.XGetGeometry(display.x11Display, x11Window, junkRoot, junkX, junkY, width, height, borderWidth, depth);

            x11.XTranslateCoordinates(display.x11Display, x11Window, junkRoot.getValue(), 0,
                    0, x, y, junkRoot);

            return new Geometry(x.getValue(), y.getValue(), width.getValue(), height.getValue(),
                    borderWidth.getValue(), depth.getValue());
        }

        /**
         * Gets the bounding box of this window.
         *
         * @return the bounding box of this window
         */
        public Rectangle getBounds() {
            WindowByReference junkRoot = new WindowByReference();
            IntByReference junkX = new IntByReference();
            IntByReference junkY = new IntByReference();
            IntByReference x = new IntByReference();
            IntByReference y = new IntByReference();
            IntByReference width = new IntByReference();
            IntByReference height = new IntByReference();
            IntByReference borderWidth = new IntByReference();
            IntByReference depth = new IntByReference();

            x11.XGetGeometry(display.x11Display, x11Window, junkRoot, junkX, junkY, width, height, borderWidth, depth);

            x11.XTranslateCoordinates(display.x11Display, x11Window, junkRoot.getValue(), 0,
                    0, x, y, junkRoot);

            return new Rectangle(x.getValue(), y.getValue(), width.getValue(), height.getValue());
        }

        /**
         * Activates this window.
         *
         * @throws X11Exception if an X11 window error occurred
         */
        public void activate() throws X11Exception {
            clientMsg("_NET_ACTIVE_WINDOW", 0, 0, 0, 0, 0);
            x11.XMapRaised(display.x11Display, x11Window);
        }

        /**
         * Moves this window to the specified desktop.
         *
         * @param desktopNr the ID of the desktop to which to move this window
         * @return {@link X11#Success} if the operation was successful
         * @throws X11Exception if an X11 window error occurred
         */
        public int moveToDesktop(int desktopNr) throws X11Exception {
            return clientMsg("_NET_WM_DESKTOP", desktopNr, 0, 0, 0, 0);
        }

        /**
         * Selects the input events for which to listen.
         *
         * @param eventMask an event mask representing the events for which to listen
         */
        public void selectInput(int eventMask) {
            x11.XSelectInput(display.x11Display, x11Window, new NativeLong(eventMask));
        }

        public int nextEvent(X11.XEvent event) {
            return x11.XNextEvent(display.x11Display, event);
        }

        public void sendEvent(int eventMask, X11.XEvent event) {
            x11.XSendEvent(display.x11Display, x11Window, 1, new NativeLong(eventMask), event);
        }

        /**
         * Closes this window gracefully.
         *
         * @return {@link X11#Success} if closing was successful
         * @throws X11Exception if an X11 window error occurred
         */
        public int close() throws X11Exception {
            return clientMsg("_NET_CLOSE_WINDOW", 0, 0, 0, 0, 0);
        }

        /**
         * Gets the value of the specified property as an integer.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as an integer,
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public Integer getIntProperty(X11.Atom xaPropType, X11.Atom xaPropName) throws X11Exception {
            byte[] property = getProperty(xaPropType, xaPropName);
            if (property == null) {
                return null;
            }
            return bytesToInt(property);
        }

        /**
         * Gets the value of the specified property as an integer.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as an integer,
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public Integer getIntProperty(X11.Atom xaPropType, String xaPropName) throws X11Exception {
            return getIntProperty(xaPropType, display.getAtom(xaPropName));
        }

        /**
         * Gets the value of the specified property as an integer array.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as an integer array,
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public int[] getIntArrayProperty(X11.Atom xaPropType, X11.Atom xaPropName) throws X11Exception {
            byte[] property = getProperty(xaPropType, xaPropName);
            if (property == null) {
                return null;
            }
            // Native.LONG_SIZE is used in getProperty() to represent the number of bytes used for each entry
            // when the return type is 32 bits
            int arrayLength = property.length / Native.LONG_SIZE;
            int[] array = new int[arrayLength];
            for (int i = 0; i < array.length; i++) {
                int byteIdx = i * Native.LONG_SIZE; // Corresponding index in the byte array
                /* On 64-bit systems, the longs returned by XGetWindowProperty() are just integers
                that are padded in the upper four bytes, so we only need to read the lower four bytes.
                (From https://linux.die.net/man/3/xgetwindowproperty) */
                int value = bytesToInt(property[byteIdx], property[byteIdx + 1], property[byteIdx + 2], property[byteIdx + 3]);
                array[i] = value;
            }
            return array;
        }

        /**
         * Gets the value of the specified property as an integer array.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as an integer array,
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public int[] getIntArrayProperty(X11.Atom xaPropType, String xaPropName) throws X11Exception {
            return getIntArrayProperty(xaPropType, display.getAtom(xaPropName));
        }

        /**
         * Gets the value of the specified property as a window.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as a window,
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public Window getWindowProperty(X11.Atom xaPropType, X11.Atom xaPropName) throws X11Exception {
            Integer windowId = getIntProperty(xaPropType, xaPropName);
            if (windowId == null) {
                return null;
            }
            X11.Window x11Window = new X11.Window(windowId);
            return new Window(display, x11Window);
        }

        /**
         * Gets the value of the specified property as a window.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as a window,
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public Window getWindowProperty(X11.Atom xaPropType, String xaPropName) throws X11Exception {
            return getWindowProperty(xaPropType, display.getAtom(xaPropName));
        }

        /**
         * Gets the value of the specified property as a null-terminated byte array.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as a null-terminated byte array,
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public byte[] getNullTerminatedProperty(X11.Atom xaPropType, X11.Atom xaPropName) throws X11Exception {
            byte[] bytesOrig = getProperty(xaPropType, xaPropName);

            if (bytesOrig == null) {
                return null;
            }

            // search for '\0'
            int i;
            for (i = 0; i < bytesOrig.length; i++) {
                if (bytesOrig[i] == '\0') {
                    break;
                }
            }

            byte[] bytesDest;

            if (i < bytesOrig.length - 1) {
                bytesDest = new byte[i + 1];
                System.arraycopy(bytesOrig, 0, bytesDest, 0, i + 1);
            } else {
                bytesDest = bytesOrig;
            }

            return bytesDest;
        }

        /**
         * Gets the value of the specified property as a null-terminated byte array.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as a null-terminated byte array,
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public byte[] getNullTerminatedProperty(X11.Atom xaPropType, String xaPropName) throws X11Exception {
            return getNullTerminatedProperty(xaPropType, display.getAtom(xaPropName));
        }

        /**
         * Gets the value of the specified property as a byte array where every '\0' character is replaced by '.'.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as a byte array where every '\0' character is replaced by '.',
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public byte[] getNullReplacedStringProperty(X11.Atom xaPropType, X11.Atom xaPropName) throws X11Exception {
            byte[] bytes = getProperty(xaPropType, xaPropName);

            if (bytes == null) {
                return null;
            }

            // search for '\0'
            int i;
            for (i = 0; i < bytes.length; i++) {
                if (bytes[i] == '\0') {
                    bytes[i] = '.';
                }
            }

            return bytes;
        }

        /**
         * Gets the value of the specified property as a byte array where every '\0' character is replaced by '.'.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as a byte array where every '\0' character is replaced by '.',
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public byte[] getNullReplacedStringProperty(X11.Atom xaPropType, String xaPropName) throws X11Exception {
            return getNullReplacedStringProperty(xaPropType, display.getAtom(xaPropName));
        }

        /**
         * Gets the value of the specified property as a string where every '\0' character is replaced by '.'.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as a string where every '\0' character is replaced by '.',
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public String getStringProperty(X11.Atom xaPropType, X11.Atom xaPropName) throws X11Exception {
            byte[] property = getNullReplacedStringProperty(xaPropType, xaPropName);
            if (property == null) {
                return null;
            }
            return new String(property);
        }

        /**
         * Gets the value of the specified property as a string where every '\0' character is replaced by '.'.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as a string where every '\0' character is replaced by '.',
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public String getStringProperty(X11.Atom xaPropType, String xaPropName) throws X11Exception {
            byte[] property = getNullReplacedStringProperty(xaPropType, xaPropName);
            if (property == null) {
                return null;
            }
            return new String(property);
        }

        /**
         * Gets the value of the specified property as a string array.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as a string array,
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public String[] getStringArrayProperty(X11.Atom xaPropType, X11.Atom xaPropName) throws X11Exception {
            byte[] property = getProperty(xaPropType, xaPropName);
            if (property == null) {
                return null;
            }
            return new String(property).split("\0");
        }

        /**
         * Gets the value of the specified property as a string array.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as a string array,
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public String[] getStringArrayProperty(X11.Atom xaPropType, String xaPropName) throws X11Exception {
            byte[] property = getProperty(xaPropType, xaPropName);
            if (property == null) {
                return null;
            }
            return new String(property).split("\0");
        }

        /**
         * Gets the value of the specified property as a UTF-8 string where every '\0' character is replaced by '.'.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as a UTF-8 string where every '\0' character is replaced by '.',
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public String getUtf8StringProperty(X11.Atom xaPropType, X11.Atom xaPropName) throws X11Exception {
            byte[] property = getNullReplacedStringProperty(xaPropType, xaPropName);
            if (property == null) {
                return null;
            }
            return new String(property, StandardCharsets.UTF_8);
        }

        /**
         * Gets the value of the specified property as a UTF-8 string where every '\0' character is replaced by '.'.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as a UTF-8 string where every '\0' character is replaced by '.',
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public String getUtf8StringProperty(X11.Atom xaPropType, String xaPropName) throws X11Exception {
            return getUtf8StringProperty(xaPropType, display.getAtom(xaPropName));
        }

        /**
         * Gets the value of the specified property as a UTF-8 string array.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as a UTF-8 string array,
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public String[] getUtf8StringArrayProperty(X11.Atom xaPropType, X11.Atom xaPropName) throws X11Exception {
            byte[] property = getProperty(xaPropType, xaPropName);
            if (property == null) {
                return null;
            }
            return new String(property, StandardCharsets.UTF_8).split("\0");
        }

        /**
         * Gets the value of the specified property as a UTF-8 string array.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as a UTF-8 string array,
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public String[] getUtf8StringArrayProperty(X11.Atom xaPropType, String xaPropName) throws X11Exception {
            return getUtf8StringArrayProperty(xaPropType, display.getAtom(xaPropName));
        }

        /**
         * Gets the value of the specified property as a byte array.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as a byte array,
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public byte[] getProperty(X11.Atom xaPropType, X11.Atom xaPropName) throws X11Exception {
            // https://github.com/mirror/libX11/blob/master/src/GetProp.c#L34

            X11.AtomByReference xaRetTypeRef = new X11.AtomByReference();
            IntByReference retFormatRef = new IntByReference();
            NativeLongByReference retNItemsRef = new NativeLongByReference();
            NativeLongByReference retBytesAfterRef = new NativeLongByReference();
            PointerByReference retPropRef = new PointerByReference();

            NativeLong longOffset = new NativeLong(0);
            NativeLong longLength = new NativeLong(MAX_PROPERTY_VALUE_LEN / 4);

            /* MAX_PROPERTY_VALUE_LEN / 4 explanation (XGetWindowProperty manpage):
             *
             * long_length = Specifies the length in 32-bit multiples of the
             *              data to be retrieved.
             */
            if (x11.XGetWindowProperty(display.x11Display, x11Window, xaPropName, longOffset, longLength, false,
                    xaPropType, xaRetTypeRef, retFormatRef,
                    retNItemsRef, retBytesAfterRef, retPropRef) != X11.Success) {
                String propName = x11.XGetAtomName(display.x11Display, xaPropName);
                throw new X11Exception("Cannot get " + propName + " property.");
                // The return property is only allocated if XGetWindowProperty succeeds,
                // so we don't need to call XFree() on it here.
            }

            try {
                X11.Atom xaRetType = xaRetTypeRef.getValue();
                Pointer retProp = retPropRef.getValue();

                if (xaRetType == null || xaPropType == null) {
                    // The specified property does not exist for the specified window.
                    return null;
                }

                if (!xaRetType.toNative().equals(xaPropType.toNative())) {
                    // Requested return type does not match actual return type
                    String propName = x11.XGetAtomName(display.x11Display, xaPropName);
                    throw new X11Exception("Invalid type of " + propName + " property");
                }

                int retFormat = retFormatRef.getValue();
                long retNItems = retNItemsRef.getValue().longValue();

                // https://www.ibm.com/docs/en/ibm-mq/9.3.x?topic=platforms-standard-data-types-aix-linux-windows
                int nBytes = switch (retFormat) {
                    // Returned data is a long array; native long size is 4 bytes on 32-bit and 8 bytes on 64-bit
                    case 32 -> Native.LONG_SIZE;
                    // Returned data is a short array; native short size is always 2 bytes
                    case 16 -> 2;
                    // Returned data is a char array; native char size is always 1 byte
                    case 8 -> 1;
                    case 0 -> 0;
                    default -> throw new X11Exception("Invalid return format: " + retFormat);
                };
                int length = Math.min((int) retNItems * nBytes, MAX_PROPERTY_VALUE_LEN);

                byte[] ret = retProp.getByteArray(0, length);

                return ret;
            } finally {
                Pointer retProp = retPropRef.getValue();
                if (retProp != null) {
                    x11.XFree(retProp);
                }
            }
        }

        /**
         * Gets the value of the specified property as a byte array.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as a byte array,
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public byte[] getProperty(X11.Atom xaPropType, String xaPropName) throws X11Exception {
            return getProperty(xaPropType, display.getAtom(xaPropName));
        }

        public int clientMsg(String msg, int data0, int data1, int data2, int data3, int data4) throws X11Exception {
            return clientMsg(
                    msg,
                    new NativeLong(data0),
                    new NativeLong(data1),
                    new NativeLong(data2),
                    new NativeLong(data3),
                    new NativeLong(data4)
            );
        }

        public int clientMsg(String msg, NativeLong data0, NativeLong data1, NativeLong data2, NativeLong data3, NativeLong data4) throws X11Exception {
            NativeLong mask = new NativeLong(X11.SubstructureRedirectMask | X11.SubstructureNotifyMask);

            X11.XClientMessageEvent event = new X11.XClientMessageEvent();
            event.type = X11.ClientMessage;
            event.serial = new NativeLong(0);
            event.send_event = 1;
            event.message_type = display.getAtom(msg);
            event.window = x11Window;
            event.format = 32;
            event.data.setType(NativeLong[].class);
            event.data.l[0] = data0;
            event.data.l[1] = data1;
            event.data.l[2] = data2;
            event.data.l[3] = data3;
            event.data.l[4] = data4;

            X11.XEvent e = new X11.XEvent();
            e.setTypedValue(event);

            if (x11.XSendEvent(display.x11Display, display.getRootWindow().x11Window, 0, mask, e) == 0) {
                throw new X11Exception("Cannot send " + msg + " event.");
            } else {
                return X11.Success;
            }
        }

        public Window[] getSubwindows() throws X11Exception {
            // https://github.com/mirror/libX11/blob/master/src/QuTree.c

            WindowByReference rootRef = new WindowByReference();
            WindowByReference parentRef = new WindowByReference();
            PointerByReference childrenRef = new PointerByReference();
            IntByReference childCountRef = new IntByReference();

            if (x11.XQueryTree(display.x11Display, x11Window, rootRef, parentRef, childrenRef, childCountRef) == 0) {
                throw new X11Exception("Can't query subwindows");
            }

            try {
                Pointer children = childrenRef.getValue();
                int childCount = childCountRef.getValue();

                if (childCount == 0) {
                    return null;
                }

                Window[] retVal = new Window[childCount];
                // Depending on if we're running on 64-bit or 32-bit systems,
                // the Window ID size may be different; we need to make sure that
                // we get the data properly no matter what
                if (X11.XID.SIZE == 4) {
                    int[] windows = children.getIntArray(0, childCount);
                    for (int x = 0; x < retVal.length; x++) {
                        X11.Window win = new X11.Window(windows[x]);
                        retVal[x] = new Window(display, win);
                    }
                } else {
                    long[] windows = children.getLongArray(0, childCount);
                    for (int x = 0; x < retVal.length; x++) {
                        X11.Window win = new X11.Window(windows[x]);
                        retVal[x] = new Window(display, win);
                    }
                }

                return retVal;
            } finally {
                Pointer children = childrenRef.getValue();
                if (children != null) {
                    x11.XFree(children);
                }
            }
        }

        public String toString() {
            return x11Window.toString();
        }

        public static class Geometry {
            public int x, y, width, height, borderWidth, depth;

            public Geometry(int x, int y, int width, int height, int borderWidth, int depth) {
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;
                this.borderWidth = borderWidth;
                this.depth = depth;
            }
        }
    }

    /**
     * General exception that is thrown when an X11 window error occurs.
     */
    public static class X11Exception extends Exception {
        public X11Exception() {
        }

        public X11Exception(String message) {
            super(message);
        }

        public X11Exception(String message, Throwable cause) {
            super(message, cause);
        }

        public X11Exception(Throwable cause) {
            super(cause);
        }
    }
}
