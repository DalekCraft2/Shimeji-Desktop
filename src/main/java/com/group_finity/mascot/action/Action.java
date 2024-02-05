package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;

/**
 * An object that represents a {@link Mascot}'s short-term movement.
 * <p>
 * {@link #next()} is called at regular intervals.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public interface Action {

    /**
     * Called when starting an action.
     *
     * @param mascot the {@link Mascot} with which to associate
     */
    void init(Mascot mascot) throws VariableException;

    /**
     * Checks whether there is a next frame.
     *
     * @return whether there is a next frame
     */
    boolean hasNext() throws VariableException;

    /**
     * Advances the {@link Mascot} to the next frame.
     *
     * @throws LostGroundException if there is no ground
     */
    void next() throws LostGroundException, VariableException;

}
