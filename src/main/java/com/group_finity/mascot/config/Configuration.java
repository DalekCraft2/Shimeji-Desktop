package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.behavior.UserBehavior;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableException;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * An object that represents all data contained within a mascot's configuration files
 * ({@code actions.xml}, {@code behaviors.xml}, and {@code info.xml}).
 * Automatically detects whether a configuration file uses the English or Japanese schema.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Configuration {
    private static final Logger log = LoggerFactory.getLogger(Configuration.class);

    private static final ResourceBundle SCHEMA_EN = ResourceBundle.getBundle("schema", Locale.US);
    private static final ResourceBundle SCHEMA_JA = ResourceBundle.getBundle("schema", Locale.JAPAN);

    private final Map<String, String> constants = new LinkedHashMap<>(2);
    private final Map<String, ActionBuilder> actionBuilders = new LinkedHashMap<>();
    private final Map<String, BehaviorBuilder> behaviorBuilders = new LinkedHashMap<>();
    private final Map<String, String> information = new LinkedHashMap<>(8);
    private ResourceBundle schema;

    public void load(final Entry configurationNode, final String imageSet) throws ConfigurationException {
        load(configurationNode, imageSet, false);
    }

    public void load(final Entry configurationNode, final String imageSet, boolean onlyLoadInfo) throws ConfigurationException {
        log.debug("Reading configuration file...");

        // check for Japanese XML tag and adapt locale accordingly
        String rootTagName = configurationNode.getName();
        if (rootTagName.equals(SCHEMA_JA.getString("Mascot"))) {
            schema = SCHEMA_JA;
        } else if (configurationNode.getName().equals(SCHEMA_EN.getString("Mascot"))) {
            schema = SCHEMA_EN;
        } else {
            throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("UnrecognizedRootTagNameErrorMessage"), rootTagName));
        }
        log.debug("Using {} schema", schema.getLocale().toLanguageTag());

        if (!onlyLoadInfo) {
            for (Entry constant : configurationNode.selectChildren(schema.getString("Constant"))) {
                constants.put(constant.getAttribute(schema.getString("Name")),
                        constant.getAttribute(schema.getString("Value")));
            }

            for (final Entry list : configurationNode.selectChildren(schema.getString("ActionList"))) {
                log.debug("Reading an action list...");

                for (final Entry node : list.selectChildren(schema.getString("Action"))) {
                    final ActionBuilder action;
                    try {
                        action = new ActionBuilder(this, node, imageSet);
                    } catch (ConfigurationException e) {
                        throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedLoadActionErrorMessage"), node.getAttributes()), e);
                    }

                    if (actionBuilders.containsKey(action.getName())) {
                        throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("DuplicateActionErrorMessage"), action.getName()));
                    }

                    actionBuilders.put(action.getName(), action);
                }
            }

            for (final Entry list : configurationNode.selectChildren(schema.getString("BehaviourList"))) {
                log.debug("Reading a behavior list...");

                loadBehaviors(list, List.of());
            }
        }

        for (final Entry list : configurationNode.selectChildren(schema.getString("Information"))) {
            log.debug("Reading an information group...");

            loadInformation(list);
        }

        log.debug("Configuration loaded successfully");
    }

    private void loadBehaviors(final Entry list, final List<String> conditions) throws ConfigurationException {
        for (final Entry node : list.getChildren()) {
            if (node.getName().equals(schema.getString("Condition"))) {
                final List<String> newConditions;
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

                loadBehaviors(node, newConditions);
            } else if (node.getName().equals(schema.getString("Behaviour"))) {
                final BehaviorBuilder behavior;
                try {
                    behavior = new BehaviorBuilder(this, node, conditions);
                } catch (ConfigurationException e) {
                    throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedLoadBehaviourErrorMessage"), node.getAttributes()), e);
                }

                if (behaviorBuilders.containsKey(behavior.getName())) {
                    throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("DuplicateBehaviourErrorMessage"), behavior.getName()));
                }

                behaviorBuilders.put(behavior.getName(), behavior);
            }
        }
    }

    private void loadInformation(final Entry list) {
        for (final Entry node : list.getChildren()) {
            String nodeName = node.getName();
            if (nodeName.equals(schema.getString("Name")) ||
                    nodeName.equals(schema.getString("PreviewImage")) ||
                    nodeName.equals(schema.getString("SplashImage"))) {
                information.put(nodeName, node.getText());
            } else if (nodeName.equals(schema.getString("Artist")) ||
                    nodeName.equals(schema.getString("Scripter")) ||
                    nodeName.equals(schema.getString("Commissioner")) ||
                    nodeName.equals(schema.getString("Support"))) {
                String nameText = node.hasAttribute(schema.getString("Name")) ? node.getAttribute(schema.getString("Name")) : null;
                String linkText = node.hasAttribute(schema.getString("URL")) ? node.getAttribute(schema.getString("URL")) : null;

                if (nameText != null) {
                    information.put(nodeName + schema.getString("Name"), nameText);
                    if (linkText != null) {
                        information.put(nodeName + schema.getString("URL"), linkText);
                    }
                }
            }
        }
    }

    public void validate() throws ConfigurationException {
        for (final ActionBuilder builder : actionBuilders.values()) {
            try {
                builder.validate();
            } catch (ConfigurationException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedValidateActionErrorMessage"), builder), e);
            }
        }
        for (final BehaviorBuilder builder : behaviorBuilders.values()) {
            try {
                builder.validate();
            } catch (ConfigurationException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedValidateBehaviourErrorMessage"), builder), e);
            }
        }

        // Ensure that all required behaviors are present
        String[] requiredBehaviors = {
                schema.getString(UserBehavior.BEHAVIORNAME_CHASEMOUSE),
                schema.getString(UserBehavior.BEHAVIORNAME_FALL),
                schema.getString(UserBehavior.BEHAVIORNAME_DRAGGED),
                schema.getString(UserBehavior.BEHAVIORNAME_THROWN)
        };
        StringBuilder stringBuilder = null;
        for (String requiredBehavior : requiredBehaviors) {
            if (!behaviorBuilders.containsKey(requiredBehavior)) {
                if (stringBuilder == null) {
                    stringBuilder = new StringBuilder(requiredBehavior);
                } else {
                    stringBuilder.append(", ").append(requiredBehavior);
                }
            }
        }
        if (stringBuilder != null) {
            throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("MissingRequiredBehaviourErrorMessage"), stringBuilder));
        }
    }

    public Action buildAction(final String name, final Map<String, String> params) throws ActionInstantiationException {
        final ActionBuilder builder = actionBuilders.get(name);
        if (builder == null) {
            throw new ActionInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString("NoCorrespondingActionFoundErrorMessage"), name));
        }

        return builder.buildAction(params);
    }

    public Behavior buildNextBehavior(final String previousName, final Mascot mascot) throws BehaviorInstantiationException {
        final VariableMap context = new VariableMap();
        context.putAll(constants); // put first so they can't override mascot
        context.put("mascot", mascot);

        final Collection<IBehaviorBuilder> candidates = new ArrayList<>();
        long totalFrequency = 0;

        final BehaviorBuilder prevBehaviorBuilder;
        if (previousName != null) {
            prevBehaviorBuilder = behaviorBuilders.get(previousName);
        } else {
            prevBehaviorBuilder = null;
        }

        if (prevBehaviorBuilder == null || prevBehaviorBuilder.isNextAdditive()) {
            for (final BehaviorBuilder behaviorBuilder : behaviorBuilders.values()) {
                try {
                    if (behaviorBuilder.isEffective(context) && isBehaviorEnabled(behaviorBuilder, mascot)) {
                        candidates.add(behaviorBuilder);
                        totalFrequency += behaviorBuilder.getFrequency();
                    }
                } catch (final VariableException e) {
                    log.warn("Failed to evaluate condition for behavior: {}", behaviorBuilder, e);
                }
            }
        }

        if (prevBehaviorBuilder != null) {
            for (final BehaviorRef behaviorBuilder : prevBehaviorBuilder.getNextBehaviorBuilders()) {
                try {
                    if (behaviorBuilder.isEffective(context) && isBehaviorEnabled(behaviorBuilder.getName(), mascot)) {
                        candidates.add(behaviorBuilder);
                        totalFrequency += behaviorBuilder.getFrequency();
                    }
                } catch (final VariableException e) {
                    log.warn("Failed to evaluate condition for behavior: {}", behaviorBuilder, e);
                }
            }
        }

        if (totalFrequency == 0) {
            Area area = Main.getInstance().getSettings().multiscreen
                    ? mascot.getEnvironment().getScreen() : mascot.getEnvironment().getWorkArea();
            // Subtract 2 from the width and add 1 to the left border X value so the mascot doesn't start climbing the walls instead of falling
            mascot.getAnchor().setLocation((int) (Math.random() * (area.getWidth() - 2)) + area.getLeft() + 1, area.getTop() - 256);
            return buildBehavior(schema.getString(UserBehavior.BEHAVIORNAME_FALL));
        }

        double random = Math.random() * totalFrequency;

        for (final IBehaviorBuilder behaviorBuilder : candidates) {
            random -= behaviorBuilder.getFrequency();
            if (random < 0) {
                return behaviorBuilder.buildBehavior();
            }
        }

        return null;
    }

    public Behavior buildBehavior(final String name, final Mascot mascot) throws BehaviorInstantiationException {
        if (behaviorBuilders.containsKey(name)) {
            if (isBehaviorEnabled(name, mascot)) {
                return behaviorBuilders.get(name).buildBehavior();
            } else {
                Area area = Main.getInstance().getSettings().multiscreen
                        ? mascot.getEnvironment().getScreen() : mascot.getEnvironment().getWorkArea();
                // Subtract 2 from the width and add 1 to the left border X value so the mascot doesn't start climbing the walls instead of falling
                mascot.getAnchor().setLocation((int) (Math.random() * (area.getWidth() - 2)) + area.getLeft() + 1, area.getTop() - 256);
                return buildBehavior(schema.getString(UserBehavior.BEHAVIORNAME_FALL));
            }
        } else {
            throw new BehaviorInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString("NoBehaviourFoundErrorMessage"), name), name);
        }
    }

    public Behavior buildBehavior(final String name) throws BehaviorInstantiationException {
        if (behaviorBuilders.containsKey(name)) {
            return behaviorBuilders.get(name).buildBehavior();
        } else {
            throw new BehaviorInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString("NoBehaviourFoundErrorMessage"), name), name);
        }
    }

    public boolean isBehaviorEnabled(final BehaviorBuilder builder, final Mascot mascot) {
        if (builder.isToggleable() && Main.getInstance().getSettings().disabledBehaviors.containsKey(mascot.getImageSet())) {
            return !Main.getInstance().getSettings().disabledBehaviors.get(mascot.getImageSet()).contains(builder.getName());
        }
        return true;
    }

    public boolean isBehaviorEnabled(final String name, final Mascot mascot) {
        if (behaviorBuilders.containsKey(name)) {
            return isBehaviorEnabled(behaviorBuilders.get(name), mascot);
        } else {
            return false;
        }
    }

    public boolean isBehaviorHidden(final String name) {
        if (behaviorBuilders.containsKey(name)) {
            return behaviorBuilders.get(name).isHidden();
        } else {
            return false;
        }
    }

    public boolean isBehaviorToggleable(final String name) {
        if (behaviorBuilders.containsKey(name)) {
            return behaviorBuilders.get(name).isToggleable();
        } else {
            return false;
        }
    }

    Map<String, ActionBuilder> getActionBuilders() {
        return actionBuilders;
    }

    Map<String, BehaviorBuilder> getBehaviorBuilders() {
        return behaviorBuilders;
    }

    public Set<String> getBehaviorNames() {
        return behaviorBuilders.keySet();
    }

    public boolean containsInformationKey(String key) {
        return information.containsKey(key);
    }

    public String getInformation(String key) {
        return information.get(key);
    }

    public ResourceBundle getSchema() {
        return schema;
    }
}
