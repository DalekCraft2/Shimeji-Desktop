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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class BehaviorBuilder {

    private static final Logger log = Logger.getLogger(BehaviorBuilder.class.getName());

    private final Configuration configuration;

    private final String name;

    private final String actionName;

    private final int frequency;

    private final List<String> conditions;

    private final boolean hidden;

    private final boolean toggleable;

    private final boolean nextAdditive;

    private final List<BehaviorBuilder> nextBehaviorBuilders = new ArrayList<>();

    private final Map<String, String> params = new LinkedHashMap<>();

    public BehaviorBuilder(final Configuration configuration, final Entry behaviorNode, final List<String> conditions) {
        this.configuration = configuration;
        name = behaviorNode.getAttribute(configuration.getSchema().getString("Name"));
        actionName = behaviorNode.getAttribute(configuration.getSchema().getString("Action")) == null ? getName() : behaviorNode.getAttribute(configuration.getSchema().getString("Action"));
        frequency = Integer.parseInt(behaviorNode.getAttribute(configuration.getSchema().getString("Frequency")));
        hidden = Boolean.parseBoolean(behaviorNode.getAttribute(configuration.getSchema().getString("Hidden")));
        this.conditions = new ArrayList<>(conditions);
        getConditions().add(behaviorNode.getAttribute(configuration.getSchema().getString("Condition")));

        // override of toggleable state for required fields
        if (name.equals(UserBehavior.BEHAVIOURNAME_FALL) ||
                name.equals(UserBehavior.BEHAVIOURNAME_THROWN) ||
                name.equals(UserBehavior.BEHAVIOURNAME_DRAGGED)) {
            toggleable = false;
        } else {
            toggleable = Boolean.parseBoolean(behaviorNode.getAttribute(configuration.getSchema().getString("Toggleable")));
        }

        log.log(Level.INFO, "Loading behavior: {0}", this);

        getParams().putAll(behaviorNode.getAttributes());
        getParams().remove(configuration.getSchema().getString("Name"));
        getParams().remove(configuration.getSchema().getString("Action"));
        getParams().remove(configuration.getSchema().getString("Frequency"));
        getParams().remove(configuration.getSchema().getString("Hidden"));
        getParams().remove(configuration.getSchema().getString("Condition"));

        boolean nextAdditive = true;

        for (final Entry nextList : behaviorNode.selectChildren(configuration.getSchema().getString("NextBehaviourList"))) {
            nextAdditive = Boolean.parseBoolean(nextList.getAttribute(configuration.getSchema().getString("Add")));

            loadBehaviors(nextList, new ArrayList<>());
        }

        this.nextAdditive = nextAdditive;

        log.log(Level.INFO, "Finished loading behavior: {0}", this);
    }

    @Override
    public String toString() {
        return "Behavior(" + getName() + "," + getFrequency() + "," + getActionName() + ")";
    }

    private void loadBehaviors(final Entry list, final List<String> conditions) {
        for (final Entry node : list.getChildren()) {

            if (node.getName().equals(configuration.getSchema().getString("Condition"))) {

                final List<String> newConditions = new ArrayList<>(conditions);
                newConditions.add(node.getAttribute(configuration.getSchema().getString("Condition")));

                loadBehaviors(node, newConditions);

            } else if (node.getName().equals(configuration.getSchema().getString("BehaviourReference"))) {
                final BehaviorBuilder behavior = new BehaviorBuilder(getConfiguration(), node, conditions);
                getNextBehaviorBuilders().add(behavior);
            }
        }
    }

    public void validate() throws ConfigurationException {
        if (!getConfiguration().getActionBuilders().containsKey(getActionName())) {
            log.log(Level.SEVERE, "There is no corresponding action for behavior: {0}", this);
            throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString("NoActionFoundErrorMessage") + "(" + this + ")");
        }
    }

    public Behavior buildBehavior() throws BehaviorInstantiationException {
        try {
            return new UserBehavior(getName(),
                    getConfiguration().buildAction(getActionName(),
                            getParams()), getConfiguration());
        } catch (final ActionInstantiationException e) {
            log.log(Level.SEVERE, "Failed to initialize the corresponding action for behavior: " + this, e);
            throw new BehaviorInstantiationException(Main.getInstance().getLanguageBundle().getString("FailedInitialiseCorrespondingActionErrorMessage") + "(" + this + ")", e);
        }
    }


    public boolean isEffective(final VariableMap context) throws VariableException {
        if (frequency == 0) {
            return false;
        }

        for (final String condition : getConditions()) {
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

    public int getFrequency() {
        return frequency;
    }

    public boolean isHidden() {
        return hidden;
    }

    public boolean isToggleable() {
        return toggleable;
    }

    private String getActionName() {
        return actionName;
    }

    private Map<String, String> getParams() {
        return params;
    }

    private List<String> getConditions() {
        return conditions;
    }

    private Configuration getConfiguration() {
        return configuration;
    }

    public boolean isNextAdditive() {
        return nextAdditive;
    }

    public List<BehaviorBuilder> getNextBehaviorBuilders() {
        return nextBehaviorBuilders;
    }
}
