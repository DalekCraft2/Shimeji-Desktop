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
 * An implementation of {@link IActionBuilder} that stores all information about an action
 * so the action can be quickly built when needed.
 * <p>
 * {@code ActionBuilder} objects are used for both top-level actions and anonymous actions.
 * Top-level actions are named actions that are defined as an immediate child of an ActionList node
 * in a configuration file. Anonymous actions are unnamed actions that are defined as a child of another
 * top-level or anonymous action.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class ActionBuilder implements IActionBuilder {

    private static final Logger log = LoggerFactory.getLogger(ActionBuilder.class);

    // Action type values
    /**
     * The type of this action is specified in the Action node's Class attribute.
     */
    private static final int TYPE_EMBEDDED = 1;
    /**
     * The type of this action is {@link Move}.
     */
    private static final int TYPE_MOVE = 2;
    /**
     * The type of this action is {@link Stay}.
     */
    private static final int TYPE_STAY = 3;
    /**
     * The type of this action is {@link Animate}.
     */
    private static final int TYPE_ANIMATE = 4;
    /**
     * The type of this action is {@link Sequence}.
     */
    private static final int TYPE_SEQUENCE = 5;
    /**
     * The type of this action is {@link Select}.
     */
    private static final int TYPE_SELECT = 6;

    /**
     * Constant for an empty array of actions.
     * This is used to save memory by only allocating one empty array.
     */
    private static final Action[] EMPTY_ACTION_ARRAY = new Action[0];

    /**
     * The name of this {@code ActionBuilder}.
     * <p>
     * If this {@code ActionBuilder} is a top-level action, this value must be unique
     * among the top-level actions within this action's parent {@link Configuration}.
     * <p>
     * If this {@code ActionBuilder} is an anonymous action, this value is not used.
     *
     * @see #getName()
     */
    private final String name;

    /**
     * The type of this action, which determines the specific class that this action uses.
     * If this action's type is {@code Embedded}, this action will use the Action node's
     * Class attribute to determine what class to use.
     *
     * @see #TYPE_EMBEDDED
     * @see #TYPE_MOVE
     * @see #TYPE_STAY
     * @see #TYPE_ANIMATE
     * @see #TYPE_SEQUENCE
     * @see #TYPE_SELECT
     */
    private final int type;

    /**
     * The class used by this action.
     * This is only used if this action's {@link #type} is {@code Embedded}.
     */
    private final Class<? extends Action> cls;

    /**
     * The parameters to add to the context of this action.
     * These will be parsed into {@link Variable} objects when this action is built.
     */
    private final Map<String, String> params;

    /**
     * The animations that can be applied to a mascot when this action is executing.
     * If this action's type is an implementation of {@link ComplexAction}, these are ignored.
     */
    private final List<AnimationBuilder> animationBuilders;

    /**
     * The child actions of this action. This may contain anonymous actions and/or action references.
     * <p>
     * If this action's type is an implementation of {@link ComplexAction}, it must have child actions.
     * Otherwise, it must have no child actions.
     */
    private final List<IActionBuilder> childActionBuilders;

    /**
     * The schema used by this action.
     */
    private final ResourceBundle schema;

    /**
     * Creates a new {@code ActionBuilder} from the data contained within the specified Action node.
     *
     * @param configuration the parent {@link Configuration} object of this {@code ActionBuilder}
     * @param actionNode the Action node from which to load this action. This can be either a top-level Action node
     * or an anonymous Action node.
     * @param imageSet the name of the image set with which this action is associated
     * @throws ConfigurationException if an error occurs whilst reading the Action node, or if the Action node
     * contains invalid data
     */
    public ActionBuilder(final Configuration configuration, final Entry actionNode, final String imageSet) throws ConfigurationException {
        schema = configuration.getSchema();
        // TODO: Require that this be non-null for top-level actions, and don't bother setting it for anonymous actions
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
            throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                    "UnknownActionTypeErrorMessage"), typeString));
        }

        if (type == TYPE_EMBEDDED) {
            // Check the class here instead of when the action is built so the user is notified of the configuration errors sooner
            String className = actionNode.getAttribute(schema.getString("Class"));
            try {
                cls = Class.forName(className).asSubclass(Action.class);
                if (cls.isAnnotationPresent(Deprecated.class)) {
                    log.warn("Image set \"{}\" uses deprecated action class: {}", imageSet, cls.getName());
                }
            } catch (final ClassNotFoundException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                        "ClassNotFoundErrorMessage"), className), e);
            } catch (final ClassCastException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                        "ClassIsNotActionErrorMessage"), className), e);
            }
        } else {
            cls = null;
        }

        if (log.isDebugEnabled()) {
            log.debug("Loading action: {}", this);
        }

        // No need to check whether the attributes map is empty like in BehaviorBuilder,
        // because it's guaranteed to not be empty since we haven't removed any of the required attributes
        params = new LinkedHashMap<>(actionNode.getAttributes());

        // Verify that all parameters can be parsed
        for (final Map.Entry<String, String> param : params.entrySet()) {
            try {
                Variable.parse(param.getValue());
            } catch (final VariableException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                        "FailedParameterEvaluationErrorMessage"), param.getKey()), e);
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
                    throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString(
                            "FailedLoadAnimationErrorMessage"), e);
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
                        throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                                "ChildActionsNotSupportedErrorMessage"), typeString));
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
                    } catch (ConfigurationException | RuntimeException e) {
                        throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                                isReference ? "FailedLoadActionReferenceErrorMessage" : "FailedLoadActionErrorMessage"
                        ), node.getAttributes()), e);
                    }
                }
            }
            if (tempActionRefs == null) {
                childActionBuilders = List.of();
            } else {
                // Make list immutable
                childActionBuilders = List.copyOf(tempActionRefs);
            }
        } else {
            childActionBuilders = List.of();
        }

        // Ensure that ComplexAction-type actions have child actions
        if (isComplexAction && childActionBuilders.isEmpty()) {
            throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                    "NoChildActionsErrorMessage"), typeString));
        }

        if (log.isDebugEnabled()) {
            log.debug("Finished loading action: {}", this);
        }
    }

    @Override
    public String toString() {
        String typeString = switch (type) {
            case TYPE_EMBEDDED -> schema.getString("Embedded") + ",className=" + cls.getName();
            case TYPE_MOVE -> schema.getString("Move");
            case TYPE_STAY -> schema.getString("Stay");
            case TYPE_ANIMATE -> schema.getString("Animate");
            case TYPE_SEQUENCE -> schema.getString("Sequence");
            case TYPE_SELECT -> schema.getString("Select");
            default -> throw new IllegalStateException("Unexpected type: " + type);
        };
        return "Action[name=" + name + ",type=" + typeString + ']';
    }

    /**
     * Ensures the validity of any data loaded by this {@code ActionBuilder} object that
     * could not be validated when this {@code ActionBuilder} object was being initialized.
     * Specifically, this validates all of this action's child actions, and ensures that the
     * hotspots in this action's animations do not reference nonexistent behaviors.
     * <p>
     * This should be called after all data has been loaded into the parent
     * {@link Configuration} object of this {@code ActionBuilder}.
     *
     * @throws ConfigurationException if one of this action's child actions contains invalid data,
     * or one of its animations contains a hotspot that references a nonexistent behavior
     * @see Configuration#validate()
     * @see ActionRef#validate()
     * @see AnimationBuilder#validate()
     */
    @Override
    public void validate() throws ConfigurationException {
        // TODO: Ensure that action parameters like "TargetBehavior" reference existing behaviors
        for (final IActionBuilder ref : childActionBuilders) {
            try {
                ref.validate();
            } catch (ConfigurationException e) {
                throw new ConfigurationException(String.format(Main.getInstance().getLanguageBundle().getString(
                        "FailedValidateActionErrorMessage"), ref), e);
            }
        }
        for (final AnimationBuilder animationBuilder : animationBuilders) {
            try {
                animationBuilder.validate();
            } catch (ConfigurationException e) {
                throw new ConfigurationException(Main.getInstance().getLanguageBundle().getString(
                        "FailedValidateAnimationErrorMessage"), e);
            }
        }
    }

    /**
     * Builds this action, its animations, and all of its child actions, and adds the specified parameters
     * to its context. If a parameter name is present in both the specified parameters and this action's
     * existing parameters, the existing parameter will be overwritten by the specified parameter.
     *
     * @param params a map of parameter names and values to add to the context of the built action
     * @return the built action
     * @throws ActionInstantiationException if one of this action's parameters cannot be parsed into a {@link Variable},
     * one of this action's animations or child actions fails to be built, or this action's class cannot be instantiated
     */
    @Override
    public Action buildAction(final Map<String, String> params) throws ActionInstantiationException {
        // Create Variable Map
        final VariableMap variables = createVariables(params);

        // Create Animations
        final List<Animation> animations;
        try {
            animations = createAnimations();
        } catch (AnimationInstantiationException e) {
            throw new ActionInstantiationException(Main.getInstance().getLanguageBundle().getString(
                    "FailedCreateAnimationErrorMessage"), e);
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
                        // NOTE: There seems to be no constructor, so move on to the next
                    }

                    try {
                        return cls.getConstructor(ResourceBundle.class, VariableMap.class).newInstance(schema, variables);
                    } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
                             IllegalArgumentException | InstantiationException | InvocationTargetException e) {
                        // NOTE: There seems to be no constructor, so move on to the next
                    }

                    return cls.getConstructor().newInstance();
                } catch (final NoSuchMethodException e) {
                    throw new ActionInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString(
                            "ClassConstructorNotFoundErrorMessage"), cls.getName()), e);
                } catch (final IllegalAccessException e) {
                    throw new ActionInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString(
                            "CannotAccessClassActionErrorMessage"), cls.getName()), e);
                } catch (final InstantiationException e) {
                    throw new ActionInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString(
                            "FailedClassActionInitialiseErrorMessage"), cls.getName()), e);
                } catch (final InvocationTargetException e) {
                    // TODO: Think of a unique error message for this without wording it confusingly
                    throw new ActionInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString(
                            "FailedClassActionInitialiseErrorMessage"), cls.getName()), e);
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

    /**
     * Builds the child actions of this action.
     *
     * @return an array containing the built child actions, or an empty array if this action has no children
     * @throws ActionInstantiationException if one of the child actions fails to be built
     */
    private Action[] createActions() throws ActionInstantiationException {
        if (childActionBuilders.isEmpty()) {
            return EMPTY_ACTION_ARRAY;
        }

        final Action[] actions = new Action[childActionBuilders.size()];
        for (int i = 0; i < childActionBuilders.size(); i++) {
            actions[i] = childActionBuilders.get(i).buildAction(Map.of());
        }
        return actions;
    }

    /**
     * Builds the animations of this action.
     *
     * @return a list containing the built animations, or an empty array if this action has no animations
     * @throws AnimationInstantiationException if one of the animations fails to be built
     * @see AnimationBuilder#buildAnimation()
     */
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

    /**
     * Creates a {@link VariableMap} with this action's parameters, and adds the specified parameters to it.
     * If a parameter name is present in both the specified parameters and this action's
     * existing parameters, the existing parameter will be overwritten by the specified parameter.
     *
     * @param params a map of parameter names and values to add to the map
     * @return a map containing this action's parameters and the specified parameters
     * @throws ActionInstantiationException if one of this action's parameters or one of the specified parameters
     * cannot be parsed into a {@link Variable}
     */
    private VariableMap createVariables(final Map<String, String> params) throws ActionInstantiationException {
        final VariableMap variables = new VariableMap();
        for (final Map.Entry<String, String> param : this.params.entrySet()) {
            try {
                variables.put(param.getKey(), Variable.parse(param.getValue()));
            } catch (final VariableException e) {
                throw new ActionInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString(
                        "FailedParameterEvaluationErrorMessage"), param.getKey()), e);
            }
        }
        if (!params.isEmpty()) {
            for (final Map.Entry<String, String> param : params.entrySet()) {
                try {
                    variables.put(param.getKey(), Variable.parse(param.getValue()));
                } catch (final VariableException e) {
                    throw new ActionInstantiationException(String.format(Main.getInstance().getLanguageBundle().getString(
                            "FailedParameterEvaluationErrorMessage"), param.getKey()), e);
                }
            }
        }
        return variables;
    }

    /**
     * Gets the name of this {@code ActionBuilder}.
     * <p>
     * If this {@code ActionBuilder} is for a top-level action, then the caller must verify that the
     * returned value is unique among the top-level actions within this {@code ActionBuilder} object's
     * parent {@link Configuration}.
     * <p>
     * If this {@code ActionBuilder} is for an anonymous action, then this action's name is unused.
     *
     * @return the name of this {@code ActionBuilder}
     */
    public String getName() {
        return name;
    }
}
