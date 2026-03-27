package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.action.*;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.ActionInstantiationException;
import com.group_finity.mascot.exception.AnimationInstantiationException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * An object that build actions.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class ActionBuilder implements IActionBuilder {

    private static final Logger log = LoggerFactory.getLogger(ActionBuilder.class);

    /**
     * List of valid values for the "Type" attribute. "Embedded" is excluded because that is checked separately.
     */
    private static final List<String> VALID_TYPES = List.of("Move", "Stay", "Animate", "Sequence", "Select");

    private final String type;
    private final String name;
    private final String className;
    private Class<? extends Action> cls;
    private final Map<String, String> params = new LinkedHashMap<>();
    private final List<AnimationBuilder> animationBuilders = new ArrayList<>();
    private final List<IActionBuilder> actionRefs = new ArrayList<>();
    private final ResourceBundle schema;

    public ActionBuilder(final Configuration configuration, final Entry actionNode, final String imageSet) throws ConfigurationException {
        schema = configuration.getSchema();
        name = actionNode.getAttribute(schema.getString("Name"));
        type = actionNode.getAttribute(schema.getString("Type"));
        className = actionNode.getAttribute(schema.getString("Class"));

        log.debug("Loading action: {}", this);

        params.putAll(actionNode.getAttributes());
        List<Entry> selectChildren = actionNode.selectChildren(schema.getString("Animation"));
        for (Entry node : selectChildren) {
            try {
                animationBuilders.add(new AnimationBuilder(schema, node, imageSet));
            } catch (ConfigurationException e) {
                throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString("FailedLoadAnimationErrorMessage"), e);
            }
        }

        for (final Entry node : actionNode.getChildren()) {
            if (node.getName().equals(schema.getString("ActionReference"))) {
                try {
                    actionRefs.add(new ActionRef(configuration, node));
                } catch (ConfigurationException e) {
                    throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedLoadActionReferenceErrorMessage"), node.getAttributes()), e);
                }
            } else if (node.getName().equals(schema.getString("Action"))) {
                try {
                    actionRefs.add(new ActionBuilder(configuration, node, imageSet));
                } catch (ConfigurationException e) {
                    throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedLoadActionErrorMessage"), node.getAttributes()), e);
                }
            }
        }

        if (type.equals(schema.getString("Embedded"))) {
            // Check the class here instead of when the action is built so the user is notified of the configuration errors sooner
            try {
                cls = Class.forName(className).asSubclass(Action.class);
            } catch (final ClassNotFoundException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("ClassNotFoundErrorMessage"), className), e);
            } catch (final ClassCastException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("ClassIsNotActionErrorMessage"), className), e);
            }
        } else if (VALID_TYPES.stream().noneMatch(type -> this.type.equals(schema.getString(type)))) {
            throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("UnknownActionTypeErrorMessage"), type));
        }

        // Verify that all parameters can be parsed
        for (final Map.Entry<String, String> param : params.entrySet()) {
            try {
                Variable.parse(param.getValue());
            } catch (final VariableException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedParameterEvaluationErrorMessage"), param.getKey()), e);
            }
        }

        log.debug("Finished loading action: {}", this);
    }

    @Override
    public String toString() {
        return "Action[name=" + name + ",type=" + type + ",className=" + className + "]";
    }

    @Override
    public void validate() throws ConfigurationException {
        for (final IActionBuilder ref : actionRefs) {
            try {
                ref.validate();
            } catch (ConfigurationException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedValidateActionErrorMessage"), ref), e);
            }
        }
    }

    @Override
    public Action buildAction(final Map<String, String> params) throws ActionInstantiationException {
        // Create Variable Map
        final VariableMap variables = createVariables(params);

        // Create Animations
        final List<Animation> animations;
        try {
            animations = createAnimations();
        } catch (AnimationInstantiationException e) {
            throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getString("FailedCreateAnimationErrorMessage"), e);
        }

        // Create Child Actions
        final List<Action> actions = createActions();

        if (type.equals(schema.getString("Embedded"))) {
            try {
                try {
                    return cls.getConstructor(ResourceBundle.class, List.class, VariableMap.class).newInstance(schema, animations, variables);
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
                         IllegalArgumentException | InstantiationException | InvocationTargetException e) {
                    // NOTE There seems to be no constructor, so move on to the next
                }

                try {
                    return cls.getConstructor(ResourceBundle.class, VariableMap.class).newInstance(schema, variables);
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
                         IllegalArgumentException | InstantiationException | InvocationTargetException e) {
                    // NOTE There seems to be no constructor, so move on to the next
                }

                return cls.getConstructor().newInstance();
            } catch (final NoSuchMethodException e) {
                throw new ActionInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString("ClassConstructorNotFoundErrorMessage"), className), e);
            } catch (final IllegalAccessException e) {
                throw new ActionInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString("CannotAccessClassActionErrorMessage"), className), e);
            } catch (final InstantiationException e) {
                throw new ActionInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedClassActionInitialiseErrorMessage"), className), e);
            } catch (final InvocationTargetException e) {
                // TODO: Think of a unique error message for this without wording it confusingly
                throw new ActionInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedClassActionInitialiseErrorMessage"), className), e);
            }

        } else if (type.equals(schema.getString("Move"))) {
            return new Move(schema, animations, variables);
        } else if (type.equals(schema.getString("Stay"))) {
            return new Stay(schema, animations, variables);
        } else if (type.equals(schema.getString("Animate"))) {
            return new Animate(schema, animations, variables);
        } else if (type.equals(schema.getString("Sequence"))) {
            return new Sequence(schema, variables, actions.toArray(new Action[0]));
        } else if (type.equals(schema.getString("Select"))) {
            return new Select(schema, variables, actions.toArray(new Action[0]));
        } else {
            throw new ActionInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString("UnknownActionTypeErrorMessage"), type));
        }
    }

    private List<Action> createActions() throws ActionInstantiationException {
        final List<Action> actions = new ArrayList<>();
        for (final IActionBuilder ref : actionRefs) {
            actions.add(ref.buildAction(new HashMap<>()));
        }
        return actions;
    }

    private List<Animation> createAnimations() throws AnimationInstantiationException {
        final List<Animation> animations = new ArrayList<>();
        for (final AnimationBuilder animationFactory : animationBuilders) {
            animations.add(animationFactory.buildAnimation());
        }
        return animations;
    }

    private VariableMap createVariables(final Map<String, String> params) throws ActionInstantiationException {
        final VariableMap variables = new VariableMap();
        for (final Map.Entry<String, String> param : this.params.entrySet()) {
            try {
                variables.put(param.getKey(), Variable.parse(param.getValue()));
            } catch (final VariableException e) {
                throw new ActionInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedParameterEvaluationErrorMessage"), param.getKey()), e);
            }
        }
        for (final Map.Entry<String, String> param : params.entrySet()) {
            try {
                variables.put(param.getKey(), Variable.parse(param.getValue()));
            } catch (final VariableException e) {
                throw new ActionInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedParameterEvaluationErrorMessage"), param.getKey()), e);
            }
        }
        return variables;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
