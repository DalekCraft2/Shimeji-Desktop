package com.group_finity.mascot.platform.virtual;

import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.platform.NativeFactory;
import com.group_finity.mascot.platform.TranslucentWindow;

/**
 * An implementation of {@link NativeFactory} that supports accessing a custom, Swing-based virtual environment
 * and creating translucent "windows" for that environment.
 *
 * @author Kilkakon
 * @since 1.0.20
 */
public class VirtualNativeFactory extends NativeFactory {
    /**
     * The virtual environment instance.
     *
     * @see #getEnvironment()
     */
    private final VirtualEnvironment environment = new VirtualEnvironment();

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public TranslucentWindow newTranslucentWindow() {
        VirtualTranslucentPanel panel = new VirtualTranslucentPanel();
        environment.addMascot(panel);
        return panel;
    }
}
