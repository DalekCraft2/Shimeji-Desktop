package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An object that builds action references.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class ActionRef implements IActionBuilder {

    private static final Logger log = LoggerFactory.getLogger(ActionRef.class);

    private final Configuration configuration;

    private final String name;

    private final Map<String, String> params;

    public ActionRef(final Configuration configuration, final Entry refNode) throws ConfigurationException {
        this.configuration = configuration;

        name = refNode.getAttribute(configuration.getSchema().getString("Name"));

        log.debug("Loading action reference: {}", this);

        Map<String, String> attributes = refNode.getAttributes();
        if (attributes.isEmpty()) {
            // Use the same one empty map instance to save memory
            params = Map.of();
        } else {
            // Use new LinkedHashMap() instead of Map.copyOf() to preserve LinkedHashMap behavior
            params = new LinkedHashMap<>(attributes);
        }

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

    @Override
    public void validate() throws ConfigurationException {
        if (!configuration.getActionBuilders().containsKey(name)) {
            throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("NoActionFoundErrorMessage"), name));
        }
    }

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
