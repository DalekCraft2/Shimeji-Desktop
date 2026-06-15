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

    /**
     * The English configuration schema. This is used for Shimeji-ee's configuration files.
     */
    private static final ResourceBundle SCHEMA_EN = ResourceBundle.getBundle("schema", Locale.US);

    /**
     * The Japanese configuration schema. This is used for the original Shimeji's configuration files.
     */
    private static final ResourceBundle SCHEMA_JA = ResourceBundle.getBundle("schema", Locale.JAPAN);

    /**
     * A map of user-defined constants that are added to the script context when
     * determining whether a behavior's conditions evaluate to {@code true}.
     *
     * @see #buildNextBehavior(String, Mascot)
     */
    private final Map<String, String> constants = new LinkedHashMap<>(2);

    /**
     * The builders for the top-level actions that have been loaded by this {@code Configuration}.
     *
     * @see #getActionBuilders()
     */
    private final Map<String, ActionBuilder> actionBuilders = new LinkedHashMap<>();

    /**
     * The builders for the top-level behaviors that have been loaded by this {@code Configuration}.
     *
     * @see #getBehaviorBuilders()
     * @see #getBehaviorNames()
     */
    private final Map<String, BehaviorBuilder> behaviorBuilders = new LinkedHashMap<>();

    /**
     * A map of information about this {@code Configuration}.
     */
    // TODO: Consider refactoring this into multiple fields
    private final Map<String, String> information = new LinkedHashMap<>(8);

    /**
     * The schema used by this {@code Configuration}.
     * <p>
     * This value is overwritten whenever {@link #load} is called. If a caller intends to use this schema after
     * {@code load} has been called, the caller should store the schema in a variable to avoid potential issues
     * caused by this schema being changed from future calls to {@code load}.
     *
     * @see #SCHEMA_EN
     * @see #SCHEMA_JA
     * @see #getSchema()
     */
    private ResourceBundle schema;

    /**
     * Creates a new {@code Configuration} from the data contained within the specified XML node.
     *
     * @param configurationNode the XML node from which to load the configuration. This should be the root node
     * of a configuration file.
     * @param imageSet the name of this {@code Configuration} object's associated image set
     * @throws ConfigurationException if an error occurs whilst reading the configuration node, or if the
     * configuration node contains invalid data
     */
    public void load(final Entry configurationNode, final String imageSet) throws ConfigurationException {
        load(configurationNode, imageSet, false);
    }

    /**
     * Creates a new {@code Configuration} from the data contained within the specified XML node.
     *
     * @param configurationNode the XML node from which to load the configuration. This should be the root node
     * of a configuration file.
     * @param imageSet the name of this {@code Configuration} object's associated image set
     * @param onlyLoadInfo whether to only read Information nodes when loading this {@code Configuration}
     * @throws ConfigurationException if an error occurs whilst reading the configuration node, or if the
     * configuration node contains invalid data
     */
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
        if (log.isDebugEnabled()) {
            log.debug("Using {} schema", schema.getLocale().toLanguageTag());
        }

        if (!onlyLoadInfo) {
            List<Entry> constantNodes = configurationNode.selectChildren(schema.getString("Constant"));
            if (!constantNodes.isEmpty()) {
                for (Entry constantNode : constantNodes) {
                    constants.put(constantNode.getAttribute(schema.getString("Name")),
                            constantNode.getAttribute(schema.getString("Value")));
                }
            }

            List<Entry> actionLists = configurationNode.selectChildren(schema.getString("ActionList"));
            if (!actionLists.isEmpty()) {
                for (final Entry actionList : actionLists) {
                    log.debug("Reading an action list...");

                    List<Entry> actionNodes = actionList.selectChildren(schema.getString("Action"));
                    if (!actionNodes.isEmpty()) {
                        for (final Entry actionNode : actionNodes) {
                            final ActionBuilder action;
                            try {
                                action = new ActionBuilder(this, actionNode, imageSet);
                            } catch (ConfigurationException | RuntimeException e) {
                                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                                        "FailedLoadActionErrorMessage"), actionNode.getAttributes()), e);
                            }

                            if (actionBuilders.containsKey(action.getName())) {
                                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                                        "DuplicateActionErrorMessage"), action.getName()));
                            }

                            actionBuilders.put(action.getName(), action);
                        }
                    }
                }
            }

            List<Entry> behaviorLists = configurationNode.selectChildren(schema.getString("BehaviourList"));
            if (!behaviorLists.isEmpty()) {
                for (final Entry behaviorList : behaviorLists) {
                    log.debug("Reading a behavior list...");

                    loadBehaviors(behaviorList, List.of());
                }
            }
        }

        List<Entry> infoNodes = configurationNode.selectChildren(schema.getString("Information"));
        if (!infoNodes.isEmpty()) {
            for (final Entry infoNode : infoNodes) {
                log.debug("Reading an information group...");

                loadInformation(infoNode);
            }
        }

        log.debug("Configuration loaded successfully");
    }

    /**
     * Loads behaviors from the specified BehaviorList/Condition node.
     * If a Condition node is present in the specified node's children, then the specified list of conditions is copied,
     * the new condition is appended to it, and the method recurses using the new Condition node and the copied list.
     *
     * @param list the BehaviorList/Condition node from which to load behaviors
     * @param conditions a list containing the conditions from the specified node and all of its parent nodes.
     * The behaviors in the specified node can only be executed when all of these conditions evaluate to {@code true}.
     * @throws ConfigurationException if a behavior in the specified list cannot be loaded or a condition cannot be parsed
     */
    private void loadBehaviors(final Entry list, final List<String> conditions) throws ConfigurationException {
        List<Entry> children = list.getChildren();
        if (children.isEmpty()) {
            return;
        }
        for (final Entry node : children) {
            if (node.getName().equals(schema.getString("Condition"))) {
                final List<String> newConditions;
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

                loadBehaviors(node, newConditions);
            } else if (node.getName().equals(schema.getString("Behaviour"))) {
                final BehaviorBuilder behavior;
                try {
                    behavior = new BehaviorBuilder(this, node, conditions);
                } catch (ConfigurationException | RuntimeException e) {
                    throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                            "FailedLoadBehaviourErrorMessage"), node.getAttributes()), e);
                }

                if (behaviorBuilders.containsKey(behavior.getName())) {
                    throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                            "DuplicateBehaviourErrorMessage"), behavior.getName()));
                }

                behaviorBuilders.put(behavior.getName(), behavior);
            }
        }
    }

    /**
     * Loads information about this {@code Configuration} from the specified Information node.
     *
     * @param infoNode the node from which to load the information
     */
    private void loadInformation(final Entry infoNode) {
        List<Entry> children = infoNode.getChildren();
        if (children.isEmpty()) {
            return;
        }
        for (final Entry node : children) {
            String nodeName = node.getName();
            if (nodeName.equals(schema.getString("Name")) ||
                    nodeName.equals(schema.getString("PreviewImage")) ||
                    nodeName.equals(schema.getString("SplashImage"))) {
                information.put(nodeName, node.getText());
            } else if (nodeName.equals(schema.getString("Artist")) ||
                    nodeName.equals(schema.getString("Scripter")) ||
                    nodeName.equals(schema.getString("Commissioner")) ||
                    nodeName.equals(schema.getString("Support"))) {
                String nameText = node.hasAttribute(schema.getString("Name")) ?
                        node.getAttribute(schema.getString("Name")) : null;
                String linkText = node.hasAttribute(schema.getString("URL")) ?
                        node.getAttribute(schema.getString("URL")) : null;

                if (nameText != null) {
                    information.put(nodeName + schema.getString("Name"), nameText);
                    if (linkText != null) {
                        information.put(nodeName + schema.getString("URL"), linkText);
                    }
                }
            }
        }
    }

    /**
     * Ensures the validity of any data loaded by this {@code Configuration} object that
     * could not be validated when this {@code Configuration} object was still loading data.
     * Specifically, this ensures that the actions, hotspots, and behaviors loaded by this
     * {@code Configuration} object do not contain references to nonexistent actions/behaviors.
     * It also ensures that all four required behaviors ({@link UserBehavior#BEHAVIORNAME_CHASEMOUSE ChaseMouse},
     * {@link UserBehavior#BEHAVIORNAME_FALL Fall}, {@link UserBehavior#BEHAVIORNAME_DRAGGED Dragged}, and
     * {@link UserBehavior#BEHAVIORNAME_THROWN Thrown}) are present.
     * <p>
     * This should be called after all data has been loaded into this {@code Configuration} object.
     *
     * @throws ConfigurationException if an action, hotspot, or behavior in this {@code Configuration} contains a
     * reference to a nonexistent action/behavior, or at least one of the required behaviors is missing
     * @see ActionBuilder#validate()
     * @see AnimationBuilder#validate()
     * @see BehaviorBuilder#validate()
     */
    public void validate() throws ConfigurationException {
        if (!actionBuilders.isEmpty()) {
            for (final ActionBuilder builder : actionBuilders.values()) {
                try {
                    builder.validate();
                } catch (ConfigurationException e) {
                    throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                            "FailedValidateActionErrorMessage"), builder), e);
                }
            }
        }
        if (!behaviorBuilders.isEmpty()) {
            for (final BehaviorBuilder builder : behaviorBuilders.values()) {
                try {
                    builder.validate();
                } catch (ConfigurationException e) {
                    throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                            "FailedValidateBehaviourErrorMessage"), builder), e);
                }
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
            throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                    "MissingRequiredBehaviourErrorMessage"), stringBuilder));
        }
    }

    /**
     * Creates a new instance of the action with the specified name, and adds the specified parameters
     * to its context. If a parameter name is present in both the specified parameters and the specified
     * action's existing parameters, the existing parameter will be overwritten by the specified parameter.
     *
     * @param name the name of the action to build
     * @param params a map of parameter names and values to add to the context of the built action
     * @return the built action
     * @throws ActionInstantiationException if the action with the specified name does not exist or otherwise fails
     * to be built
     * @see ActionBuilder#buildAction(Map)
     */
    public Action buildAction(final String name, final Map<String, String> params) throws ActionInstantiationException {
        final ActionBuilder builder = actionBuilders.get(name);
        if (builder == null) {
            throw new ActionInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString(
                    "NoCorrespondingActionFoundErrorMessage"), name));
        }

        return builder.buildAction(params);
    }

    /**
     * Builds the next behavior for the behavior with the specified name. The returned behavior will be randomly chosen
     * based on the frequencies of the candidates for the next behavior. Behaviors with larger frequencies have a
     * greater chance of being chosen.
     * <p>
     * The candidates for the next behavior are selected from the specified behavior's
     * {@linkplain BehaviorBuilder#getNextBehaviorBuilders() next behavior list}. If the specified behavior is
     * {@linkplain BehaviorBuilder#isNextAdditive() next additive}, other top-level behaviors may be added to the
     * list of candidates as well.
     * <p>
     * In order to be a candidate for the next behavior, a behavior must fulfill three requirements:
     * <ul>
     *     <li>It must have a frequency greater than 0.</li>
     *     <li>It must be effective (meaning that all of its conditions must evaluate to {@code true}).</li>
     *     <li>It must be enabled for the image set used by the specified mascot.</li>
     * </ul>
     * <p>
     * If there are no candidates for the next behavior, then the specified mascot's position will be set above
     * the top of the screen, and the "Fall" behavior will be returned.
     *
     * @param previousName the name of the behavior whose next behavior should be built
     * @param mascot the mascot to use when checking the enabled states of the specified behavior's children
     * @return the built behavior, or the "Fall" behavior if there are no candidates for the next behavior
     * @throws BehaviorInstantiationException if the next behavior fails to be built
     * @see IBehaviorBuilder#buildBehavior()
     * @see IBehaviorBuilder#getFrequency()
     * @see BehaviorBuilder#isNextAdditive()
     * @see BehaviorBuilder#isEffective(VariableMap)
     * @see BehaviorRef#isEffective(VariableMap)
     */
    public Behavior buildNextBehavior(final String previousName, final Mascot mascot) throws BehaviorInstantiationException {
        final VariableMap context = new VariableMap();
        if (!constants.isEmpty()) {
            context.putAll(constants); // put first so they can't override mascot
        }
        context.put("mascot", mascot);

        final Collection<IBehaviorBuilder> candidates = new ArrayList<>();
        long totalFrequency = 0;

        final BehaviorBuilder prevBehaviorBuilder;
        if (previousName != null) {
            prevBehaviorBuilder = behaviorBuilders.get(previousName);
        } else {
            prevBehaviorBuilder = null;
        }

        // If the previous behavior builder is next additive,
        // allow the top-level behaviors to be candidates for the next behavior
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

        // If the previous behavior has next behaviors, iterate through them to find valid candidates
        // for the next behavior
        if (prevBehaviorBuilder != null && !prevBehaviorBuilder.getNextBehaviorBuilders().isEmpty()) {
            for (final BehaviorRef behaviorRef : prevBehaviorBuilder.getNextBehaviorBuilders()) {
                try {
                    if (behaviorRef.isEffective(context) && isBehaviorEnabled(behaviorRef.getName(), mascot)) {
                        candidates.add(behaviorRef);
                        totalFrequency += behaviorRef.getFrequency();
                    }
                } catch (final VariableException e) {
                    log.warn("Failed to evaluate condition for behavior: {}", behaviorRef, e);
                }
            }
        }

        // If there are no candidates for the next behavior, set the mascot's position
        // to be above the top of the screen, and return the "Fall" behavior
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

        /* TODO: Move the fallback "Fall" behavior code down here to replace this null return,
            because this can be reached if a behavior has a negative frequency.
            Also, ensure that frequencies are not negative when loading behaviors. */
        return null;
    }

    /**
     * Creates a new instance of the behavior with the specified name.
     * If the specified behavior is not enabled for the specified mascot, the mascot's position will be set above the
     * top of the screen, and the "Fall" behavior will be returned.
     *
     * @param name the name of the behavior to build
     * @param mascot the mascot to use when checking whether the specified behavior is enabled
     * @return the built behavior, or the "Fall" behavior if the specified behavior is not enabled for the
     * specified mascot
     * @throws BehaviorInstantiationException if a behavior with the specified name does not exist, or the specified
     * behavior fails to be built
     * @see #isBehaviorEnabled(String, Mascot)
     * @see BehaviorBuilder#buildBehavior()
     */
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
            throw new BehaviorInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString(
                    "NoBehaviourFoundErrorMessage"), name), name);
        }
    }

    /**
     * Creates a new instance of the behavior with the specified name.
     *
     * @param name the name of the behavior to build
     * @return the built behavior
     * @throws BehaviorInstantiationException if a behavior with the specified name does not exist, or the specified
     * behavior fails to be built
     * @see BehaviorBuilder#buildBehavior()
     */
    public Behavior buildBehavior(final String name) throws BehaviorInstantiationException {
        if (behaviorBuilders.containsKey(name)) {
            return behaviorBuilders.get(name).buildBehavior();
        } else {
            throw new BehaviorInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString(
                    "NoBehaviourFoundErrorMessage"), name), name);
        }
    }

    /**
     * Checks whether the behavior associated with the specified behavior builder is enabled for the specified mascot.
     *
     * @param builder the builder of the behavior whose enabled state is to be checked
     * @param mascot the mascot to use when checking whether the specified behavior is enabled
     * @return {@code true} if the specified behavior is enabled for the image set used by the specified mascot;
     * {@code false} otherwise
     */
    public boolean isBehaviorEnabled(final BehaviorBuilder builder, final Mascot mascot) {
        if (builder.isToggleable() && Main.getInstance().getSettings().disabledBehaviors.containsKey(mascot.getImageSet())) {
            return !Main.getInstance().getSettings().disabledBehaviors.get(mascot.getImageSet()).contains(builder.getName());
        }
        return true;
    }

    /**
     * Checks whether the behavior with the specified name is enabled for the specified mascot.
     *
     * @param name the name of the behavior whose enabled state is to be checked
     * @param mascot the mascot to use when checking whether the specified behavior is enabled
     * @return {@code true} if the specified behavior is enabled for the image set used by the specified mascot;
     * {@code false} otherwise
     */
    public boolean isBehaviorEnabled(final String name, final Mascot mascot) {
        if (behaviorBuilders.containsKey(name)) {
            return isBehaviorEnabled(behaviorBuilders.get(name), mascot);
        } else {
            return false;
        }
    }

    /**
     * Checks whether the behavior with the specified name is hidden from the "Set Behavior" submenu in the mascot
     * context menu.
     *
     * @param name the name of the behavior whose hidden state is to be checked
     * @return {@code true} if the specified behavior is hidden from the "Set Behavior" submenu; {@code false} otherwise
     */
    public boolean isBehaviorHidden(final String name) {
        if (behaviorBuilders.containsKey(name)) {
            return behaviorBuilders.get(name).isHidden();
        } else {
            return false;
        }
    }

    /**
     * Checks whether the behavior with the specified name can be toggled via the "Allowed Behaviors" submenu in the
     * mascot context menu.
     *
     * @param name the name of the behavior whose toggleability is to be checked
     * @return {@code true} if the specified behavior can be toggled; {@code false} otherwise
     */
    public boolean isBehaviorToggleable(final String name) {
        if (behaviorBuilders.containsKey(name)) {
            return behaviorBuilders.get(name).isToggleable();
        } else {
            return false;
        }
    }

    /**
     * Gets the builders for the top-level actions that have been loaded by this {@code Configuration}.
     *
     * @return the top-level action builders loaded by this {@code Configuration}
     */
    Map<String, ActionBuilder> getActionBuilders() {
        return actionBuilders;
    }

    /**
     * Gets the builders for the top-level behaviors that have been loaded by this {@code Configuration}.
     *
     * @return the top-level behavior builders loaded by this {@code Configuration}
     * @see #getBehaviorNames()
     */
    Map<String, BehaviorBuilder> getBehaviorBuilders() {
        return behaviorBuilders;
    }

    /**
     * Gets the names of the top-level behaviors that have been loaded by this {@code Configuration}.
     *
     * @return the names of the top-level behaviors that have been loaded by this {@code Configuration}
     */
    public Set<String> getBehaviorNames() {
        return behaviorBuilders.keySet();
    }

    /**
     * Checks whether the information of this {@code Configuration} contains the specified key.
     *
     * @param key the key whose presence in the information of this {@code Configuration} is to be tested
     * @return {@code true} if the information of this {@code Configuration} contains a mapping for the specified key;
     * {@code false} otherwise
     */
    public boolean containsInformationKey(String key) {
        return information.containsKey(key);
    }

    /**
     * Gets the information value that is mapped to the specified key.
     *
     * @param key the key whose associated value is to be returned
     * @return the specified key's associated value, or {@code null} if the information of this {@code Configuration}
     * does not contain a mapping for the specified key
     */
    public String getInformation(String key) {
        return information.get(key);
    }

    /**
     * Gets the schema used by this {@code Configuration}.
     * <p>
     * This value is overwritten whenever {@link #load} is called. If a caller intends to use this schema after
     * {@code load} has been called, the caller should store the schema in a variable to avoid potential issues
     * caused by this schema being changed from future calls to {@code load}.
     *
     * @return the schema used by this {@code Configuration}
     */
    public ResourceBundle getSchema() {
        return schema;
    }
}
