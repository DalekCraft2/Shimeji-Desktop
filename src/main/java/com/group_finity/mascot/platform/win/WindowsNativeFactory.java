package com.group_finity.mascot.platform.win;

import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.platform.NativeFactory;
import com.group_finity.mascot.platform.TranslucentWindow;

/**
 * An implementation of {@link NativeFactory} that supports accessing the
 * Windows native environment and creating translucent windows that are specialized for Windows.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class WindowsNativeFactory extends NativeFactory {
    /**
     * The Windows environment instance.
     *
     * @see #getEnvironment()
     */
    private final Environment environment = new WindowsEnvironment();

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public TranslucentWindow newTranslucentWindow() {
        return new WindowsTranslucentWindow();
    }
}
