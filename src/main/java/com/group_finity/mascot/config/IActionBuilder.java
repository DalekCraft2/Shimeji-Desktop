package com.group_finity.mascot.config;

import com.group_finity.mascot.action.Action;

import java.util.Map;

/**
 * An object that builds actions.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 * @see ActionBuilder
 * @see ActionRef
 */
public interface IActionBuilder {

    /**
     * Ensures the validity of any data loaded by this {@code IActionBuilder} object that
     * could not be validated when this {@code IActionBuilder} object was being initialized.
     * <p>
     * This should be called after all data has been loaded into the parent
     * {@link Configuration} object of this {@code IActionBuilder}.
     *
     * @throws ConfigurationException if this {@code IActionBuilder} object contains invalid data
     * @see ActionBuilder#validate()
     * @see ActionRef#validate()
     */
    void validate() throws ConfigurationException;

    /**
     * Builds this action and adds the specified parameters to its context.
     *
     * @param params a map of parameter names and values to add to the context of the built action
     * @return the built action
     * @throws ActionInstantiationException if this action fails to be built
     * @see ActionBuilder#buildAction(Map)
     * @see ActionRef#buildAction(Map)
     */
    Action buildAction(final Map<String, String> params) throws ActionInstantiationException;
}
