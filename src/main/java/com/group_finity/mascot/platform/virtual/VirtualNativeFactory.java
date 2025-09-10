package com.group_finity.mascot.platform.virtual;

import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.platform.NativeFactory;
import com.group_finity.mascot.platform.TranslucentWindow;

/**
 * Virtual desktop factory.
 *
 * @author Kilkakon
 * @since 1.0.20
 */
public class VirtualNativeFactory extends NativeFactory {
    private final VirtualEnvironment environment = new VirtualEnvironment();

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public TranslucentWindow newTranslucentWindow() {
        VirtualTranslucentPanel panel = new VirtualTranslucentPanel();
        environment.addShimeji(panel);
        return panel;
    }
}
