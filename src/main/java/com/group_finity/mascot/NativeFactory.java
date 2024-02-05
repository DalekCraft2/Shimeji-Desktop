package com.group_finity.mascot;

import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.sun.jna.Platform;

import java.awt.image.BufferedImage;

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
                instance = new com.group_finity.mascot.win.NativeFactoryImpl();
            } else if (Platform.isMac()) {
                instance = new com.group_finity.mascot.mac.NativeFactoryImpl();
            } else if (/* Platform.isLinux() */ Platform.isX11()) {
                // Because Linux uses X11, this functions as the Linux support.
                instance = new com.group_finity.mascot.x11.NativeFactoryImpl();
            }
        } else if (environment.equals("virtual")) {
            instance = new com.group_finity.mascot.virtual.NativeFactoryImpl();
        }
    }

    /**
     * Gets the {@link Environment} object.
     *
     * @return the {@link Environment} object
     */
    public abstract Environment getEnvironment();

    /**
     * Creates a {@link NativeImage} with the specified {@link BufferedImage}.
     * This image can be used for masking {@link TranslucentWindow}.
     *
     * @param src the image to use to create the {@link NativeImage}
     * @return the new native image
     */
    public abstract NativeImage newNativeImage(BufferedImage src);

    /**
     * Creates a window that can be displayed semi-transparently.
     *
     * @return the new window
     */
    public abstract TranslucentWindow newTransparentWindow();
}
