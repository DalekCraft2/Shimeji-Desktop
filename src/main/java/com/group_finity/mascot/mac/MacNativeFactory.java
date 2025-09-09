/*
 * Created by nonowarn
 * https://github.com/nonowarn/shimeji4mac
 */
package com.group_finity.mascot.mac;

import com.group_finity.mascot.NativeFactory;
import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.generic.GenericNativeFactory;
import com.group_finity.mascot.image.TranslucentWindow;

/**
 * @author nonowarn
 */
public class MacNativeFactory extends NativeFactory {
    private final NativeFactory delegate = new GenericNativeFactory();
    private final Environment environment = new MacEnvironment();

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public TranslucentWindow newTransparentWindow() {
        return new MacTranslucentWindow(delegate);
    }
}
