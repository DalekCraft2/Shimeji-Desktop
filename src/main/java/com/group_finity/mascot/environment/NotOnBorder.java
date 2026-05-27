package com.group_finity.mascot.environment;

import java.awt.*;

/**
 * Represents a nonexistent {@link Border}.
 * Certain script-accessible methods such as {@link MascotEnvironment#getWall()} may return this
 * as an alternative to returning {@code null}, to avoid NullPointerExceptions in the event
 * that a mascot script expects a non-{@code null} return value.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class NotOnBorder implements Border {

    /**
     * The singleton instance of {@code NotOnBorder}.
     */
    public static final NotOnBorder INSTANCE = new NotOnBorder();

    private NotOnBorder() {
    }

    @Override
    public boolean isOn(final Point location) {
        return false;
    }

    @Override
    public Point move(final Point location) {
        return location;
    }
}
