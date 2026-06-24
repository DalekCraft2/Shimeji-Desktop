package com.group_finity.mascot.behavior;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.config.BehaviorBuilder;

import java.awt.event.MouseEvent;

/**
 * An object that represents the long-term behavior of a {@link Mascot}.
 * <p>
 * The primary function of a behavior is to link the end of one {@link com.group_finity.mascot.action.Action Action}
 * to the start of another. Although similar to {@link com.group_finity.mascot.action.ComplexAction ComplexAction}
 * implementations, behaviors are not forced to follow a linear sequence of behaviors when executing. The next behavior
 * to execute after any given behavior is randomly chosen from a
 * {@linkplain BehaviorBuilder#getNextBehaviorBuilders() list of behaviors}, which are set per-behavior. The likelihood
 * of a behavior being chosen to execute next is affected by its {@linkplain BehaviorBuilder#getFrequency() frequency}.
 * <p>
 * Similarly to actions, behaviors can also be set to only execute when certain conditions are met. However, unlike
 * actions, behaviors only evaluate their conditions to determine whether to <i>start</i> executing, whereas actions
 * evaluate their conditions on every tick to determine whether to <i>continue</i> executing.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 * @see com.group_finity.mascot.action.Action
 * @see Mascot#setBehavior(Behavior)
 */
public interface Behavior {

    /**
     * Initializes this behavior and associates it with the specified {@link Mascot}.
     *
     * @param mascot the {@code Mascot} that will execute this behavior
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
     * Called when a mouse button is pressed on the window of this behavior's associated {@link Mascot}.
     *
     * @param e the event created by a mouse button being pressed
     * @throws BehaviorExecutionException if the next behavior fails to initialize
     * @see java.awt.event.MouseListener#mousePressed(MouseEvent)
     */
    void mousePressed(MouseEvent e) throws BehaviorExecutionException;

    /**
     * Called when a mouse button is released on the window of this behavior's associated {@link Mascot}.
     *
     * @param e the event created by a mouse button being released
     * @throws BehaviorExecutionException if the next behavior fails to initialize
     * @see java.awt.event.MouseListener#mouseReleased(MouseEvent)
     */
    void mouseReleased(MouseEvent e) throws BehaviorExecutionException;
}
