/*
 * Created by asdfman and Ygarr
 * https://github.com/asdfman/linux-shimeji
 * https://github.com/Ygarr/linux-shimeji
 */
package com.group_finity.mascot.x11;

import com.group_finity.mascot.NativeFactory;
import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;

import java.awt.image.BufferedImage;

/**
 * @author asdfman
 */
public class NativeFactoryImpl extends NativeFactory {

    private Environment environment = new X11Environment();

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public NativeImage newNativeImage(final BufferedImage src) {
        return new X11NativeImage(src);
    }

    @Override
    public TranslucentWindow newTransparentWindow() {
        return new X11TranslucentWindow();
    }

}
