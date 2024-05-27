package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.behavior.UserBehavior;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class Configuration {
    private static final Logger log = Logger.getLogger(Configuration.class.getName());
    private final Map<String, String> constants = new LinkedHashMap<>(2);
    private final Map<String, ActionBuilder> actionBuilders = new LinkedHashMap<>();
    private final Map<String, BehaviorBuilder> behaviorBuilders = new LinkedHashMap<>();
    private final Map<String, String> information = new LinkedHashMap<>(8);
    private ResourceBundle schema;

    public void load(final Entry configurationNode, final String imageSet) throws IOException, ConfigurationException {
        log.log(Level.INFO, "Reading configuration file...");

        // prepare schema
        Locale locale;

        // check for Japanese XML tag and adapt locale accordingly
        if (configurationNode.hasChild("\u52D5\u4F5C\u30EA\u30B9\u30C8") ||
                configurationNode.hasChild("\u884C\u52D5\u30EA\u30B9\u30C8")) {
            locale = Locale.JAPAN;
        } else {
            locale = Locale.US;
        }
        log.log(Level.INFO, "Using " + locale.toLanguageTag() + " schema");

        URL[] urls = {Main.CONFIG_DIRECTORY.toUri().toURL()};
        try (URLClassLoader loader = new URLClassLoader(urls)) {
            // ResourceBundle.Control utf8Control = new Utf8ResourceBundleControl(false);
            // schema = ResourceBundle.getBundle("schema", locale, loader, utf8Control);
            schema = ResourceBundle.getBundle("schema", locale, loader);
        }

        for (Entry constant : configurationNode.selectChildren(schema.getString("Constant"))) {
            getConstants().put(constant.getAttribute(schema.getString("Name")),
                    constant.getAttribute(schema.getString("Value")));
        }

        log.log(Level.INFO, "Reading action lists");
        for (final Entry list : configurationNode.selectChildren(schema.getString("ActionList"))) {
            log.log(Level.INFO, "Reading an action list...");

            for (final Entry node : list.selectChildren(schema.getString("Action"))) {
                final ActionBuilder action = new ActionBuilder(this, node, imageSet);

                if (getActionBuilders().containsKey(action.getName())) {
                    throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString("DuplicateActionErrorMessage") + ": " + action.getName());
                }

                getActionBuilders().put(action.getName(), action);
            }

            log.log(Level.INFO, "Finished reading an action list");
        }
        log.log(Level.INFO, "Finished reading all action lists");

        log.log(Level.INFO, "Reading behavior lists");
        for (final Entry list : configurationNode.selectChildren(schema.getString("BehaviourList"))) {
            log.log(Level.INFO, "Reading a behavior list...");

            loadBehaviors(list, new ArrayList<>());

            log.log(Level.INFO, "Finished reading a behavior list");
        }
        log.log(Level.INFO, "Finished reading all behavior lists");

        log.log(Level.INFO, "Reading information");
        for (final Entry list : configurationNode.selectChildren(schema.getString("Information"))) {
            log.log(Level.INFO, "Reading an information group...");

            loadInformation(list);

            log.log(Level.INFO, "Finished reading information group");
        }
        log.log(Level.INFO, "Finished reading all information");

        log.log(Level.INFO, "Configuration loaded successfully");
    }

    private void loadBehaviors(final Entry list, final List<String> conditions) {
        for (final Entry node : list.getChildren()) {
            if (node.getName().equals(schema.getString("Condition"))) {
                final List<String> newConditions = new ArrayList<>(conditions);
                newConditions.add(node.getAttribute(schema.getString("Condition")));

                loadBehaviors(node, newConditions);
            } else if (node.getName().equals(schema.getString("Behaviour"))) {
                final BehaviorBuilder behavior = new BehaviorBuilder(this, node, conditions);
                getBehaviorBuilders().put(behavior.getName(), behavior);
            }
        }
    }

    public Action buildAction(final String name, final Map<String, String> params) throws ActionInstantiationException {
        final ActionBuilder factory = actionBuilders.get(name);
        if (factory == null) {
            throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getString("NoCorrespondingActionFoundErrorMessage") + ": " + name);
        }

        return factory.buildAction(params);
    }

    private void loadInformation(final Entry list) {
        for (final Entry node : list.getChildren()) {
            if (node.getName().equals(schema.getString("Name")) ||
                    node.getName().equals(schema.getString("PreviewImage")) ||
                    node.getName().equals(schema.getString("SplashImage"))) {
                information.put(node.getName(), node.getText());
            } else if (node.getName().equals(schema.getString("Artist")) ||
                    node.getName().equals(schema.getString("Scripter")) ||
                    node.getName().equals(schema.getString("Commissioner")) ||
                    node.getName().equals(schema.getString("Support"))) {
                String nameText = node.getAttribute(schema.getString("Name")) != null ? node.getAttribute(schema.getString("Name")) : null;
                String linkText = node.getAttribute(schema.getString("URL")) != null ? node.getAttribute(schema.getString("URL")) : null;

                if (nameText != null) {
                    information.put(node.getName() + schema.getString("Name"), nameText);
                    if (linkText != null) {
                        information.put(node.getName() + schema.getString("URL"), linkText);
                    }
                }
            }
        }
    }

    public void validate() throws ConfigurationException {
        for (final ActionBuilder builder : getActionBuilders().values()) {
            builder.validate();
        }
        for (final BehaviorBuilder builder : getBehaviorBuilders().values()) {
            builder.validate();
        }
    }

    public Behavior buildNextBehavior(final String previousName, final Mascot mascot) throws BehaviorInstantiationException {
        final VariableMap context = new VariableMap();
        context.putAll(getConstants()); // put first so they can't override mascot
        context.put("mascot", mascot);

        final Collection<BehaviorBuilder> candidates = new ArrayList<>();
        long totalFrequency = 0;
        for (final BehaviorBuilder behaviorFactory : getBehaviorBuilders().values()) {
            try {
                if (behaviorFactory.isEffective(context) && isBehaviorEnabled(behaviorFactory, mascot)) {
                    candidates.add(behaviorFactory);
                    totalFrequency += behaviorFactory.getFrequency();
                }
            } catch (final VariableException e) {
                log.log(Level.WARNING, "Failed to calculate the frequency of the behavior", e);
            }
        }

        if (previousName != null) {
            final BehaviorBuilder previousBehaviorFactory = getBehaviorBuilders().get(previousName);
            if (!previousBehaviorFactory.isNextAdditive()) {
                totalFrequency = 0;
                candidates.clear();
            }
            for (final BehaviorBuilder behaviorFactory : previousBehaviorFactory.getNextBehaviorBuilders()) {
                try {
                    if (behaviorFactory.isEffective(context) && isBehaviorEnabled(behaviorFactory, mascot)) {
                        candidates.add(behaviorFactory);
                        totalFrequency += behaviorFactory.getFrequency();
                    }
                } catch (final VariableException e) {
                    log.log(Level.WARNING, "Failed to calculate the frequency of the behavior", e);
                }
            }
        }

        if (totalFrequency == 0) {
            if (Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Multiscreen", "true"))) {
                mascot.setAnchor(new Point((int) (Math.random() * (mascot.getEnvironment().getScreen().getRight() - mascot.getEnvironment().getScreen().getLeft())) + mascot.getEnvironment().getScreen().getLeft(),
                        mascot.getEnvironment().getScreen().getTop() - 256));
            } else {
                mascot.setAnchor(new Point((int) (Math.random() * (mascot.getEnvironment().getWorkArea().getRight() - mascot.getEnvironment().getWorkArea().getLeft())) + mascot.getEnvironment().getWorkArea().getLeft(),
                        mascot.getEnvironment().getWorkArea().getTop() - 256));
            }
            return buildBehavior(schema.getString(UserBehavior.BEHAVIOURNAME_FALL));
        }

        double random = Math.random() * totalFrequency;

        for (final BehaviorBuilder behaviorFactory : candidates) {
            random -= behaviorFactory.getFrequency();
            if (random < 0) {
                return behaviorFactory.buildBehavior();
            }
        }

        return null;
    }

    public Behavior buildBehavior(final String name, final Mascot mascot) throws BehaviorInstantiationException {
        if (behaviorBuilders.containsKey(name)) {
            if (isBehaviorEnabled(name, mascot)) {
                return getBehaviorBuilders().get(name).buildBehavior();
            } else {
                if (Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Multiscreen", "true"))) {
                    mascot.setAnchor(new Point((int) (Math.random() * (mascot.getEnvironment().getScreen().getRight() - mascot.getEnvironment().getScreen().getLeft())) + mascot.getEnvironment().getScreen().getLeft(),
                            mascot.getEnvironment().getScreen().getTop() - 256));
                } else {
                    mascot.setAnchor(new Point((int) (Math.random() * (mascot.getEnvironment().getWorkArea().getRight() - mascot.getEnvironment().getWorkArea().getLeft())) + mascot.getEnvironment().getWorkArea().getLeft(),
                            mascot.getEnvironment().getWorkArea().getTop() - 256));
                }
                return buildBehavior(schema.getString(UserBehavior.BEHAVIOURNAME_FALL));
            }
        } else {
            throw new BehaviorInstantiationException(Main.getInstance().getLanguageBundle().getString("NoBehaviourFoundErrorMessage") + " (" + name + ")");
        }
    }

    public Behavior buildBehavior(final String name) throws BehaviorInstantiationException {
        if (behaviorBuilders.containsKey(name)) {
            return getBehaviorBuilders().get(name).buildBehavior();
        } else {
            throw new BehaviorInstantiationException(Main.getInstance().getLanguageBundle().getString("NoBehaviourFoundErrorMessage") + " (" + name + ")");
        }
    }

    public boolean isBehaviorEnabled(final BehaviorBuilder builder, final Mascot mascot) {
        if (builder.isToggleable()) {
            return Arrays.stream(Main.getInstance().getProperties().getProperty("DisabledBehaviours." + mascot.getImageSet(), "").split("/")).noneMatch(behaviour -> behaviour.equals(builder.getName()));
        }
        return true;
    }

    public boolean isBehaviorEnabled(final String name, final Mascot mascot) {
        if (behaviorBuilders.containsKey(name)) {
            return isBehaviorEnabled(getBehaviorBuilders().get(name), mascot);
        } else {
            return false;
        }
    }

    public boolean isBehaviorHidden(final String name) {
        if (behaviorBuilders.containsKey(name)) {
            return getBehaviorBuilders().get(name).isHidden();
        } else {
            return false;
        }
    }

    public boolean isBehaviorToggleable(final String name) {
        if (behaviorBuilders.containsKey(name)) {
            return getBehaviorBuilders().get(name).isToggleable();
        } else {
            return false;
        }
    }

    private Map<String, String> getConstants() {
        return constants;
    }

    Map<String, ActionBuilder> getActionBuilders() {
        return actionBuilders;
    }

    private Map<String, BehaviorBuilder> getBehaviorBuilders() {
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
