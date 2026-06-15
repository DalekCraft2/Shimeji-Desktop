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
 * An implementation of {@link IBehaviorBuilder} that stores all information about a behavior
 * so the behavior can be quickly built when needed.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class BehaviorBuilder implements IBehaviorBuilder {

    private static final Logger log = LoggerFactory.getLogger(BehaviorBuilder.class);

    /**
     * The parent {@link Configuration} object of this {@code BehaviorBuilder}.
     */
    private final Configuration configuration;

    /**
     * The name of this {@code BehaviorBuilder}. This value must be unique
     * among the top-level behaviors within this behavior's parent {@link Configuration}.
     *
     * @see #getName()
     */
    private final String name;

    /**
     * The name of the action referenced by this {@code BehaviorBuilder}.
     * An {@link ActionBuilder} with this name must exist within the parent {@link Configuration} of this
     * {@code BehaviorBuilder} by the time {@link #validate()} is called.
     * <p>
     * If this attribute is not present in the Behavior node, it is defaulted to the behavior's name.
     */
    private final String actionName;

    /**
     * The frequency, or weight, of this behavior. This is used in conjunction with the frequencies of other behaviors
     * to calculate the probability of this behavior being executed. Larger values make it more likely, and smaller
     * values make it less likely. A value of 0 will make this behavior never execute unless it is specifically
     * referenced by an action or another behavior.
     *
     * @see #getFrequency()
     */
    private final int frequency;

    /**
     * The conditions for this {@code BehaviorBuilder}.
     * All of these must evaluate to {@code true} in order for this behavior to be executed.
     * <p>
     * Note that these conditions are not stored as {@link Variable} objects, because they are re-parsed every time
     * {@link #isEffective(VariableMap)} is called. This means that the values they had when they were last evaluated
     * are not stored internally, so it makes no difference which variant of {@linkplain Variable#parse(String) script syntax}
     * (i.e., {@code ${...}} or {@code #{...}}) is used for them.
     *
     * @see #isEffective(VariableMap)
     */
    private final List<String> conditions;

    /**
     * Whether this behavior is hidden from the "Set Behavior" submenu in the mascot context menu.
     * If this attribute is not present in the Behavior node, it is defaulted to {@code false}.
     *
     * @see #isHidden()
     */
    private final boolean hidden;

    /**
     * Whether this behavior can be toggled via the "Allowed Behaviors" submenu in the mascot context menu.
     * If this behavior is one of the four required behaviors ({@link UserBehavior#BEHAVIORNAME_CHASEMOUSE ChaseMouse},
     * {@link UserBehavior#BEHAVIORNAME_FALL Fall}, {@link UserBehavior#BEHAVIORNAME_DRAGGED Dragged}, or
     * {@link UserBehavior#BEHAVIORNAME_THROWN Thrown}), or if this attribute is not present in the Behavior node,
     * it is defaulted to {@code false}.
     *
     * @see #isToggleable()
     */
    private final boolean toggleable;

    /**
     * Whether this behavior allows other top-level behaviors (i.e., other {@code BehaviorBuilder} objects) to be
     * candidates for this behavior's next behavior, in addition to the behaviors in {@link #nextBehaviorBuilders}.
     *
     * @see #frequency
     * @see #nextBehaviorBuilders
     * @see #isNextAdditive()
     */
    private final boolean nextAdditive;

    /**
     * A list of references to the behaviors that may execute after this behavior has finished executing.
     *
     * @see #getNextBehaviorBuilders()
     */
    private final List<BehaviorRef> nextBehaviorBuilders;

    /**
     * The parameters to add to the context of this behavior's associated action.
     * These will be parsed into {@link Variable} objects when this behavior is built.
     */
    private final Map<String, String> params;

    /**
     * Creates a new {@code BehaviorBuilder} from the data contained within the specified Behavior node.
     *
     * @param configuration the parent {@link Configuration} object of this {@code BehaviorBuilder}
     * @param behaviorNode the Behavior node from which to load this behavior
     * @param conditions the conditions that must all evaluate to {@code true} for this behavior to be executed
     * @throws ConfigurationException if one of the Behavior node's attributes cannot be parsed into a {@link Variable}
     * @throws NumberFormatException if the Behavior node's Frequency attribute cannot be parsed as an int
     */
    public BehaviorBuilder(final Configuration configuration, final Entry behaviorNode, final List<String> conditions) throws ConfigurationException {
        this.configuration = configuration;
        ResourceBundle schema = configuration.getSchema();
        name = behaviorNode.getAttribute(schema.getString("Name"));
        actionName = behaviorNode.hasAttribute(schema.getString("Action")) ?
                behaviorNode.getAttribute(schema.getString("Action")) : name;
        frequency = Integer.parseInt(behaviorNode.getAttribute(schema.getString("Frequency")));
        hidden = behaviorNode.hasAttribute(schema.getString("Hidden")) &&
                Boolean.parseBoolean(behaviorNode.getAttribute(schema.getString("Hidden")));

        if (log.isDebugEnabled()) {
            log.debug("Loading behavior: {}", this);
        }

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
        if (!params.isEmpty()) {
            for (final Map.Entry<String, String> param : params.entrySet()) {
                try {
                    Variable.parse(param.getValue());
                } catch (final VariableException e) {
                    throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                            "FailedParameterEvaluationErrorMessage"), param.getKey()), e);
                }
            }
        }

        List<Entry> nextLists = behaviorNode.selectChildren(schema.getString("NextBehaviourList"));
        if (nextLists.isEmpty()) {
            nextAdditive = true;
            nextBehaviorBuilders = List.of();
        } else {
            boolean nextAdditive = true;
            List<BehaviorRef> nextBehaviorBuilders = new ArrayList<>();

            for (final Entry nextList : nextLists) {
                nextAdditive = Boolean.parseBoolean(nextList.getAttribute(schema.getString("Add")));

                loadBehaviors(nextList, List.of(), nextBehaviorBuilders);
            }

            this.nextAdditive = nextAdditive;
            // Make list immutable
            this.nextBehaviorBuilders = List.copyOf(nextBehaviorBuilders);
        }

        if (log.isDebugEnabled()) {
            log.debug("Finished loading behavior: {}", this);
        }
    }

    @Override
    public String toString() {
        return "Behavior[name=" + name + ",frequency=" + frequency + ",actionName=" + actionName + "]";
    }

    /**
     * Loads behaviors from the specified NextBehaviorList/Condition node.
     * If a Condition node is present in the specified node's children, then the specified list of conditions is copied,
     * the new condition is appended to it, and the method recurses using the new Condition node and the copied list.
     *
     * @param list the NextBehaviorList/Condition node from which to load behaviors
     * @param conditions a list containing the conditions from the specified node and all of its parent nodes.
     * The behaviors in the specified node can only be executed when all of these conditions evaluate to {@code true}.
     * @throws ConfigurationException if a behavior in the specified list cannot be loaded or a condition cannot be parsed
     */
    private void loadBehaviors(final Entry list, final List<String> conditions, final List<BehaviorRef> nextBehaviorBuilders) throws ConfigurationException {
        List<Entry> children = list.getChildren();
        if (children.isEmpty()) {
            return;
        }
        ResourceBundle schema = configuration.getSchema();
        for (final Entry node : children) {
            if (node.getName().equals(schema.getString("Condition"))) {
                List<String> newConditions;
                if (node.hasAttribute(schema.getString("Condition"))) {
                    String condition = node.getAttribute(schema.getString("Condition"));
                    try {
                        // Verify that the condition can be parsed
                        Variable.parse(condition);
                    } catch (final VariableException e) {
                        throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString(
                                "FailedConditionEvaluationErrorMessage"), e);
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
                } catch (ConfigurationException | RuntimeException e) {
                    throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                            "FailedLoadBehaviourReferenceErrorMessage"), node.getAttributes()), e);
                }
            }
        }
    }

    /**
     * Ensures the validity of any data loaded by this {@code BehaviorBuilder} object that
     * could not be validated when this {@code BehaviorBuilder} object was being initialized.
     * Specifically, this ensures that this behavior does not reference a nonexistent action,
     * and that none of this behavior's next behaviors reference a nonexistent behavior.
     * <p>
     * This should be called after all data has been loaded into the parent
     * {@link Configuration} object of this {@code BehaviorBuilder}.
     *
     * @throws ConfigurationException if this behavior references a nonexistent action,
     * or one of this behavior's next behaviors references a nonexistent behavior
     */
    public void validate() throws ConfigurationException {
        if (!configuration.getActionBuilders().containsKey(actionName)) {
            throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                    "NoActionFoundErrorMessage"), actionName));
        }
        if (!nextBehaviorBuilders.isEmpty()) {
            for (final BehaviorRef ref : nextBehaviorBuilders) {
                try {
                    ref.validate();
                } catch (ConfigurationException e) {
                    throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                            "FailedValidateBehaviourErrorMessage"), ref), e);
                }
            }
        }
    }

    @Override
    public Behavior buildBehavior() throws BehaviorInstantiationException {
        try {
            return new UserBehavior(name, configuration.buildAction(actionName, params), configuration);
        } catch (final ActionInstantiationException e) {
            throw new BehaviorInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString(
                    "FailedInitialiseCorrespondingActionErrorMessage"), actionName), name, e);
        }
    }

    /**
     * Builds this behavior and its corresponding top-level action, and adds the specified parameters
     * to the action's context. If a parameter name is present in both the specified parameters and this behavior's
     * existing parameters, the existing parameter will be overwritten by the specified parameter.
     *
     * @param params a map of parameter names and values to add to the context of this behavior's corresponding action
     * @return the built behavior
     * @throws BehaviorInstantiationException if this behavior's corresponding action fails to be built
     * @see BehaviorBuilder#buildBehavior()
     * @see BehaviorRef#buildBehavior()
     * @see ActionBuilder#buildAction(Map)
     */
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
            throw new BehaviorInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString(
                    "FailedInitialiseCorrespondingActionErrorMessage"), actionName), name, e);
        }
    }

    /**
     * Checks whether this behavior's conditions all evaluate to {@code true}
     * using the supplied context.
     *
     * @param context the context to use when evaluating this behavior's conditions
     * @return {@code true} if the conditions all evaluated to {@code true}; {@code false} otherwise
     * @throws VariableException if one of the conditions fails to be evaluated
     * @see Variable#get(VariableMap)
     */
    public boolean isEffective(final VariableMap context) throws VariableException {
        if (frequency == 0) {
            return false;
        }

        if (!conditions.isEmpty()) {
            for (final String condition : conditions) {
                if (condition != null) {
                    if (!(Boolean) Variable.parse(condition).get(context)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Gets the name of this {@code BehaviorBuilder}. The caller must verify that the returned
     * value is unique among the top-level behaviors within this {@code BehaviorBuilder} object's
     * parent {@link Configuration}.
     *
     * @return the name of this {@code BehaviorBuilder}
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the frequency, or weight, of this behavior. This is used in conjunction with the frequencies of other
     * behaviors to calculate the probability of this behavior being executed. Larger values make it more likely,
     * and smaller values make it less likely. If the returned value is 0, this behavior will never execute unless
     * it is specifically referenced by an action or another behavior.
     *
     * @return the frequency of this behavior
     */
    @Override
    public int getFrequency() {
        return frequency;
    }

    /**
     * Gets whether this behavior is hidden from the "Set Behavior" submenu in the mascot context menu.
     *
     * @return {@code true} if this behavior is hidden from the "Set Behavior" submenu; {@code false} otherwise
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * Gets whether this behavior can be toggled via the "Allowed Behaviors" submenu in the mascot context menu.
     *
     * @return {@code true} if this behavior can be toggled via the "Allowed Behaviors" submenu;
     * {@code false} otherwise
     */
    public boolean isToggleable() {
        return toggleable;
    }

    /**
     * Gets whether this behavior allows other top-level behaviors (i.e., other {@code BehaviorBuilder} objects) to be
     * candidates for this behavior's next behavior, in addition to the behaviors in {@link #nextBehaviorBuilders}.
     *
     * @return {@code true} if this behavior allows other top-level behaviors to be candidates
     * for its next behavior; {@code false} otherwise
     * @see #getFrequency()
     * @see #getNextBehaviorBuilders()
     */
    public boolean isNextAdditive() {
        return nextAdditive;
    }

    /**
     * Gets a list of references to the behaviors that may execute after this behavior has finished executing.
     *
     * @return a list of references to the behaviors that may be executed after this behavior has finished executing
     */
    public List<BehaviorRef> getNextBehaviorBuilders() {
        return nextBehaviorBuilders;
    }
}
