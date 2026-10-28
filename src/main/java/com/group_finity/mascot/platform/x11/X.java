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

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.X11;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

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
    /** Maximum length for property values, in bytes. */
    public static final int MAX_PROPERTY_VALUE_LEN = 4096;

    /**
     * The instance of the {@code X11} library interface, for convenience.
     */
    private static final X11 x11 = X11.INSTANCE;

    /**
     * Converts the first eight entries of the specified byte array into a long.
     * The bytes in the array should be in little-endian order.
     *
     * @param prop a byte array containing the eight bytes of the long, in little-endian order
     * @return the long value corresponding to the eight bytes
     */
    private static long bytesToLong(byte[] prop) {
        return (prop[7] & 0xffL) << 56
                | (prop[6] & 0xffL) << 48
                | (prop[5] & 0xffL) << 40
                | (prop[4] & 0xffL) << 32
                | (prop[3] & 0xffL) << 24
                | (prop[2] & 0xffL) << 16
                | (prop[1] & 0xffL) << 8
                | prop[0] & 0xffL;
    }

    /**
     * Converts eight entries of the specified byte array into a long, starting from the specified offset.
     * The bytes in the array should be in little-endian order.
     *
     * @param prop a byte array containing the eight bytes of the long, in little-endian order
     * @param offset the offset at which to start reading values from the array
     * @return the long value corresponding to the eight bytes
     */
    private static long bytesToLong(byte[] prop, int offset) {
        return (prop[7 + offset] & 0xffL) << 56
                | (prop[6 + offset] & 0xffL) << 48
                | (prop[5 + offset] & 0xffL) << 40
                | (prop[4 + offset] & 0xffL) << 32
                | (prop[3 + offset] & 0xffL) << 24
                | (prop[2 + offset] & 0xffL) << 16
                | (prop[1 + offset] & 0xffL) << 8
                | prop[offset] & 0xffL;
    }

    /**
     * Converts the first four entries of the specified byte array into an integer.
     * The bytes in the array should be in little-endian order.
     *
     * @param prop a byte array containing the four bytes of the integer, in little-endian order
     * @return the integer value corresponding to the four bytes
     */
    private static int bytesToInt(byte[] prop) {
        return (prop[3] & 0xff) << 24
                | (prop[2] & 0xff) << 16
                | (prop[1] & 0xff) << 8
                | prop[0] & 0xff;
    }

    /**
     * Converts four entries of the specified byte array into an integer, starting from the specified offset.
     * The bytes in the array should be in little-endian order.
     *
     * @param prop a byte array containing the four bytes of the integer, in little-endian order
     * @param offset the offset at which to start reading values from the array
     * @return the integer value corresponding to the four bytes
     */
    private static int bytesToInt(byte[] prop, int offset) {
        return (prop[3 + offset] & 0xff) << 24
                | (prop[2 + offset] & 0xff) << 16
                | (prop[1 + offset] & 0xff) << 8
                | prop[offset] & 0xff;
    }

    /**
     * Represents a connection to the X server that controls a display.
     */
    public static class Display {
        /**
         * The native X11 display.
         */
        private final X11.Display x11Display;
        /**
         * Map used for caching atoms.
         */
        private final Map<String, X11.Atom> atomCache = new HashMap<>();

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
         * Closes the connection to the X server for this display and destroys all windows, resource IDs,
         * or other resources that the client has created on this display.
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
         * Gets the atom identifier associated with the specified name.
         *
         * @param name the name of the atom
         * @return the atom identifier with the specified name
         */
        public X11.Atom getAtom(String name) {
            X11.Atom atom = atomCache.get(name);
            if (atom == null) {
                atom = x11.XInternAtom(x11Display, name, false);
                atomCache.put(name, atom);
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
         * Gets the root window for the default screen.
         *
         * @return the root window for the default screen
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
         * Gets all windows managed by the window manager, in initial mapping order from oldest to newest.
         *
         * @return all windows managed by the window manager, in initial mapping order from oldest to newest
         * @throws X11Exception if an X11 window error occurred
         * @see #getWindowsLayerOrdered()
         */
        public Window[] getWindows() throws X11Exception {
            Window rootWindow = getRootWindow();

            try {
                return rootWindow.getWindowArrayProperty(X11.XA_WINDOW, "_NET_CLIENT_LIST");
            } catch (X11Exception e) {
                try {
                    // GNOME winhint with an equivalent function to the EWMH hint above
                    return rootWindow.getWindowArrayProperty(X11.XA_CARDINAL, "_WIN_CLIENT_LIST");
                } catch (X11Exception e1) {
                    throw new X11Exception("Cannot get client list properties (_NET_CLIENT_LIST or _WIN_CLIENT_LIST)", e1);
                }
            }
        }

        /**
         * Gets all windows managed by the window manager, in stacking order from bottommost to topmost.
         *
         * @return all windows managed by the window manager, in stacking order from bottommost to topmost
         * @throws X11Exception if an X11 window error occurred
         * @see #getWindows()
         */
        public Window[] getWindowsLayerOrdered() throws X11Exception {
            return getRootWindow().getWindowArrayProperty(X11.XA_WINDOW, "_NET_CLIENT_LIST_STACKING");
        }

        /**
         * Gets the number of virtual desktops.
         *
         * @return the number of virtual desktops
         * @throws X11Exception if an X11 window error occurred
         */
        public Integer getDesktopCount() throws X11Exception {
            Window root = getRootWindow();

            try {
                return root.getIntProperty(X11.XA_CARDINAL, "_NET_NUMBER_OF_DESKTOPS");
            } catch (X11Exception e) {
                try {
                    // GNOME winhint with an equivalent function to the EWMH hint above
                    return root.getIntProperty(X11.XA_CARDINAL, "_WIN_WORKSPACE_COUNT");
                } catch (X11Exception e1) {
                    throw new X11Exception("Cannot get number of desktops properties (_NET_NUMBER_OF_DESKTOPS or _WIN_WORKSPACE_COUNT)", e1);
                }
            }
        }

        /**
         * Gets the index of the current desktop.
         * This will always be an integer between 0 and {@code getDesktopCount() - 1}.
         *
         * @return the index of the current desktop
         * @throws X11Exception if an X11 window error occurred
         * @see #switchDesktop(int)
         */
        public Integer getActiveDesktopNumber() throws X11Exception {
            Window root = getRootWindow();

            try {
                return root.getIntProperty(X11.XA_CARDINAL, "_NET_CURRENT_DESKTOP");
            } catch (X11Exception e) {
                try {
                    // GNOME winhint with an equivalent function to the EWMH hint above
                    return root.getIntProperty(X11.XA_CARDINAL, "_WIN_WORKSPACE");
                } catch (X11Exception e1) {
                    throw new X11Exception("Cannot get current desktop properties (_NET_CURRENT_DESKTOP or _WIN_WORKSPACE property)", e1);
                }
            }
        }

        /**
         * Gets the names of all virtual desktops.
         * <p>
         * Note that the length of the returned array is not guaranteed to equal the value returned by
         * {@link #getDesktopCount()}; it may have more or fewer entries. If it has more entries than the desktop count,
         * the extra entries are being reserved for future use. If it has fewer entries than the desktop count, the
         * desktops not present in the array are unnamed.
         *
         * @return an array of the names of all virtual desktops
         * @throws X11Exception if an X11 window error occurred
         */
        public String[] getDesktopNames() throws X11Exception {
            Window root = getRootWindow();

            try {
                return root.getUtf8StringArrayProperty(getAtom("UTF8_STRING"), "_NET_DESKTOP_NAMES");
            } catch (X11Exception e) {
                try {
                    // GNOME winhint with an equivalent function to the EWMH hint above
                    return root.getStringArrayProperty(X11.XA_STRING, "_WIN_WORKSPACE_NAMES");
                } catch (X11Exception e1) {
                    throw new X11Exception("Cannot get desktop names properties (_NET_DESKTOP_NAMES or _WIN_WORKSPACE_NAMES)", e1);
                }
            }
        }

        /**
         * Gets an array containing representations of all virtual desktops.
         *
         * @return an array of all virtual desktops
         * @throws X11Exception if an X11 window error occurred
         */
        public Desktop[] getDesktops() throws X11Exception {
            Integer desktopCount = getDesktopCount();
            if (desktopCount == null) {
                return null;
            }

            String[] desktopNames = getDesktopNames();
            if (desktopNames == null) {
                return null;
            }

            Desktop[] desktops = new Desktop[desktopCount];
            for (int i = 0; i < desktops.length; i++) {
                desktops[i] = new Desktop(this, i, i < desktopNames.length ? desktopNames[i] : null);
            }

            return desktops;
        }

        /**
         * Switches to the specified desktop.
         *
         * @param index the index of the desktop to which to switch
         * @throws X11Exception if an X11 window error occurred
         * @see #getActiveDesktopNumber()
         */
        public void switchDesktop(int index) throws X11Exception {
            getRootWindow().clientMsg("_NET_CURRENT_DESKTOP", index, 0, 0, 0, 0);
        }

        /**
         * Sets whether the window manager is in "showing the desktop" mode.
         *
         * @param state {@code true} if the window manager should be in "showing the desktop" mode; {@code false} otherwise
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
         * @param keyName the name of the key symbol
         * @return the key symbol corresponding to the key name, or {@link X11#NoSymbol} if the specified name
         * does not match a valid key symbol
         */
        public X11.KeySym getKeySym(String keyName) {
            return x11.XStringToKeysym(keyName);
        }

        /**
         * Gets the key symbol corresponding to the specified keycode and the specified element of the keycode vector.
         *
         * @param keyCode the keycode
         * @param index the element of the keycode vector
         * @return the key symbol corresponding to the keycode and the element of the keycode vector,
         * or {@link X11#NoSymbol} if no symbol is defined
         */
        public X11.KeySym getKeySym(byte keyCode, int index) {
            return x11.XKeycodeToKeysym(x11Display, keyCode, index);
        }

        /**
         * Gets the keycode corresponding to the specified key symbol.
         *
         * @param keySym the key symbol that is to be searched for
         * @return the keycode corresponding to the key symbol, or 0 if the specified key symbol is not defined
         * for any keycode
         */
        public byte getKeyCode(X11.KeySym keySym) {
            return x11.XKeysymToKeycode(x11Display, keySym);
        }

        /**
         * Gets the keycode corresponding to the specified key name.
         *
         * @param keyName the name of the key symbol that is to be searched for
         * @return the keycode corresponding to the key name, or 0 if the specified key symbol is not defined
         * for any keycode
         */
        public byte getKeyCode(String keyName) {
            X11.KeySym keySym = getKeySym(keyName);
            if (keySym.toNative().equals(X11.NoSymbol)) {
                return 0;
            }
            return x11.XKeysymToKeycode(x11Display, keySym);
        }

        /**
         * Gets the name of the specified key symbol.
         *
         * @param keySym the key symbol whose name is to be returned
         * @return the name of the key symbol, or {@code null} if the specified key symbol is
         * not defined
         */
        public String getKeyName(X11.KeySym keySym) {
            return x11.XKeysymToString(keySym);
        }

        /**
         * Gets the name of the key symbol corresponding to the specified keycode and
         * the specified element of the keycode vector.
         *
         * @param keyCode the keycode
         * @param index the element of the keycode vector
         * @return the name of the key symbol corresponding to the keycode and the element of the keycode vector,
         * or {@code null} if no symbol is defined
         */
        public String getKeyName(byte keyCode, int index) {
            X11.KeySym keySym = getKeySym(keyCode, index);
            if (keySym == null) {
                return null;
            }
            return getKeyName(keySym);
        }

        /**
         * Gets a newly created modifier keymap that contains the keys being used as modifiers by this display.
         *
         * @return a newly created modifier keymap containing the keys being used as modifiers by this display
         */
        public ModifierKeymap getModifierKeymap() {
            X11.XModifierKeymapRef xModifierKeymapRef = x11.XGetModifierMapping(x11Display);
            try {
                return new ModifierKeymap(xModifierKeymapRef);
            } finally {
                x11.XFreeModifiermap(xModifierKeymapRef);
            }
        }

        /**
         * Sets the keycodes of the keys that are to be used as modifiers by this display.
         *
         * @param modifierKeymap the modifier keymap containing the keycodes of the keys
         * that are to be used as modifiers
         */
        public void setModifierKeymap(ModifierKeymap modifierKeymap) {
            X11.XModifierKeymapRef xModifierKeymapRef = null;
            try {
                xModifierKeymapRef = modifierKeymap.toXModifierKeymap();
                x11.XSetModifierMapping(x11Display, xModifierKeymapRef);
            } finally {
                if (xModifierKeymapRef != null) {
                    x11.XFreeModifiermap(xModifierKeymapRef);
                }
            }
        }
    }


    /**
     * Modifier keymap. The lists {@link #shift}, {@link #lock}, {@link #control},
     * {@link #mod1}, {@link #mod2}, {@link #mod3}, {@link #mod4}, and {@link #mod5}
     * contain the keycodes as Byte objects. You can directly access these lists to
     * read, replace, remove, or insert new keycodes to these modifiers.
     * To apply a new modifier keymap to a display, use
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
         * Creates a modifier keymap and reads the modifiers from the specified {@code XModifierKeymapRef}.
         *
         * @param xModifierKeymapRef the {@code XModifierKeymapRef} from which to read the modifiers
         */
        public ModifierKeymap(X11.XModifierKeymapRef xModifierKeymapRef) {
            fromXModifierKeymap(xModifierKeymapRef);
        }

        /**
         * Reads all modifiers from the specified {@code XModifierKeymapRef}.
         *
         * @param xModifierKeymapRef the {@code XModifierKeymapRef} from which to read the modifiers
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
         * Gets an {@code XModifierKeymapRef} corresponding to this object.
         *
         * @return an {@code XModifierKeymapRef} corresponding to this object
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

            X11.XModifierKeymapRef xModifierKeymapRef = null;
            try {
                xModifierKeymapRef = x11.XNewModifiermap(count);
                xModifierKeymapRef.modifiermap.write(0, keys, 0, keys.length);
                return xModifierKeymapRef;
            } catch (Throwable e) {
                if (xModifierKeymapRef != null) {
                    x11.XFreeModifiermap(xModifierKeymapRef);
                }
                throw e;
            }
        }

        /**
         * Gets an array containing all modifier lists, in the order
         * {@code [shift, lock, control, mod1, mod2, mod3, mod4, mod5]}.
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
     * Represents a virtual desktop.
     */
    public static class Desktop {
        /**
         * The display used to retrieve this virtual desktop's information.
         */
        public X.Display display;
        /**
         * The ID of this virtual desktop.
         */
        public int id;
        /**
         * The name of this virtual desktop.
         */
        public String name;

        /**
         * Creates a new virtual desktop.
         *
         * @param display the display used to retrieve this virtual desktop's information
         * @param id the ID of this virtual desktop
         * @param name the name of this virtual desktop
         */
        public Desktop(Display display, int id, String name) {
            this.display = display;
            this.id = id;
            this.name = name;
        }
    }


    /**
     * X Window.
     */
    public static class Window {
        /**
         * The display where this window is allocated.
         */
        private final X.Display display;

        /**
         * The native X11 window.
         *
         * @see #getX11Window()
         */
        private final X11.Window x11Window;

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
         * @return the ID of this window
         */
        public int getID() {
            return x11Window.intValue();
        }

        /**
         * Gets the name of this window.
         *
         * @return the name of this window
         * @throws X11Exception if an X11 window error occurred
         */
        public String getTitle() throws X11Exception {
            try {
                return getUtf8StringProperty(display.getAtom("UTF8_STRING"), "_NET_WM_NAME");
            } catch (X11Exception e) {
                return getStringProperty(X11.XA_STRING, X11.XA_WM_NAME);
            }
        }

        /**
         * Gets an array of hints describing the state of this window.
         *
         * @return an array of hints describing the state of this window
         * @throws X11Exception if an X11 window error occurred
         */
        public int[] getState() throws X11Exception {
            return getIntArrayProperty(X11.XA_ATOM, "_NET_WM_STATE");
        }

        /**
         * Gets an array of atoms indicating the functional type of this window.
         *
         * @return an array of atoms indicating the functional type of this window
         * @throws X11Exception if an X11 window error occurred
         */
        public int[] getType() throws X11Exception {
            return getIntArrayProperty(X11.XA_ATOM, "_NET_WM_WINDOW_TYPE");
        }

        /**
         * Gets the extents of this window's frame, in the order {@code [left, right, top, bottom]}.
         *
         * @return the extents of this window's frame
         * @throws X11Exception if an X11 window error occurred
         */
        public int[] getFrameExtents() throws X11Exception {
            return getIntArrayProperty(X11.XA_CARDINAL, "_NET_FRAME_EXTENTS");
        }

        /**
         * Gets the class of this window, in the format {@code [resource_name, resource_class]}.
         *
         * @return the class of this window
         * @throws X11Exception if an X11 window error occurred
         */
        public String[] getWindowClass() throws X11Exception {
            return getStringArrayProperty(X11.XA_STRING, X11.XA_WM_CLASS);
        }

        /**
         * Gets the process ID of the client that owns this window.
         *
         * @return the process ID of the client that owns this window
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
         * @see #moveToDesktop(int)
         */
        public Integer getDesktop() throws X11Exception {
            try {
                return getIntProperty(X11.XA_CARDINAL, "_NET_WM_DESKTOP");
            } catch (X11Exception e) {
                // GNOME winhint with an equivalent function to the EWMH hint above
                return getIntProperty(X11.XA_CARDINAL, "_WIN_WORKSPACE");
            }
        }

        /**
         * Gets the name of the client machine of this window.
         *
         * @return the name of the client machine of this window
         * @throws X11Exception if an X11 window error occurred
         */
        public String getMachine() throws X11Exception {
            return getStringProperty(X11.XA_STRING, X11.XA_WM_CLIENT_MACHINE);
        }

        /**
         * Gets the current attributes of this window.
         *
         * @return the current attributes of this window
         */
        public X11.XWindowAttributes getXWindowAttributes() {
            X11.XWindowAttributes xwa = new X11.XWindowAttributes();
            x11.XGetWindowAttributes(display.x11Display, x11Window, xwa);

            return xwa;
        }

        /**
         * Gets the current geometry of this window.
         *
         * @return the current geometry of this window
         */
        public Geometry getGeometry() {
            X11.WindowByReference junkRoot = new X11.WindowByReference();
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
         * Gets the current bounding box of this window.
         *
         * @return the current bounding box of this window
         */
        public Rectangle getBounds() {
            X11.WindowByReference junkRoot = new X11.WindowByReference();
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
         * Activates this window and brings it to the front.
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
         * @throws X11Exception if an X11 window error occurred
         * @see #getDesktop()
         */
        public void moveToDesktop(int desktopNr) throws X11Exception {
            clientMsg("_NET_WM_DESKTOP", desktopNr, 0, 0, 0, 0);
        }

        /**
         * Selects the input events for which to listen.
         *
         * @param eventMask an event mask representing the events for which to listen
         */
        public void selectInput(int eventMask) {
            // XSelectInput always returns 1; no point returning that here
            x11.XSelectInput(display.x11Display, x11Window, new NativeLong(eventMask));
        }

        /**
         * Copies the next event in the queue into the specified {@code XEvent} structure.
         *
         * @param event the {@code XEvent} into which to copy the next event
         */
        public void nextEvent(X11.XEvent event) {
            // XNextEvent always returns 0; no point returning that here
            x11.XNextEvent(display.x11Display, event);
        }

        /**
         * Sends the specified event to this window.
         *
         * @param eventMask the event mask to pass to the event. If this is empty, the event is sent
         * to the client that created this window. Otherwise, the event is propagated to the closest ancestor
         * of the window for which some client has selected a type specified in this mask and for which no
         * intervening window has that type in its
         * {@link X11.XWindowAttributes#do_not_propagate_mask do_not_propagate_mask}.
         * @param event the type of event to send
         * @return zero if the conversion to wire protocol format failed; nonzero otherwise
         */
        public int sendEvent(int eventMask, X11.XEvent event) {
            return x11.XSendEvent(display.x11Display, x11Window, 1, new NativeLong(eventMask), event);
        }

        /**
         * Closes this window gracefully.
         *
         * @throws X11Exception if an X11 window error occurred
         */
        public void close() throws X11Exception {
            clientMsg("_NET_CLOSE_WINDOW", 0, 0, 0, 0, 0);
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
            byte[] property = getProperty(xaPropType, xaPropName, 32);
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
            byte[] property = getProperty(xaPropType, xaPropName, 32);
            if (property == null) {
                return null;
            }
            if (property.length == 0) {
                return new int[0];
            }
            // NativeLong.SIZE is used in getProperty() to represent the number of bytes used for each entry
            // when the return type is 32 bits
            int[] array = new int[property.length / NativeLong.SIZE];
            for (int i = 0; i < array.length; i++) {
                /* On 64-bit systems, the longs returned by XGetWindowProperty() are just integers
                that are padded in the upper four bytes, so we only need to read the lower four bytes.
                (From https://linux.die.net/man/3/xgetwindowproperty) */
                array[i] = bytesToInt(property, i * NativeLong.SIZE);
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
            byte[] property = getProperty(xaPropType, xaPropName, 32);
            if (property == null) {
                return null;
            }
            if (X11.Window.SIZE == 8) {
                return new Window(display, new X11.Window(bytesToLong(property)));
            } else {
                return new Window(display, new X11.Window(bytesToInt(property)));
            }
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
         * Gets the value of the specified property as a window array.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as a window array,
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public Window[] getWindowArrayProperty(X11.Atom xaPropType, X11.Atom xaPropName) throws X11Exception {
            byte[] property = getProperty(xaPropType, xaPropName, 32);
            if (property == null) {
                return null;
            }
            if (property.length == 0) {
                return new Window[0];
            }
            Window[] array = new Window[property.length / X11.Window.SIZE];
            for (int i = 0; i < array.length; i++) {
                if (X11.Window.SIZE == 8) {
                    array[i] = new Window(display, new X11.Window(bytesToLong(property, i * X11.Window.SIZE)));
                } else {
                    array[i] = new Window(display, new X11.Window(bytesToInt(property, i * X11.Window.SIZE)));
                }
            }
            return array;
        }

        /**
         * Gets the value of the specified property as a window array.
         *
         * @param xaPropType the property type
         * @param xaPropName the property name
         * @return the value of the property as a window array,
         * or {@code null} if the specified property does not exist for this window
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public Window[] getWindowArrayProperty(X11.Atom xaPropType, String xaPropName) throws X11Exception {
            return getWindowArrayProperty(xaPropType, display.getAtom(xaPropName));
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
            byte[] bytesOrig = getProperty(xaPropType, xaPropName, 8);

            if (bytesOrig == null) {
                return null;
            }

            if (bytesOrig.length == 0) {
                return bytesOrig;
            }

            // Get index of first null terminator
            int i;
            for (i = 0; i < bytesOrig.length; i++) {
                if (bytesOrig[i] == '\0') {
                    break;
                }
            }

            byte[] bytesDest;

            if (i < bytesOrig.length - 1) {
                // Create new array containing everything from the original array up to and including the null terminator
                bytesDest = new byte[i + 1];
                System.arraycopy(bytesOrig, 0, bytesDest, 0, i + 1);
            } else {
                // If the original array contained no null terminator, return the original array
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
        public byte[] getNullReplacedProperty(X11.Atom xaPropType, X11.Atom xaPropName) throws X11Exception {
            byte[] bytes = getProperty(xaPropType, xaPropName, 8);

            if (bytes == null) {
                return null;
            }

            if (bytes.length > 0) {
                // Replace all null terminators with '.'
                for (int i = 0; i < bytes.length; i++) {
                    if (bytes[i] == '\0') {
                        bytes[i] = '.';
                    }
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
        public byte[] getNullReplacedProperty(X11.Atom xaPropType, String xaPropName) throws X11Exception {
            return getNullReplacedProperty(xaPropType, display.getAtom(xaPropName));
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
            byte[] property = getNullReplacedProperty(xaPropType, xaPropName);
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
            return getStringProperty(xaPropType, display.getAtom(xaPropName));
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
            byte[] property = getProperty(xaPropType, xaPropName, 8);
            if (property == null) {
                return null;
            }
            if (property.length == 0) {
                return new String[0];
            }
            int terminatorCount = (int) IntStream.range(0, property.length).filter(i -> property[i] == '\0').count();
            String[] array = new String[terminatorCount];
            // startIndex: the index of the first byte of the next string in the property byte array
            // curString: the index of the next string to be updated in the returned array
            for (int i = 0, startIndex = 0, curString = 0; i < property.length && curString < array.length; i++) {
                if (property[i] == '\0') {
                    int length = i - startIndex;
                    array[curString] = new String(property, startIndex, length);
                    // Update the start index to be one byte after the last null terminator
                    startIndex = i + 1;
                    curString++;
                }
            }

            return array;
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
            return getStringArrayProperty(xaPropType, display.getAtom(xaPropName));
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
            byte[] property = getNullReplacedProperty(xaPropType, xaPropName);
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
            byte[] property = getProperty(xaPropType, xaPropName, 8);
            if (property == null) {
                return null;
            }
            if (property.length == 0) {
                return new String[0];
            }
            int terminatorCount = (int) IntStream.range(0, property.length).filter(i -> property[i] == '\0').count();
            String[] array = new String[terminatorCount];
            // startIndex: the index of the first byte of the next string in the property byte array
            // curString: the index of the next string to be updated in the returned array
            for (int i = 0, startIndex = 0, curString = 0; i < property.length && curString < array.length; i++) {
                if (property[i] == '\0') {
                    int length = i - startIndex;
                    array[curString] = new String(property, startIndex, length, StandardCharsets.UTF_8);
                    // Update the start index to be one byte after the last null terminator
                    startIndex = i + 1;
                    curString++;
                }
            }

            return array;
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
         * @param xaPropType the property type. If this is {@link X11#AnyPropertyType},
         * the property type will not be checked against the actual property type.
         * @param xaPropName the property name
         * @param format the format of the property. Can be 0 for any format, 8 for native char arrays,
         * 16 for native short arrays, and 32 for native long arrays.
         * @return the value of the property as a byte array,
         * or {@code null} if the specified property does not exist for this window
         * @throws IllegalArgumentException if {@code xaPropType} or {@code xaPropName} are {@code null};
         * or if {@code format} is not 0, 8, 16, or 32
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public byte[] getProperty(X11.Atom xaPropType, X11.Atom xaPropName, int format) throws X11Exception {
            if (xaPropType == null) {
                throw new IllegalArgumentException("Property type cannot be null");
            }

            if (xaPropName == null) {
                throw new IllegalArgumentException("Property name cannot be null");
            }

            if (format != 0 && format != 8 && format != 16 && format != 32) {
                throw new IllegalArgumentException("Invalid format parameter: " + format);
            }

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
                int retFormat = retFormatRef.getValue();

                if (xaRetType == null || retFormat == 0) {
                    // The specified property does not exist for the specified window.
                    return null;
                }

                if (!xaPropType.toNative().equals(X11.AnyPropertyType) && !xaRetType.equals(xaPropType)) {
                    // Requested return type does not match actual return type
                    String requestedTypeName = x11.XGetAtomName(display.x11Display, xaPropType);
                    String actualTypeName = x11.XGetAtomName(display.x11Display, xaRetType);
                    throw new X11Exception("Requested property type " + requestedTypeName +
                            " does not match actual property type " + actualTypeName);
                }

                if (format != 0 && format != retFormat) {
                    throw new X11Exception("Requested format " + format +
                            " does not match actual format " + retFormat);
                }

                int nBytes = switch (retFormat) {
                    // Returned data is a long array; native long size is 4 bytes on 32-bit and 8 bytes on 64-bit
                    case 32 -> NativeLong.SIZE;
                    // Returned data is a short array; native short size is always 2 bytes
                    case 16 -> 2;
                    // Returned data is a char array; native char size is always 1 byte
                    case 8 -> 1;
                    default -> throw new X11Exception("Invalid return format: " + retFormat);
                };
                long retNItems = retNItemsRef.getValue().longValue();
                int length = Math.min((int) retNItems * nBytes, MAX_PROPERTY_VALUE_LEN);

                Pointer retProp = retPropRef.getValue();
                return retProp.getByteArray(0, length);
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
         * @param xaPropType the property type. If this is {@link X11#AnyPropertyType},
         * the property type will not be checked against the actual property type.
         * @param xaPropName the property name
         * @param format the format of the property. Can be 0 for any format, 8 for native char arrays,
         * 16 for native short arrays, and 32 for native long arrays.
         * @return the value of the property as a byte array,
         * or {@code null} if the specified property does not exist for this window
         * @throws IllegalArgumentException if {@code xaPropType} or {@code xaPropName} are {@code null};
         * or if {@code format} is not 0, 8, 16, or 32
         * @throws X11Exception if the operation failed, or if the specified type
         * does not match the actual type of the property
         */
        public byte[] getProperty(X11.Atom xaPropType, String xaPropName, int format) throws X11Exception {
            return getProperty(xaPropType, display.getAtom(xaPropName), format);
        }

        /**
         * Sends a client message event with the specified message type and data to this window.
         *
         * @param msg the name of the atom that describes the message type of the event
         * @param data0 the first parameter to pass to the event
         * @param data1 the second parameter to pass to the event
         * @param data2 the third parameter to pass to the event
         * @param data3 the fourth parameter to pass to the event
         * @param data4 the fifth parameter to pass to the event
         * @throws X11Exception if the event failed to be sent
         */
        public void clientMsg(String msg, int data0, int data1, int data2, int data3, int data4) throws X11Exception {
            clientMsg(
                    msg,
                    new NativeLong(data0),
                    new NativeLong(data1),
                    new NativeLong(data2),
                    new NativeLong(data3),
                    new NativeLong(data4)
            );
        }

        /**
         * Sends a client message event with the specified message type and data to this window.
         *
         * @param msg the name of the atom that describes the message type of the event
         * @param data0 the first parameter to pass to the event
         * @param data1 the second parameter to pass to the event
         * @param data2 the third parameter to pass to the event
         * @param data3 the fourth parameter to pass to the event
         * @param data4 the fifth parameter to pass to the event
         * @throws X11Exception if the event failed to be sent
         */
        public void clientMsg(String msg, NativeLong data0, NativeLong data1, NativeLong data2, NativeLong data3, NativeLong data4) throws X11Exception {
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
            }
        }

        /**
         * Gets the child windows of this window, in stacking order from bottommost to topmost.
         *
         * @return the child windows of this window, or {@code null} if there are no children
         * @throws X11Exception if the operation failed
         */
        public Window[] getSubwindows() throws X11Exception {
            X11.WindowByReference rootRef = new X11.WindowByReference();
            X11.WindowByReference parentRef = new X11.WindowByReference();
            PointerByReference childrenRef = new PointerByReference();
            IntByReference childCountRef = new IntByReference();

            if (x11.XQueryTree(display.x11Display, x11Window, rootRef, parentRef, childrenRef, childCountRef) == 0) {
                throw new X11Exception("Cannot query subwindows");
            }

            try {
                Pointer children = childrenRef.getValue();
                int childCount = childCountRef.getValue();

                if (childCount == 0) {
                    return null;
                }

                Window[] retVal = new Window[childCount];
                // Depending on if we're running on 64-bit or 32-bit systems,
                // the Window size may be different; we need to make sure that
                // we get the data properly no matter what
                if (X11.Window.SIZE == 8) {
                    long[] windows = children.getLongArray(0, childCount);
                    for (int x = 0; x < retVal.length; x++) {
                        retVal[x] = new Window(display, new X11.Window(windows[x]));
                    }
                } else {
                    int[] windows = children.getIntArray(0, childCount);
                    for (int x = 0; x < retVal.length; x++) {
                        retVal[x] = new Window(display, new X11.Window(windows[x]));
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

        @Override
        public String toString() {
            return x11Window.toString();
        }

        /**
         * Represents a window's bounding box, border width, and bit depth.
         *
         * @see #getGeometry()
         */
        public static class Geometry {
            /**
             * The X-coordinate of the upper-left outer corner of the window, relative to its parent's origin.
             */
            public int x;
            /**
             * The Y-coordinate of the upper-left outer corner of the window, relative to its parent's origin.
             */
            public int y;
            /**
             * The width of the inside of the window, not including the border.
             */
            public int width;
            /**
             * The height of the inside of the window, not including the border.
             */
            public int height;
            /**
             * The border width of the window.
             */
            public int borderWidth;
            /**
             * The bit depth (bits per pixel) of the window.
             */
            public int depth;

            /**
             * Creates a new Geometry object.
             *
             * @param x the X-coordinate of the upper-left outer corner of the window, relative to its parent's origin
             * @param y the Y-coordinate of the upper-left outer corner of the window, relative to its parent's origin
             * @param width the width of the inside of the window, not including the border
             * @param height the height of the inside of the window, not including the border
             * @param borderWidth the border width of the window
             * @param depth the bit depth (bits per pixel) of the window
             */
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
