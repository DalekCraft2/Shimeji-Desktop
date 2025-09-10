package com.group_finity.mascot.win;

import com.group_finity.mascot.NativeFactory;
import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.image.TranslucentWindow;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class WindowsNativeFactory extends NativeFactory {
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
