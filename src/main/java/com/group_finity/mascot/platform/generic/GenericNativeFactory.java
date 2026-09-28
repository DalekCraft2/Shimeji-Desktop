package com.group_finity.mascot.platform.generic;

import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.platform.NativeFactory;
import com.group_finity.mascot.platform.TranslucentWindow;

/**
 * A cross-platform implementation of {@link NativeFactory} that supports accessing basic information about the
 * native environment and creating translucent windows.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class GenericNativeFactory extends NativeFactory {
    /**
     * The generic environment instance.
     *
     * @see #getEnvironment()
     */
    private final Environment environment = new GenericEnvironment();

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public TranslucentWindow newTranslucentWindow() {
        return new GenericTranslucentWindow();
    }
}
