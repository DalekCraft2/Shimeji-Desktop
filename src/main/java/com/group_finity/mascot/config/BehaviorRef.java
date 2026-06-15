package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableException;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * An implementation of {@link IBehaviorBuilder} that stores minimal information about a behavior,
 * and delegates {@link #buildBehavior()} calls to the {@link BehaviorBuilder} that it references.
 *
 * @author DalekCraft
 */
public class BehaviorRef implements IBehaviorBuilder {

    private static final Logger log = LoggerFactory.getLogger(BehaviorRef.class);

    /**
     * The parent {@link Configuration} object of this {@code BehaviorRef}.
     */
    private final Configuration configuration;

    /**
     * The name of the behavior referenced by this {@code BehaviorRef}.
     * A {@link BehaviorBuilder} with this name must exist within the parent {@link Configuration} of this
     * {@code BehaviorRef} by the time {@link #validate()} is called.
     *
     * @see #getName()
     */
    private final String name;

    /**
     * The frequency, or weight, of this behavior. This is used in conjunction with the frequencies of other behaviors
     * to calculate the probability of this behavior being executed. Larger values make it more likely, and smaller
     * values make it less likely. A value of 0 will make this behavior never execute.
     *
     * @see #getFrequency()
     */
    private final int frequency;

    /**
     * The conditions for this {@code BehaviorRef}.
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
     * The parameters to add to the context of the associated behavior of this {@code BehaviorRef}.
     * These will be parsed into {@link Variable} objects when this behavior is built.
     */
    private final Map<String, String> params;

    /**
     * Creates a new {@code BehaviorRef} from the data contained within the specified BehaviorReference node.
     *
     * @param configuration the parent {@link Configuration} object of this {@code BehaviorRef}
     * @param refNode the BehaviorReference node from which to load this behavior
     * @param conditions the conditions that must all evaluate to {@code true} for this behavior to be executed
     * @throws ConfigurationException if one of the BehaviorReference node's attributes cannot be parsed into a
     * {@link Variable}
     * @throws NumberFormatException if the BehaviorReference node's Frequency attribute cannot be parsed as an int
     */
    public BehaviorRef(final Configuration configuration, final Entry refNode, final List<String> conditions) throws ConfigurationException {
        this.configuration = configuration;
        ResourceBundle schema = configuration.getSchema();
        // Ensure that the Name and Frequency attributes are present
        name = refNode.getAttribute(schema.getString("Name"));
        if (name == null) {
            throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                    "MissingRequiredAttributeErrorMessage"), schema.getString("Name")));
        }
        String frequencyText = refNode.getAttribute(schema.getString("Frequency"));
        if (frequencyText == null) {
            throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                    "MissingRequiredAttributeErrorMessage"), schema.getString("Frequency")));
        }
        frequency = Integer.parseInt(frequencyText);

        if (log.isDebugEnabled()) {
            log.debug("Loading behavior reference: {}", this);
        }

        // If the Condition attribute is present, add it to the list of conditions
        if (refNode.hasAttribute(schema.getString("Condition"))) {
            String condition = refNode.getAttribute(schema.getString("Condition"));
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

        // Copy the BehaviorReference node's attributes and remove any attributes that are used by the program.
        // This will leave us with only the custom user-defined attributes, if any exist.
        Map<String, String> tempParams = new LinkedHashMap<>(refNode.getAttributes());
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

            // Verify that all parameters can be parsed
            for (final Map.Entry<String, String> param : params.entrySet()) {
                try {
                    Variable.parse(param.getValue());
                } catch (final VariableException e) {
                    throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedParameterEvaluationErrorMessage"), param.getKey()), e);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "BehaviorRef[name=" + name + ",frequency=" + frequency + "]";
    }

    /**
     * Ensures the validity of any data loaded by this {@code BehaviorRef} object that
     * could not be validated when this {@code BehaviorRef} object was being initialized.
     * Specifically, this ensures that this {@code BehaviorRef} does not reference a nonexistent behavior.
     * <p>
     * This should be called after all data has been loaded into the parent
     * {@link Configuration} object of this {@code BehaviorRef}.
     *
     * @throws ConfigurationException if this {@code BehaviorRef} references a nonexistent behavior
     */
    public void validate() throws ConfigurationException {
        if (!configuration.getBehaviorNames().contains(name)) {
            throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("NoBehaviourFoundErrorMessage"), name));
        }
    }

    /**
     * Builds the behavior that is referenced by this {@code BehaviorRef}.
     *
     * @return the built behavior
     * @throws BehaviorInstantiationException if the referenced behavior fails to be built
     * @see BehaviorBuilder#buildBehavior(Map)
     */
    @Override
    public Behavior buildBehavior() throws BehaviorInstantiationException {
        return configuration.getBehaviorBuilders().get(name).buildBehavior(params);
    }

    /**
     * Checks whether this {@code BehaviorRef} object's conditions all evaluate
     * to {@code true} using the supplied context.
     *
     * @param context the context to use when evaluating this {@code BehaviorRef} object's conditions
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
     * Gets the name of the behavior referenced by this {@code BehaviorRef}.
     *
     * @return the name of the behavior referenced by this {@code BehaviorRef}
     */
    public String getName() {
        return name;
    }

    @Override
    public int getFrequency() {
        return frequency;
    }
}
