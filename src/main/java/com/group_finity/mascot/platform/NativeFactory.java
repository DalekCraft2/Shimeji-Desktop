package com.group_finity.mascot.platform;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.platform.generic.GenericNativeFactory;
import com.group_finity.mascot.platform.mac.MacNativeFactory;
import com.group_finity.mascot.platform.virtual.VirtualNativeFactory;
import com.group_finity.mascot.platform.win.WindowsNativeFactory;
import com.group_finity.mascot.platform.x11.X11NativeFactory;
import com.sun.jna.Platform;

/**
 * Provides utilities for accessing the native environment and creating translucent windows that are
 * optimized for the current environment.
 * <p>
 * {@link #getInstance()} returns an instance of a Windows, Mac, Linux (X11), or general-purpose subclass
 * depending on the execution environment.
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
     * Creates an instance of a {@code NativeFactory} subclass based on the current platform and user settings,
     * or recreates the instance if an instance had already been created.
     * <p>
     * Before resetting the {@code NativeFactory} instance, the {@link Environment} should be disposed
     * by calling {@link Environment#dispose()}.
     */
    public static void resetInstance() {
        boolean windowedMode = Main.getInstance().getSettings().windowedMode;

        if (windowedMode) {
            instance = new VirtualNativeFactory();
        } else {
            if (Platform.isWindows()) {
                instance = new WindowsNativeFactory();
            } else if (Platform.isMac()) {
                instance = new MacNativeFactory();
            } else if (Platform.isX11()) {
                instance = new X11NativeFactory();
            } else {
                instance = new GenericNativeFactory();
            }
        }
    }

    /**
     * Gets the {@link Environment} instance.
     *
     * @return the {@link Environment} instance
     */
    public abstract Environment getEnvironment();

    /**
     * Creates a window that can be displayed translucently.
     *
     * @return the new window
     */
    public abstract TranslucentWindow newTranslucentWindow();
}
