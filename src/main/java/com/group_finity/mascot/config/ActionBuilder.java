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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class ActionBuilder implements IActionBuilder {

    private static final Logger log = Logger.getLogger(ActionBuilder.class.getName());
    private final String type;
    private final String name;
    private final String className;
    private final Map<String, String> params = new LinkedHashMap<>();
    private final List<AnimationBuilder> animationBuilders = new ArrayList<>();
    private final List<IActionBuilder> actionRefs = new ArrayList<>();
    private final ResourceBundle schema;

    public ActionBuilder(final Configuration configuration, final Entry actionNode, final String imageSet) throws IOException {
        schema = configuration.getSchema();
        name = actionNode.getAttribute(schema.getString("Name"));
        type = actionNode.getAttribute(schema.getString("Type"));
        className = actionNode.getAttribute(schema.getString("Class"));

        log.log(Level.INFO, "Loading action: {0}", this);

        getParams().putAll(actionNode.getAttributes());
        for (final Entry node : actionNode.selectChildren(schema.getString("Animation"))) {
            getAnimationBuilders().add(new AnimationBuilder(schema, node, imageSet));
        }

        for (final Entry node : actionNode.getChildren()) {
            if (node.getName().equals(schema.getString("ActionReference"))) {
                getActionRefs().add(new ActionRef(configuration, node));
            } else if (node.getName().equals(schema.getString("Action"))) {
                getActionRefs().add(new ActionBuilder(configuration, node, imageSet));
            }
        }

        log.log(Level.INFO, "Finished loading action: {0}", this);
    }

    @Override
    public String toString() {
        return "Action(" + getName() + "," + getType() + "," + getClassName() + ")";
    }

    @Override
    public Action buildAction(final Map<String, String> params) throws ActionInstantiationException {

        try {
            // Create Variable Map
            final VariableMap variables = createVariables(params);

            // Create Animations
            final List<Animation> animations = createAnimations();

            // Create Child Actions
            final List<Action> actions = createActions();

            if (type.equals(schema.getString("Embedded"))) {
                try {
                    @SuppressWarnings("unchecked") final Class<? extends Action> cls = (Class<? extends Action>) Class.forName(getClassName());
                    try {
                        try {
                            return cls.getConstructor(ResourceBundle.class, List.class, VariableMap.class).newInstance(schema, animations, variables);
                        } catch (IllegalAccessException | IllegalArgumentException | InstantiationException |
                                 NoSuchMethodException | SecurityException | InvocationTargetException e) {
                            // NOTE There seems to be no constructor, so move on to the next
                        }

                        return cls.getConstructor(ResourceBundle.class, VariableMap.class).newInstance(schema, variables);
                    } catch (IllegalAccessException | IllegalArgumentException | InstantiationException |
                             NoSuchMethodException | SecurityException | InvocationTargetException e) {
                        // NOTE There seems to be no constructor, so move on to the next
                    }

                    return cls.getConstructor().newInstance();
                } catch (final InstantiationException e) {
                    throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getString("FailedClassActionInitialiseErrorMessage") + "(" + this + ")", e);
                } catch (final IllegalAccessException e) {
                    throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getString("CannotAccessClassActionErrorMessage") + "(" + this + ")", e);
                } catch (final ClassNotFoundException e) {
                    throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getString("ClassNotFoundErrorMessage") + "(" + this + ")", e);
                } catch (NoSuchMethodException e) {
                    // TODO Get translations for the following error message
                    throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getString("ClassConstructorNotFoundErrorMessage") + "(" + this + ")", e);
                } catch (InvocationTargetException e) {
                    // TODO Think of a unique error message for this without wording it confusingly
                    throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getString("FailedClassActionInitialiseErrorMessage") + "(" + this + ")", e);
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
                throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getString("UnknownActionTypeErrorMessage") + "(" + this + ")");
            }

        } catch (final AnimationInstantiationException e) {
            throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getString("FailedCreateAnimationErrorMessage") + "(" + this + ")", e);
        } catch (final VariableException e) {
            throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getString("FailedParameterEvaluationErrorMessage") + "(" + this + ")", e);
        }
    }

    @Override
    public void validate() throws ConfigurationException {
        for (final IActionBuilder ref : getActionRefs()) {
            ref.validate();
        }
    }

    private List<Action> createActions() throws ActionInstantiationException {
        final List<Action> actions = new ArrayList<>();
        for (final IActionBuilder ref : getActionRefs()) {
            actions.add(ref.buildAction(new HashMap<>()));
        }
        return actions;
    }

    private List<Animation> createAnimations() throws AnimationInstantiationException {
        final List<Animation> animations = new ArrayList<>();
        for (final AnimationBuilder animationFactory : getAnimationBuilders()) {
            animations.add(animationFactory.buildAnimation());
        }
        return animations;
    }

    private VariableMap createVariables(final Map<String, String> params) throws VariableException {
        final VariableMap variables = new VariableMap();
        for (final Map.Entry<String, String> param : getParams().entrySet()) {
            variables.put(param.getKey(), Variable.parse(param.getValue()));
        }
        for (final Map.Entry<String, String> param : params.entrySet()) {
            variables.put(param.getKey(), Variable.parse(param.getValue()));
        }
        return variables;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    private String getClassName() {
        return className;
    }

    private Map<String, String> getParams() {
        return params;
    }

    private List<AnimationBuilder> getAnimationBuilders() {
        return animationBuilders;
    }

    private List<IActionBuilder> getActionRefs() {
        return actionRefs;
    }


}
