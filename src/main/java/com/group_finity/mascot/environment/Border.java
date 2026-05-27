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
     * Checks whether the specified {@link Point} is on this {@code Border}.
     *
     * @param location the {@code Point} to check
     * @return whether the specified {@code Point} is on this {@code Border}
     */
    boolean isOn(Point location);

    /**
     * Moves the specified {@link Point} to match any changes to the
     * position and size of this {@code Border} since the last frame.
     * <p>
     * For example, if this {@code Border} has moved to the right by X units,
     * then {@code location} will be moved to the right by X units as well.
     * If this {@code Border} is a {@linkplain FloorCeiling floor or ceiling}
     * and has been scaled to be half as wide compared to the last frame,
     * then {@code location} will be updated to be half as far from this
     * {@code Border} object's left edge than it previously was.
     *
     * @param location the {@code Point} to move
     * @return the moved {@code Point}
     */
    Point move(Point location);
}
