package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An implementation of {@link IActionBuilder} that stores minimal information about an action,
 * and delegates {@link #buildAction(Map)} calls to the {@link ActionBuilder} that it references.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class ActionRef implements IActionBuilder {

    private static final Logger log = LoggerFactory.getLogger(ActionRef.class);

    /**
     * The parent {@link Configuration} object of this {@code ActionRef}.
     */
    private final Configuration configuration;

    /**
     * The name of the action referenced by this {@code ActionRef}.
     * An {@link ActionBuilder} with this name must exist within the parent {@link Configuration} of this
     * {@code ActionRef} by the time {@link #validate()} is called.
     */
    private final String name;

    /**
     * The parameters to add to the context of this action.
     * These will be parsed into {@link Variable} objects when this action is built.
     */
    private final Map<String, String> params;

    /**
     * Creates a new {@code ActionRef} from the data contained within the specified ActionReference node.
     *
     * @param configuration the parent {@link Configuration} object of this {@code ActionRef}
     * @param refNode the ActionReference node from which to load this action
     * @throws ConfigurationException if one of the ActionReference node's attributes cannot be parsed
     * into a {@link Variable}
     */
    public ActionRef(final Configuration configuration, final Entry refNode) throws ConfigurationException {
        this.configuration = configuration;

        name = refNode.getAttribute(configuration.getSchema().getString("Name"));

        if (log.isDebugEnabled()) {
            log.debug("Loading action reference: {}", this);
        }

        // No need to check whether the attributes map is empty like in BehaviorBuilder,
        // because it's guaranteed to not be empty since we haven't removed any of the required attributes
        params = new LinkedHashMap<>(refNode.getAttributes());

        // Verify that all parameters can be parsed
        for (final Map.Entry<String, String> param : params.entrySet()) {
            try {
                Variable.parse(param.getValue());
            } catch (final VariableException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedParameterEvaluationErrorMessage"), param.getKey()), e);
            }
        }
    }

    @Override
    public String toString() {
        return "ActionRef[name=" + name + "]";
    }

    /**
     * Ensures the validity of any data loaded by this {@code ActionRef} object that
     * could not be validated when this {@code ActionRef} object was being initialized.
     * Specifically, this ensures that this {@code ActionRef} does not reference a nonexistent action.
     * <p>
     * This should be called after all data has been loaded into the parent
     * {@link Configuration} object of this {@code ActionRef}.
     *
     * @throws ConfigurationException if this {@code ActionRef} references a nonexistent action
     * @see ActionBuilder#validate()
     */
    @Override
    public void validate() throws ConfigurationException {
        if (!configuration.getActionBuilders().containsKey(name)) {
            throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("NoActionFoundErrorMessage"), name));
        }
    }

    /**
     * Builds the action that is referenced by this {@code ActionRef}, and adds the specified parameters
     * to its context. If a parameter name is present in both the specified parameters and this action
     * reference's existing parameters, the specified parameter will be ignored in favor of the existing parameter.
     *
     * @param params a map of parameter names and values to add to the context of the built action
     * @return the built action
     * @throws ActionInstantiationException if this {@code ActionRef} references a nonexistent action,
     * or the referenced action fails to be built
     * @see ActionBuilder#buildAction(Map)
     */
    @Override
    public Action buildAction(final Map<String, String> params) throws ActionInstantiationException {
        final Map<String, String> newParams;
        if (this.params.isEmpty() && params.isEmpty()) {
            newParams = Map.of();
        } else if (this.params.isEmpty()) {
            newParams = params;
        } else if (params.isEmpty()) {
            newParams = this.params;
        } else {
            newParams = new LinkedHashMap<>(params);
            newParams.putAll(this.params);
        }
        return configuration.buildAction(name, newParams);
    }
}
