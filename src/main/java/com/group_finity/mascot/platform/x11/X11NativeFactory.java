/*
 * Created by asdfman
 * https://github.com/asdfman/linux-shimeji
 */
package com.group_finity.mascot.platform.x11;

import com.group_finity.mascot.platform.NativeFactory;
import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.platform.TranslucentWindow;

/**
 * @author asdfman
 */
public class X11NativeFactory extends NativeFactory {
    private final Environment environment = new X11Environment();

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public TranslucentWindow newTranslucentWindow() {
        return new X11TranslucentWindow();
    }
}
