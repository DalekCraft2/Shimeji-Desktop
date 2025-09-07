package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.environment.Border;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;

/**
 * An object that represents a {@link Mascot}'s short-term movement.
 * <p>
 * {@link #next()} is called at regular intervals.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public interface Action {

    /**
     * Called when starting an action.
     *
     * @param mascot the {@link Mascot} with which to associate
     * @throws VariableException if one of the parameters passed to the action is invalid or can not be parsed
     */
    void init(Mascot mascot) throws VariableException;

    /**
     * Checks whether there is a next frame.
     *
     * @return whether there is a next frame
     * @throws VariableException if one of the parameters passed to the action can not be parsed
     */
    boolean hasNext() throws VariableException;

    /**
     * Advances the associated {@link Mascot} to the next frame.
     *
     * @throws LostGroundException if the {@link Mascot} is not on any {@link Border} or should otherwise begin falling
     * @throws VariableException if one of the parameters passed to the action can not be parsed
     */
    void next() throws LostGroundException, VariableException;
}
