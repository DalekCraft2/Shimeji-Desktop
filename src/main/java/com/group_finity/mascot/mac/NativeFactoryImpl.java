package com.group_finity.mascot.mac;

import com.group_finity.mascot.NativeFactory;
import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;

import java.awt.image.BufferedImage;

/**
 * @author nonowarn
 */
public class NativeFactoryImpl extends NativeFactory {
    private NativeFactory delegate = new com.group_finity.mascot.generic.NativeFactoryImpl();
    private Environment environment = new MacEnvironment();

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public NativeImage newNativeImage(final BufferedImage src) {
        return delegate.newNativeImage(src);
    }

    @Override
    public TranslucentWindow newTransparentWindow() {
        return new MacTranslucentWindow(delegate);
    }
}
