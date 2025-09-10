package com.group_finity.mascot;

import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.image.TranslucentWindow;
import com.group_finity.mascot.mac.MacNativeFactory;
import com.group_finity.mascot.virtual.VirtualNativeFactory;
import com.group_finity.mascot.win.WindowsNativeFactory;
import com.group_finity.mascot.x11.X11NativeFactory;
import com.sun.jna.Platform;

/**
 * Provides access to the native environment.
 * {@link #getInstance()} returns an instance of a Windows, Mac, Linux (X11), or general-purpose subclass depending on the execution environment.
 *
 * @author Yuki Yamada
 */
public abstract class NativeFactory {
    private static NativeFactory instance;

    static {
        resetInstance();
    }

    /**
     * Obtains an instance of the subclass according to the execution environment.
     *
     * @return the environment-specific subclass
     */
    public static NativeFactory getInstance() {
        return instance;
    }

    /**
     * Creates an instance of the subclass.
     */
    public static void resetInstance() {
        String environment = Main.getInstance().getProperties().getProperty("Environment", "generic");

        if (environment.equals("generic")) {
            if (Platform.isWindows()) {
                instance = new WindowsNativeFactory();
            } else if (Platform.isMac()) {
                instance = new MacNativeFactory();
            } else if (/* Platform.isLinux() */ Platform.isX11()) {
                // Because Linux uses X11, this functions as the Linux support.
                instance = new X11NativeFactory();
            }
        } else if (environment.equals("virtual")) {
            instance = new VirtualNativeFactory();
        }
    }

    /**
     * Gets the {@link Environment} object.
     *
     * @return the {@link Environment} object
     */
    public abstract Environment getEnvironment();

    /**
     * Creates a window that can be displayed translucently.
     *
     * @return the new window
     */
    public abstract TranslucentWindow newTranslucentWindow();
}
