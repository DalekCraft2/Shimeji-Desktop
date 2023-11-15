package com.group_finity.mascot;

import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.sun.jna.Platform;

import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

public abstract class NativeFactory {

    private static final NativeFactory instance;

    static {
        Class<? extends NativeFactory> impl;
        if (Platform.isWindows()) {
            impl = com.group_finity.mascot.win.NativeFactoryImpl.class;
        } else if (Platform.isMac()) {
            impl = com.group_finity.mascot.mac.NativeFactoryImpl.class;
        } else {
            impl = com.group_finity.mascot.generic.NativeFactoryImpl.class;
        }

        try {
            instance = impl.getDeclaredConstructor().newInstance();
        } catch (final InstantiationException | IllegalAccessException |
                       InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static NativeFactory getInstance() {
        return instance;
    }

    public abstract Environment getEnvironment();

    public abstract NativeImage newNativeImage(BufferedImage src);

    public abstract TranslucentWindow newTransparentWindow();
}
