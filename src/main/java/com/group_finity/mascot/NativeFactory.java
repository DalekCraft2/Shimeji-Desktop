package com.group_finity.mascot;

import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.sun.jna.Platform;

import java.awt.image.BufferedImage;

public abstract class NativeFactory {
    private static NativeFactory instance;

    static {
        resetInstance();
    }

    public static NativeFactory getInstance() {
        return instance;
    }

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

    public abstract Environment getEnvironment();

    public abstract NativeImage newNativeImage(BufferedImage src);

    public abstract TranslucentWindow newTransparentWindow();
}
