package com.group_finity.mascot.environment;

import com.group_finity.mascot.Mascot;

import java.awt.*;

/**
 * Represents a moveable surface with which {@link Mascot Mascots} can interact.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public interface Border {

    /**
     * Returns whether the given {@link Point} is on this border.
     *
     * @param location the {@link Point} to check
     * @return whether the given {@link Point} is on this border
     */
    boolean isOn(Point location);

    /**
     * Moves the given {@link Point} to match any changes to the position and size of this border since the last frame.
     * <p>
     * For example, if this border has moved to the right by X units, then {@code location} will be moved to the right
     * by X units as well. If this border is a {@linkplain FloorCeiling floor or ceiling} and has been scaled to be half
     * as wide compared to the last frame, then {@code location} will be updated to be half as far from the border's
     * left edge than it previously was.
     *
     * @param location the {@link Point} to move
     * @return the moved {@link Point}
     */
    Point move(Point location);
}
