package com.group_finity.mascot.behavior;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.exception.CantBeAliveException;

import java.awt.event.MouseEvent;

/**
 * An object that represents a {@link Mascot}'s long-term behavior.
 * <p>
 * Used with {@link Mascot#setBehavior(Behavior)}.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public interface Behavior {

    /**
     * Called when starting a behavior.
     *
     * @param mascot the {@link Mascot} with which to associate
     * @throws CantBeAliveException if this behavior, its corresponding action, or any next behavior fails to initialize
     * and the associated {@link Mascot} should be disposed
     */
    void init(Mascot mascot) throws CantBeAliveException;

    /**
     * Advances the mascot to the next frame.
     *
     * @throws CantBeAliveException if the next behavior fails to initialize and the associated {@link Mascot} should be
     * disposed
     */
    void next() throws CantBeAliveException;

    /**
     * Called when a mouse button is pressed.
     *
     * @param e the event created by a mouse button being pressed
     * @throws CantBeAliveException if the next behavior fails to initialize and the associated {@link Mascot} should be
     * disposed
     */
    void mousePressed(MouseEvent e) throws CantBeAliveException;

    /**
     * Called when a mouse button is released.
     *
     * @param e the event created by a mouse button being released
     * @throws CantBeAliveException if the next behavior fails to initialize and the associated {@link Mascot} should be
     * disposed
     */
    void mouseReleased(MouseEvent e) throws CantBeAliveException;
}
