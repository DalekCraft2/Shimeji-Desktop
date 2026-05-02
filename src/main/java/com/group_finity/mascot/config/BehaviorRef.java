package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final Map<String, String> params;

    public BehaviorRef(final Configuration configuration, final Entry refNode, final List<String> conditions) throws ConfigurationException {
        this.configuration = configuration;
        name = refNode.getAttribute(configuration.getSchema().getString("Name"));
        frequency = Integer.parseInt(refNode.getAttribute(configuration.getSchema().getString("Frequency")));

        log.debug("Loading behavior reference: {}", this);

        if (refNode.hasAttribute(configuration.getSchema().getString("Condition"))) {
            String condition = refNode.getAttribute(configuration.getSchema().getString("Condition"));
            try {
                // Verify that the condition can be parsed
                Variable.parse(condition);
            } catch (final VariableException e) {
                throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString("FailedConditionEvaluationErrorMessage"), e);
            }

            if (conditions.isEmpty()) {
                this.conditions = List.of(condition);
            } else {
                String[] conditionArray = conditions.toArray(new String[conditions.size() + 1]);
                conditionArray[conditionArray.length - 1] = condition;
                this.conditions = List.of(conditionArray);
            }
        } else {
            this.conditions = List.copyOf(conditions);
        }

        Map<String, String> tempParams = new LinkedHashMap<>(refNode.getAttributes());
        tempParams.remove(configuration.getSchema().getString("Name"));
        tempParams.remove(configuration.getSchema().getString("Action"));
        tempParams.remove(configuration.getSchema().getString("Frequency"));
        tempParams.remove(configuration.getSchema().getString("Hidden"));
        tempParams.remove(configuration.getSchema().getString("Condition"));
        tempParams.remove(configuration.getSchema().getString("Toggleable"));
        if (tempParams.isEmpty()) {
            // Use the same one empty map instance to save memory
            params = Map.of();
        } else {
            // Don't use Map.copyOf() so LinkedHashMap behavior is preserved
            params = tempParams;
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
        return configuration.getBehaviorBuilders().get(name).buildBehavior(params);
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
