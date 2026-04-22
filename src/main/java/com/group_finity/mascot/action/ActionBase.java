package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.Variable;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ResourceBundle;

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
            return "Action[className=" + getClass().getSimpleName() + ",name=" + getName() + "]";
        } catch (final VariableException e) {
            return "Action[className=" + getClass().getSimpleName() + ",name=" + null + "]";
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
        getVariables().put("mascot", mascot);
        getVariables().put("action", this);

        // Initialize variable values
        getVariables().init();

        // Initialize the animations
        for (final Animation animation : animations) {
            animation.init();
        }
    }

    @Override
    public boolean hasNext() throws VariableException {
        if (getMascot() == null) {
            return false;
        }

        final boolean effective = isEffective();
        final boolean inTime = getTime() < getDuration();

        return effective && inTime;
    }

    @Override
    public void next() throws LostGroundException, VariableException {
        if (getMascot() == null) {
            return;
        }

        initFrame();

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

    private void initFrame() {
        // Initialize variable values (each frame)
        getVariables().initFrame();

        // Initialize animation frames
        for (final Animation animation : getAnimations()) {
            animation.initFrame();
        }
    }

    protected abstract void tick() throws LostGroundException, VariableException;

    protected void refreshHotspots() {
        synchronized (getMascot().getHotspotLock()) {
            try {
                Animation animation = getAnimation();
                if (animation != null) {
                    // This clears and sets the mascot's hotspots
                    getMascot().setHotspots(animation.getHotspots());
                }
            } catch (VariableException e) {
                // Clear the hotspots if we failed to get the animation
                getMascot().clearHotspots();
            }
        }
    }

    protected ResourceBundle getSchema() {
        return schema;
    }

    protected List<Animation> getAnimations() {
        return animations;
    }

    protected Animation getAnimation() throws VariableException {
        for (final Animation animation : getAnimations()) {
            if (animation.isEffective(getVariables())) {
                return animation;
            }
        }

        return null;
    }

    protected VariableMap getVariables() {
        return variables;
    }

    protected void putVariable(final String key, final Object value) {
        synchronized (getVariables()) {
            getVariables().put(key, value);
        }
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
        synchronized (getVariables()) {
            final Variable variable = getVariables().getRawMap().get(name);
            if (variable != null) {
                return type.cast(variable.get(getVariables()));
            }
        }

        return defaultValue;
    }
}
