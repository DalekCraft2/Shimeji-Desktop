package com.group_finity.mascot.behavior;

import com.group_finity.mascot.Mascot;

import java.awt.event.MouseEvent;

/**
 * An object that represents a {@link Mascot}'s long-term behavior.
 * <p>
 * Used with {@link Mascot#setBehavior(Behavior)}.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 * @see com.group_finity.mascot.action.Action
 */
public interface Behavior {

    /**
     * Called when starting a behavior.
     *
     * @param mascot the {@link Mascot} with which to associate
     * @throws BehaviorExecutionException if this behavior, its corresponding action, or any next behavior fails to
     * initialize
     * @see com.group_finity.mascot.action.Action#init(Mascot)
     */
    void init(Mascot mascot) throws BehaviorExecutionException;

    /**
     * Advances the associated {@link Mascot} to the next frame.
     *
     * @throws BehaviorExecutionException if the next behavior fails to initialize
     * @see com.group_finity.mascot.action.Action#next()
     */
    void next() throws BehaviorExecutionException;

    /**
     * Called when a mouse button is pressed.
     *
     * @param e the event created by a mouse button being pressed
     * @throws BehaviorExecutionException if the next behavior fails to initialize
     * @see java.awt.event.MouseListener#mousePressed(MouseEvent)
     */
    void mousePressed(MouseEvent e) throws BehaviorExecutionException;

    /**
     * Called when a mouse button is released.
     *
     * @param e the event created by a mouse button being released
     * @throws BehaviorExecutionException if the next behavior fails to initialize
     * @see java.awt.event.MouseListener#mouseReleased(MouseEvent)
     */
    void mouseReleased(MouseEvent e) throws BehaviorExecutionException;
}
