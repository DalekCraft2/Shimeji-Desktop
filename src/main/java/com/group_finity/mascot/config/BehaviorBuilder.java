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

    private final List<BehaviorRef> nextBehaviorBuilders = new ArrayList<>();

    private final Map<String, String> params = new LinkedHashMap<>();

    public BehaviorBuilder(final Configuration configuration, final Entry behaviorNode, final List<String> conditions) throws ConfigurationException {
        this.configuration = configuration;
        name = behaviorNode.getAttribute(configuration.getSchema().getString("Name"));
        actionName = behaviorNode.hasAttribute(configuration.getSchema().getString("Action")) ? behaviorNode.getAttribute(configuration.getSchema().getString("Action")) : name;
        frequency = Integer.parseInt(behaviorNode.getAttribute(configuration.getSchema().getString("Frequency")));
        hidden = Boolean.parseBoolean(behaviorNode.getAttribute(configuration.getSchema().getString("Hidden")));

        log.debug("Loading behavior: {}", this);

        String condition = behaviorNode.getAttribute(configuration.getSchema().getString("Condition"));
        try {
            // Verify that the condition can be parsed
            Variable.parse(condition);
        } catch (final VariableException e) {
            throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString("FailedConditionEvaluationErrorMessage"), e);
        }
        this.conditions = new ArrayList<>(conditions);
        this.conditions.add(condition);

        // override of toggleable state for required fields
        if (name.equals(UserBehavior.BEHAVIOURNAME_FALL) ||
                name.equals(UserBehavior.BEHAVIOURNAME_THROWN) ||
                name.equals(UserBehavior.BEHAVIOURNAME_DRAGGED)) {
            toggleable = false;
        } else {
            toggleable = Boolean.parseBoolean(behaviorNode.getAttribute(configuration.getSchema().getString("Toggleable")));
        }

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

        boolean nextAdditive = true;

        for (final Entry nextList : behaviorNode.selectChildren(configuration.getSchema().getString("NextBehaviourList"))) {
            nextAdditive = Boolean.parseBoolean(nextList.getAttribute(configuration.getSchema().getString("Add")));

            loadBehaviors(nextList, new ArrayList<>());
        }

        this.nextAdditive = nextAdditive;

        log.debug("Finished loading behavior: {}", this);
    }

    @Override
    public String toString() {
        return "Behavior[name=" + name + ",frequency=" + frequency + ",actionName=" + actionName + "]";
    }

    private void loadBehaviors(final Entry list, final List<String> conditions) throws ConfigurationException {
        for (final Entry node : list.getChildren()) {
            if (node.getName().equals(configuration.getSchema().getString("Condition"))) {
                String condition = node.getAttribute(configuration.getSchema().getString("Condition"));
                try {
                    // Verify that the condition can be parsed
                    Variable.parse(condition);
                } catch (final VariableException e) {
                    throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString("FailedConditionEvaluationErrorMessage"), e);
                }
                final List<String> newConditions = new ArrayList<>(conditions);
                newConditions.add(condition);

                loadBehaviors(node, newConditions);
            } else if (node.getName().equals(configuration.getSchema().getString("BehaviourReference"))) {
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
        final Map<String, String> newParams = new LinkedHashMap<>(this.params);
        newParams.putAll(params);
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
