package com.group_finity.mascot.config;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.action.*;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableException;
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

    private static final int TYPE_EMBEDDED = 1;
    private static final int TYPE_MOVE = 2;
    private static final int TYPE_STAY = 3;
    private static final int TYPE_ANIMATE = 4;
    private static final int TYPE_SEQUENCE = 5;
    private static final int TYPE_SELECT = 6;

    /**
     * Constant for an empty array of actions. Used to save memory.
     */
    private static final Action[] EMPTY_ACTION_ARRAY = new Action[0];

    private final String name;
    private final int type;
    private final String className;
    private final Class<? extends Action> cls;
    private final Map<String, String> params;
    private final List<AnimationBuilder> animationBuilders;
    private final List<IActionBuilder> actionRefs;
    private final ResourceBundle schema;

    public ActionBuilder(final Configuration configuration, final Entry actionNode, final String imageSet) throws ConfigurationException {
        schema = configuration.getSchema();
        /* The Name attribute is optional (more specifically, it's required for top-level actions
        and unused for child actions). However, because we want it to default to null if it's absent,
        we don't need to use hasAttribute() to manually assign a default value
        because getAttribute() will return null if it's absent anyway. */
        name = actionNode.getAttribute(schema.getString("Name"));

        String typeString = actionNode.getAttribute(schema.getString("Type"));
        if (typeString.equals(schema.getString("Embedded"))) {
            type = TYPE_EMBEDDED;
        } else if (typeString.equals(schema.getString("Move"))) {
            type = TYPE_MOVE;
        } else if (typeString.equals(schema.getString("Stay"))) {
            type = TYPE_STAY;
        } else if (typeString.equals(schema.getString("Animate"))) {
            type = TYPE_ANIMATE;
        } else if (typeString.equals(schema.getString("Sequence"))) {
            type = TYPE_SEQUENCE;
        } else if (typeString.equals(schema.getString("Select"))) {
            type = TYPE_SELECT;
        } else {
            throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("UnknownActionTypeErrorMessage"), typeString));
        }

        className = actionNode.getAttribute(schema.getString("Class"));

        if (log.isDebugEnabled()) {
            log.debug("Loading action: {}", this);
        }

        if (type == TYPE_EMBEDDED) {
            // Check the class here instead of when the action is built so the user is notified of the configuration errors sooner
            try {
                cls = Class.forName(className).asSubclass(Action.class);
                if (cls.isAnnotationPresent(Deprecated.class)) {
                    log.warn("Image set \"{}\" uses deprecated action class: {}", imageSet, cls.getName());
                }
            } catch (final ClassNotFoundException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("ClassNotFoundErrorMessage"), className), e);
            } catch (final ClassCastException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("ClassIsNotActionErrorMessage"), className), e);
            }
        } else {
            cls = null;
        }

        // No need to check whether the attributes map is empty like in BehaviorBuilder,
        // because it's guaranteed to not be empty since we haven't removed any of the required attributes
        params = new LinkedHashMap<>(actionNode.getAttributes());

        // Verify that all parameters can be parsed
        for (final Map.Entry<String, String> param : params.entrySet()) {
            try {
                Variable.parse(param.getValue());
            } catch (final VariableException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedParameterEvaluationErrorMessage"), param.getKey()), e);
            }
        }

        List<Entry> animationNodes = actionNode.selectChildren(schema.getString("Animation"));
        if (animationNodes.isEmpty()) {
            animationBuilders = List.of();
        } else {
            AnimationBuilder[] animationBuilderArray = new AnimationBuilder[animationNodes.size()];
            for (int i = 0; i < animationNodes.size(); i++) {
                Entry animationNode = animationNodes.get(i);
                try {
                    animationBuilderArray[i] = new AnimationBuilder(configuration, animationNode, imageSet);
                } catch (ConfigurationException e) {
                    throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString("FailedLoadAnimationErrorMessage"), e);
                }
            }
            animationBuilders = List.of(animationBuilderArray);
        }

        boolean isComplexAction = type == TYPE_SEQUENCE || type == TYPE_SELECT;

        /*
        To estimate the number of ActionReference and Action nodes combined, we calculate the number of remaining
        children (the total number of children minus the number of Animation nodes). We could alternatively
        call selectChildren() for both ActionReference and Action and then add the sizes of the returned lists
        to get a more accurate answer, but that might slow things down if this action has a lot of children.

        If the number of remaining children is greater than 0, there may be some Action and ActionReference nodes
        that we need to load.
         */
        int remainingChildren = actionNode.getChildren().size() - animationNodes.size();
        if (remainingChildren > 0) {
            List<IActionBuilder> tempActionRefs = null;
            for (final Entry node : actionNode.getChildren()) {
                boolean isReference = node.getName().equals(schema.getString("ActionReference"));
                if (isReference || node.getName().equals(schema.getString("Action"))) {
                    // Do not allow non-ComplexAction-type actions to have child actions
                    if (!isComplexAction) {
                        throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("ChildActionsNotSupportedErrorMessage"), type));
                    }
                    if (tempActionRefs == null) {
                        // Only initialize a new ArrayList if we need to; otherwise, use List.of() to save memory.
                        // Use the number of remaining children as the initial capacity so the list's internal
                        // array doesn't have to be resized.
                        tempActionRefs = new ArrayList<>(remainingChildren);
                    }
                    try {
                        tempActionRefs.add(isReference ?
                                new ActionRef(configuration, node) :
                                new ActionBuilder(configuration, node, imageSet));
                    } catch (ConfigurationException e) {
                        throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                                isReference ? "FailedLoadActionReferenceErrorMessage" : "FailedLoadActionErrorMessage"
                        ), node.getAttributes()), e);
                    }
                }
            }
            if (tempActionRefs == null) {
                actionRefs = List.of();
            } else {
                // Make list immutable
                actionRefs = List.copyOf(tempActionRefs);
            }
        } else {
            actionRefs = List.of();
        }

        // Ensure that ComplexAction-type actions have child actions
        if (isComplexAction && actionRefs.isEmpty()) {
            throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString("NoChildActionsErrorMessage"), type));
        }

        if (log.isDebugEnabled()) {
            log.debug("Finished loading action: {}", this);
        }
    }

    @Override
    public String toString() {
        String typeString = switch (type) {
            case TYPE_EMBEDDED -> schema.getString("Embedded");
            case TYPE_MOVE -> schema.getString("Move");
            case TYPE_STAY -> schema.getString("Stay");
            case TYPE_ANIMATE -> schema.getString("Animate");
            case TYPE_SEQUENCE -> schema.getString("Sequence");
            case TYPE_SELECT -> schema.getString("Select");
            default -> throw new IllegalStateException("Unexpected type: " + type);
        };
        return "Action[name=" + name + ",type=" + typeString + ",className=" + className + "]";
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
        for (final AnimationBuilder animationBuilder : animationBuilders) {
            try {
                animationBuilder.validate();
            } catch (ConfigurationException e) {
                throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString("FailedValidateAnimationErrorMessage"), e);
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
        final Action[] actions = createActions();

        switch (type) {
            case TYPE_EMBEDDED -> {
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
            }
            case TYPE_MOVE -> {
                return new Move(schema, animations, variables);
            }
            case TYPE_STAY -> {
                return new Stay(schema, animations, variables);
            }
            case TYPE_ANIMATE -> {
                return new Animate(schema, animations, variables);
            }
            case TYPE_SEQUENCE -> {
                return new Sequence(schema, variables, actions);
            }
            case TYPE_SELECT -> {
                return new Select(schema, variables, actions);
            }
            default -> // This should not be reached, because we verified that the type was valid in the constructor
                    throw new IllegalStateException("Unexpected type: " + type);
        }
    }

    private Action[] createActions() throws ActionInstantiationException {
        if (actionRefs.isEmpty()) {
            return EMPTY_ACTION_ARRAY;
        }

        final Action[] actions = new Action[actionRefs.size()];
        for (int i = 0; i < actionRefs.size(); i++) {
            actions[i] = actionRefs.get(i).buildAction(Map.of());
        }
        return actions;
    }

    private List<Animation> createAnimations() throws AnimationInstantiationException {
        if (animationBuilders.isEmpty()) {
            return List.of();
        }

        final Animation[] animations = new Animation[animationBuilders.size()];
        for (int i = 0; i < animationBuilders.size(); i++) {
            animations[i] = animationBuilders.get(i).buildAnimation();
        }
        return List.of(animations);
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
        if (!params.isEmpty()) {
            for (final Map.Entry<String, String> param : params.entrySet()) {
                try {
                    variables.put(param.getKey(), Variable.parse(param.getValue()));
                } catch (final VariableException e) {
                    throw new ActionInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString("FailedParameterEvaluationErrorMessage"), param.getKey()), e);
                }
            }
        }
        return variables;
    }

    public String getName() {
        return name;
    }
}
