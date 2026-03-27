package com.group_finity.mascot.config;

import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;

import java.util.Map;

/**
 * An object that builds behaviors and behavior references.
 *
 * @author DalekCraft
 */
public interface IBehaviorBuilder {

    /**
     * Builds the behavior, its corresponding action, and all of the corresponding action's children actions/action
     * references.
     *
     * @return the built behavior
     * @throws BehaviorInstantiationException if the behavior's corresponding action fails to be built
     * @see IActionBuilder#buildAction(Map)
     */
    Behavior buildBehavior() throws BehaviorInstantiationException;

    /**
     * Gets the frequency of this behavior.
     *
     * @return the frequency of this behavior
     */
    int getFrequency();
}
