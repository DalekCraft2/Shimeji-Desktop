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
 * The {@code NativeFactory} instance can be initialized with {@link #resetInstance()}, and accessed with
 * {@link #getInstance()}. Based on the execution environment, {@code getInstance()} may return an instance that
 * is optimized for Windows, macOS, or Linux (X11), and will default to a general-purpose instance otherwise.
 * If the user has enabled windowed mode, {@code getInstance()} will return an instance that can access the
 * virtual environment.
 *
 * @author Yuki Yamada
 */
public abstract class NativeFactory {
    /**
     * The current {@code NativeFactory} instance, as set by {@link #resetInstance()}.
     *
     * @see #getInstance()
     * @see #resetInstance()
     */
    private static NativeFactory instance;

    static {
        resetInstance();
    }

    /**
     * Gets the current {@code NativeFactory} instance, as set by {@link #resetInstance()}.
     * Based on the execution environment, the returned instance will be optimized for Windows, macOS, or
     * Linux (X11), and will default to a general-purpose instance in the case of other environments.
     * If the user has enabled windowed mode, the returned instance will be able to access the virtual environment.
     *
     * @return the {@code NativeFactory} instance for the current platform
     */
    public static NativeFactory getInstance() {
        return instance;
    }

    /**
     * Creates an instance of a {@code NativeFactory} subclass based on the current platform and user settings,
     * or recreates the instance if an instance had already been created.
     * <p>
     * Based on the execution environment, the new instance will be optimized for Windows, macOS, or
     * Linux (X11), and will default to a general-purpose instance in the case of other environments.
     * If the user has enabled windowed mode, the new instance will be able to access the virtual environment.
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
     * Gets the {@link Environment} instance for the current platform.
     *
     * @return the {@link Environment} instance
     */
    public abstract Environment getEnvironment();

    /**
     * Creates a window that can be displayed translucently on the current platform.
     *
     * @return a new translucent window instance
     */
    public abstract TranslucentWindow newTranslucentWindow();
}
