package com.group_finity.mascot.behavior;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.exception.CantBeAliveException;

import java.awt.event.MouseEvent;

/**
 * An object that represents a {@link Mascot}'s long-term behavior.
 * <p>
 * Used with {@link Mascot#setBehavior(Behavior)}.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public interface Behavior {

    /**
     * Called when starting a behavior.
     *
     * @param mascot the {@link Mascot} with which to associate
     */
    void init(Mascot mascot) throws CantBeAliveException;

    /**
     * Advances the mascot to the next frame.
     */
    void next() throws CantBeAliveException;

    /**
     * Called when a mouse button is pressed.
     *
     * @param e the event created by a mouse button being pressed
     */
    void mousePressed(MouseEvent e) throws CantBeAliveException;

    /**
     * Called when a mouse button is released.
     *
     * @param e the event created by a mouse button being released
     */
    void mouseReleased(MouseEvent e) throws CantBeAliveException;
}
