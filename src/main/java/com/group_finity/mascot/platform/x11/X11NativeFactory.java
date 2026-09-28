/*
 * Created by asdfman
 * https://github.com/asdfman/linux-shimeji
 */
package com.group_finity.mascot.platform.x11;

import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.platform.NativeFactory;
import com.group_finity.mascot.platform.TranslucentWindow;

/**
 * An implementation of {@link NativeFactory} that supports accessing the
 * X11 native environment and creating translucent windows that are specialized for X11.
 *
 * @author asdfman
 */
public class X11NativeFactory extends NativeFactory {
    /**
     * The X11 environment instance.
     *
     * @see #getEnvironment()
     */
    private final X11Environment environment = new X11Environment();

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public TranslucentWindow newTranslucentWindow() {
        return new X11TranslucentWindow(environment.getDisplay().getX11Display(), environment.getDockValue());
    }
}
