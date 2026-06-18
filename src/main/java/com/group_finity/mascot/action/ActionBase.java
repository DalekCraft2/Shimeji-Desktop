package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableException;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Abstract class that implements common functionality of actions.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public abstract class ActionBase implements Action {
    private static final Logger log = LoggerFactory.getLogger(ActionBase.class);

    private static final String PARAMETER_DURATION = "Duration";
    private static final int DEFAULT_DURATION = Integer.MAX_VALUE;

    private static final String PARAMETER_CONDITION = "Condition";
    private static final boolean DEFAULT_CONDITION = true;

    private static final String PARAMETER_DRAGGABLE = "Draggable";
    private static final boolean DEFAULT_DRAGGABLE = true;

    private static final String PARAMETER_AFFORDANCE = "Affordance";
    private static final String DEFAULT_AFFORDANCE = "";

    private final ResourceBundle schema;

    private final List<Animation> animations;

    private final VariableMap variables;

    private final ReadWriteLock variableLock = new ReentrantReadWriteLock();

    private Mascot mascot;

    private int startTime;

    public ActionBase(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        this.schema = schema;
        this.animations = animations;
        variables = context;
    }

    @Override
    public String toString() {
        try {
            return "Action[className=" + getClass().getSimpleName() + ",name=" + getName() + ']';
        } catch (final VariableException e) {
            return "Action[className=" + getClass().getSimpleName() + ",name=" + null + ']';
        }
    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        if (mascot == null) {
            return;
        }

        setMascot(mascot);
        setTime(0);

        // Add mascot and action to the variable map, so they can be used in the script
        putVariable("mascot", mascot);
        putVariable("action", this);

        // Initialize variable values
        variableLock.writeLock().lock();
        try {
            getVariables().init();
        } finally {
            variableLock.writeLock().unlock();
        }

        // Initialize the animations
        if (!animations.isEmpty()) {
            for (final Animation animation : animations) {
                animation.init();
            }
        }
    }

    @Override
    public boolean hasNext() throws VariableException {
        return getMascot() != null && getTime() < getDuration() && isEffective();
    }

    @Override
    public void next() throws LostGroundException, VariableException {
        if (getMascot() == null) {
            return;
        }

        resetVariables();

        // Clear affordances
        if (!getMascot().getAffordances().isEmpty()) {
            getMascot().getAffordances().clear();
        }
        if (!getAffordance().trim().isEmpty()) {
            getMascot().getAffordances().add(getAffordance());
        }

        // Refresh hotspots
        refreshHotspots();

        tick();
    }

    private void resetVariables() {
        // Clear cached variable values (each frame)
        variableLock.writeLock().lock();
        try {
            getVariables().resetValues();
        } finally {
            variableLock.writeLock().unlock();
        }

        // Clear cached animation condition values (each frame)
        if (!getAnimations().isEmpty()) {
            for (final Animation animation : getAnimations()) {
                animation.resetCondition();
            }
        }
    }

    protected abstract void tick() throws LostGroundException, VariableException;

    protected void refreshHotspots() {
        ReadWriteLock lock = getMascot().getHotspotLock();
        lock.writeLock().lock();
        try {
            Animation animation = getAnimation();
            if (animation != null) {
                // This clears and sets the mascot's hotspots
                getMascot().setHotspots(animation.getHotspots());
            }
        } catch (VariableException e) {
            // Clear the hotspots if we failed to get the animation
            getMascot().clearHotspots();
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected ResourceBundle getSchema() {
        return schema;
    }

    protected List<Animation> getAnimations() {
        return animations;
    }

    protected Animation getAnimation() throws VariableException {
        if (!getAnimations().isEmpty()) {
            variableLock.readLock().lock();
            try {
                for (final Animation animation : getAnimations()) {
                    if (animation.isEffective(getVariables())) {
                        return animation;
                    }
                }
            } finally {
                variableLock.readLock().unlock();
            }
        }

        return null;
    }

    protected VariableMap getVariables() {
        return variables;
    }

    protected void putVariable(final String key, final Object value) {
        variableLock.writeLock().lock();
        try {
            getVariables().put(key, value);
        } finally {
            variableLock.writeLock().unlock();
        }
    }

    protected ReadWriteLock getVariableLock() {
        return variableLock;
    }

    protected Mascot getMascot() {
        return mascot;
    }

    private void setMascot(final Mascot mascot) {
        this.mascot = mascot;
    }

    protected MascotEnvironment getEnvironment() {
        return getMascot().getEnvironment();
    }

    protected int getTime() {
        return getMascot().getTime() - startTime;
    }

    protected void setTime(final int time) {
        startTime = getMascot().getTime() - time;
    }

    private String getName() throws VariableException {
        return eval(schema.getString("Name"), String.class, null);
    }

    private int getDuration() throws VariableException {
        return eval(schema.getString(PARAMETER_DURATION), Number.class, DEFAULT_DURATION).intValue();
    }

    private boolean isEffective() throws VariableException {
        return eval(schema.getString(PARAMETER_CONDITION), Boolean.class, DEFAULT_CONDITION);
    }

    public boolean isDraggable() throws VariableException {
        return eval(schema.getString(PARAMETER_DRAGGABLE), Boolean.class, DEFAULT_DRAGGABLE);
    }

    protected String getAffordance() throws VariableException {
        return eval(schema.getString(PARAMETER_AFFORDANCE), String.class, DEFAULT_AFFORDANCE);
    }

    protected <T> T eval(final String name, final Class<T> type, final T defaultValue) throws VariableException {
        variableLock.readLock().lock();
        try {
            // Get the raw Variable object so we can throw a VariableException if it fails to evaluate
            final Variable variable = getVariables().getRawMap().get(name);
            if (variable != null) {
                return type.cast(variable.get(getVariables()));
            }
        } finally {
            variableLock.readLock().unlock();
        }

        return defaultValue;
    }
}
