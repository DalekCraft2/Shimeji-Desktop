package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.behavior.UserBehavior;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableException;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * An object that builds behaviors.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class BehaviorBuilder implements IBehaviorBuilder {

    private static final Logger log = LoggerFactory.getLogger(BehaviorBuilder.class);

    private final Configuration configuration;

    private final String name;

    private final String actionName;

    private final int frequency;

    private final List<String> conditions;

    private final boolean hidden;

    private final boolean toggleable;

    private final boolean nextAdditive;

    private final List<BehaviorRef> nextBehaviorBuilders;

    private final Map<String, String> params;

    public BehaviorBuilder(final Configuration configuration, final Entry behaviorNode, final List<String> conditions) throws ConfigurationException {
        this.configuration = configuration;
        ResourceBundle schema = configuration.getSchema();
        name = behaviorNode.getAttribute(schema.getString("Name"));
        actionName = behaviorNode.hasAttribute(schema.getString("Action")) ?
                behaviorNode.getAttribute(schema.getString("Action")) : name;
        frequency = Integer.parseInt(behaviorNode.getAttribute(schema.getString("Frequency")));
        hidden = behaviorNode.hasAttribute(schema.getString("Hidden")) &&
                Boolean.parseBoolean(behaviorNode.getAttribute(schema.getString("Hidden")));

        log.debug("Loading behavior: {}", this);

        if (behaviorNode.hasAttribute(schema.getString("Condition"))) {
            String condition = behaviorNode.getAttribute(schema.getString("Condition"));
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

        // override of toggleable state for required fields
        if (!behaviorNode.hasAttribute(schema.getString("Toggleable")) ||
                name.equals(schema.getString(UserBehavior.BEHAVIORNAME_CHASEMOUSE)) ||
                name.equals(schema.getString(UserBehavior.BEHAVIORNAME_FALL)) ||
                name.equals(schema.getString(UserBehavior.BEHAVIORNAME_THROWN)) ||
                name.equals(schema.getString(UserBehavior.BEHAVIORNAME_DRAGGED))) {
            toggleable = false;
        } else {
            toggleable = Boolean.parseBoolean(behaviorNode.getAttribute(schema.getString("Toggleable")));
        }

        Map<String, String> tempParams = new LinkedHashMap<>(behaviorNode.getAttributes());
        tempParams.remove(schema.getString("Name"));
        tempParams.remove(schema.getString("Action"));
        tempParams.remove(schema.getString("Frequency"));
        tempParams.remove(schema.getString("Hidden"));
        tempParams.remove(schema.getString("Condition"));
        tempParams.remove(schema.getString("Toggleable"));
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

        boolean nextAdditive = true;
        List<BehaviorRef> nextBehaviorBuilders = new ArrayList<>();

        for (final Entry nextList : behaviorNode.selectChildren(schema.getString("NextBehaviourList"))) {
            nextAdditive = Boolean.parseBoolean(nextList.getAttribute(schema.getString("Add")));

            loadBehaviors(nextList, List.of(), nextBehaviorBuilders);
        }

        this.nextAdditive = nextAdditive;
        // Make list immutable
        this.nextBehaviorBuilders = List.copyOf(nextBehaviorBuilders);

        log.debug("Finished loading behavior: {}", this);
    }

    @Override
    public String toString() {
        return "Behavior[name=" + name + ",frequency=" + frequency + ",actionName=" + actionName + "]";
    }

    private void loadBehaviors(final Entry list, final List<String> conditions, final List<BehaviorRef> nextBehaviorBuilders) throws ConfigurationException {
        ResourceBundle schema = configuration.getSchema();
        for (final Entry node : list.getChildren()) {
            if (node.getName().equals(schema.getString("Condition"))) {
                List<String> newConditions;
                if (node.hasAttribute(schema.getString("Condition"))) {
                    String condition = node.getAttribute(schema.getString("Condition"));
                    try {
                        // Verify that the condition can be parsed
                        Variable.parse(condition);
                    } catch (final VariableException e) {
                        throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString("FailedConditionEvaluationErrorMessage"), e);
                    }

                    if (conditions.isEmpty()) {
                        newConditions = List.of(condition);
                    } else {
                        String[] conditionArray = conditions.toArray(new String[conditions.size() + 1]);
                        conditionArray[conditionArray.length - 1] = condition;
                        newConditions = List.of(conditionArray);
                    }
                } else {
                    newConditions = List.copyOf(conditions);
                }

                loadBehaviors(node, newConditions, nextBehaviorBuilders);
            } else if (node.getName().equals(schema.getString("BehaviourReference"))) {
                try {
                    nextBehaviorBuilders.add(new BehaviorRef(configuration, node, conditions));
                } catch (ConfigurationException e) {
                    throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedLoadBehaviourReferenceErrorMessage"), node.getAttributes()), e);
                }
            }
        }
    }

    /**
     * Ensures that this behavior and all of its children do not reference nonexistent actions or behaviors.
     * Should be called after all behaviors have been loaded from {@code behaviors.xml}.
     *
     * @throws ConfigurationException if the behavior or one of its children references a nonexistent action or behavior
     */
    public void validate() throws ConfigurationException {
        if (!configuration.getActionBuilders().containsKey(actionName)) {
            throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("NoActionFoundErrorMessage"), actionName));
        }
        for (final BehaviorRef ref : nextBehaviorBuilders) {
            try {
                ref.validate();
            } catch (ConfigurationException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedValidateBehaviourErrorMessage"), ref), e);
            }
        }
    }

    @Override
    public Behavior buildBehavior() throws BehaviorInstantiationException {
        try {
            return new UserBehavior(name, configuration.buildAction(actionName, params), configuration);
        } catch (final ActionInstantiationException e) {
            throw new BehaviorInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedInitialiseCorrespondingActionErrorMessage"), actionName), name, e);
        }
    }

    public Behavior buildBehavior(final Map<String, String> params) throws BehaviorInstantiationException {
        final Map<String, String> newParams;
        if (this.params.isEmpty() && params.isEmpty()) {
            newParams = Map.of();
        } else if (this.params.isEmpty()) {
            newParams = params;
        } else if (params.isEmpty()) {
            newParams = this.params;
        } else {
            newParams = new LinkedHashMap<>(this.params);
            newParams.putAll(params);
        }
        try {
            return new UserBehavior(name, configuration.buildAction(actionName, newParams), configuration);
        } catch (final ActionInstantiationException e) {
            throw new BehaviorInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedInitialiseCorrespondingActionErrorMessage"), actionName), name, e);
        }
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

    public boolean isHidden() {
        return hidden;
    }

    public boolean isToggleable() {
        return toggleable;
    }

    public boolean isNextAdditive() {
        return nextAdditive;
    }

    public List<BehaviorRef> getNextBehaviorBuilders() {
        return nextBehaviorBuilders;
    }
}
