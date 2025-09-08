package com.group_finity.mascot.config;

import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;

import java.util.Map;

/**
 * An object that builds actions and action references.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public interface IActionBuilder {

    /**
     * Builds the action and all of its children actions/action references using the given parameters.
     *
     * @param params a {@link Map} of attributes. This will contain the attributes from all actions in this action's
     * inheritance tree, as well as the non-functional attributes from the behavior corresponding to the root action
     * (non-functional referring to attributes that are not read by the program when building the behavior).
     * @return the built action
     * @throws ActionInstantiationException if the action contains an action reference without a corresponding
     * action, an action's class can not be instantiated, or a script inside the action fails to be compiled
     */
    Action buildAction(final Map<String, String> params) throws ActionInstantiationException;

    /**
     * Validates the action. Should be called after {@link #buildAction(Map)} has been called.
     *
     * @throws ConfigurationException if the action or one of its children references a nonexistent action
     */
    void validate() throws ConfigurationException;
}
