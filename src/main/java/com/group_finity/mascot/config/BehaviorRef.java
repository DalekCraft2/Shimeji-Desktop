package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.behavior.UserBehavior;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An object that builds behavior references.
 *
 * @author DalekCraft
 */
public class BehaviorRef implements IBehaviorBuilder {

    private static final Logger log = LoggerFactory.getLogger(BehaviorRef.class);

    private final Configuration configuration;

    private final String name;

    private final int frequency;

    private final List<String> conditions;

    private final Map<String, String> params = new LinkedHashMap<>();

    public BehaviorRef(final Configuration configuration, final Entry behaviorNode, final List<String> conditions) throws ConfigurationException {
        this.configuration = configuration;
        name = behaviorNode.getAttribute(configuration.getSchema().getString("Name"));
        frequency = Integer.parseInt(behaviorNode.getAttribute(configuration.getSchema().getString("Frequency")));

        String condition = behaviorNode.getAttribute(configuration.getSchema().getString("Condition"));
        try {
            // Verify that the condition can be parsed
            Variable.parse(condition);
        } catch (final VariableException e) {
            throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString("FailedConditionEvaluationErrorMessage"), e);
        }
        this.conditions = new ArrayList<>(conditions);
        this.conditions.add(condition);

        log.debug("Loading behavior reference: {}", this);

        params.putAll(behaviorNode.getAttributes());
        params.remove(configuration.getSchema().getString("Name"));
        params.remove(configuration.getSchema().getString("Action"));
        params.remove(configuration.getSchema().getString("Frequency"));
        params.remove(configuration.getSchema().getString("Hidden"));
        params.remove(configuration.getSchema().getString("Condition"));
        params.remove(configuration.getSchema().getString("Toggleable"));

        // Verify that all parameters can be parsed
        for (final Map.Entry<String, String> param : params.entrySet()) {
            try {
                Variable.parse(param.getValue());
            } catch (final VariableException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedParameterEvaluationErrorMessage"), param.getKey()), e);
            }
        }

        log.debug("Finished loading behavior reference: {}", this);
    }

    @Override
    public String toString() {
        return "BehaviorRef[name=" + name + ",frequency=" + frequency + "]";
    }

    /**
     * Ensures that this behavior reference does not reference a nonexistent behavior.
     * Should be called after all behaviors have been loaded from {@code behaviors.xml}.
     *
     * @throws ConfigurationException if the behavior reference references a nonexistent behavior
     */
    public void validate() throws ConfigurationException {
        if (!configuration.getBehaviorNames().contains(name)) {
            throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("NoBehaviourFoundErrorMessage"), name));
        }
    }

    @Override
    public Behavior buildBehavior() throws BehaviorInstantiationException {
        final Map<String, String> newParams = new LinkedHashMap<>(params);
        return configuration.getBehaviorBuilders().get(name).buildBehavior(newParams);
    }

    public boolean isEffective(final VariableMap context) throws VariableException {
        if (frequency == 0) {
            return false;
        }

        for (final String condition : conditions) {
            if (condition != null) {
                if (!(Boolean) Variable.parse(condition).get(context)) {
                    return false;
                }
            }
        }

        return true;
    }

    public String getName() {
        return name;
    }

    @Override
    public int getFrequency() {
        return frequency;
    }
}
