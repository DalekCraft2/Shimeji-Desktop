package com.group_finity.mascot.generic;

import com.group_finity.mascot.NativeFactory;
import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.image.TranslucentWindow;

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
    public TranslucentWindow newTransparentWindow() {
        return new GenericTranslucentWindow();
    }
}
