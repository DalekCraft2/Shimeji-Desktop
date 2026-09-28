/*
 * Created by nonowarn
 * https://github.com/nonowarn/shimeji4mac
 */
package com.group_finity.mascot.platform.mac;

import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.platform.NativeFactory;
import com.group_finity.mascot.platform.TranslucentWindow;

/**
 * An implementation of {@link NativeFactory} that supports accessing the
 * macOS native environment and creating translucent windows that are specialized for macOS.
 *
 * @author nonowarn
 */
public class MacNativeFactory extends NativeFactory {
    /**
     * The macOS environment instance.
     *
     * @see #getEnvironment()
     */
    private final Environment environment = new MacEnvironment();

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public TranslucentWindow newTranslucentWindow() {
        return new MacTranslucentWindow();
    }
}
