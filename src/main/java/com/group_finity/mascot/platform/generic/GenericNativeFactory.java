package com.group_finity.mascot.platform.generic;

import com.group_finity.mascot.platform.NativeFactory;
import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.platform.TranslucentWindow;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class GenericNativeFactory extends NativeFactory {
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
