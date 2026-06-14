package com.group_finity.mascot.config;

import com.group_finity.mascot.behavior.Behavior;

import java.util.Map;

/**
 * An object that builds behaviors.
 *
 * @author DalekCraft
 * @see BehaviorBuilder
 * @see BehaviorRef
 */
public interface IBehaviorBuilder {

    /**
     * Builds this behavior and its corresponding top-level action.
     *
     * @return the built behavior
     * @throws BehaviorInstantiationException if this behavior's corresponding action fails to be built
     * @see BehaviorBuilder#buildBehavior()
     * @see BehaviorRef#buildBehavior()
     * @see ActionBuilder#buildAction(Map)
     */
    Behavior buildBehavior() throws BehaviorInstantiationException;

    /**
     * Gets the frequency, or weight, of this behavior. This is used in conjunction with the frequencies of other
     * behaviors to calculate the probability of this behavior being executed. Larger values make it more likely,
     * and smaller values make it less likely. If the returned value is 0, this behavior will never execute.
     *
     * @return the frequency of this behavior
     */
    int getFrequency();
}
