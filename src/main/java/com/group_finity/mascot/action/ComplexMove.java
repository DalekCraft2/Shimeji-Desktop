package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Kilkakon
 * @since 1.0.21.3
 */
public class ComplexMove extends BorderedAction {
    private static final Logger log = Logger.getLogger(ComplexMove.class.getName());

    private final Breed.Delegate delegate = new Breed.Delegate(this);

    public static final String PARAMETER_CHARACTERISTICS = "Characteristics";

    private static final String DEFAULT_CHARACTERISTICS = "";

    public static final String PARAMETER_BEHAVIOUR = "Behaviour";

    private static final String DEFAULT_BEHAVIOUR = "";

    public static final String PARAMETER_TARGETBEHAVIOUR = "TargetBehaviour";

    private static final String DEFAULT_TARGETBEHAVIOUR = "";

    public static final String PARAMETER_TARGETLOOK = "TargetLook";

    private static final boolean DEFAULT_TARGETLOOK = false;

    public static final String PARAMETER_TARGETX = "TargetX";

    private static final int DEFAULT_TARGETX = Integer.MAX_VALUE;

    public static final String PARAMETER_TARGETY = "TargetY";

    private static final int DEFAULT_TARGETY = Integer.MAX_VALUE;

    private WeakReference<Mascot> target;

    private boolean turning = false;

    private Boolean hasTurning = null;

    private boolean breedEnabled = false;

    private boolean scanEnabled = false;

    public ComplexMove(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        super.init(mascot);

        for (String characteristic : getCharacteristics().split(",")) {
            if (characteristic.equals(getSchema().getString("Breed"))) {
                breedEnabled = true;
            }
            if (characteristic.equals(getSchema().getString("Scan"))) {
                scanEnabled = true;
            }
        }

        if (breedEnabled) {
            delegate.initScaling();
            delegate.validateBornCount();
            delegate.validateBornInterval();
        }
        if (scanEnabled) {
            // cannot broadcast while scanning for an affordance
            getMascot().getAffordances().clear();

            if (getMascot().getManager() != null) {
                target = getMascot().getManager().getMascotWithAffordance(getAffordance());
            }
            putVariable(getSchema().getString("TargetX"), target != null && target.get() != null ? target.get().getAnchor().x : null);
            putVariable(getSchema().getString("TargetY"), target != null && target.get() != null ? target.get().getAnchor().y : null);
        }
    }

    @Override
    public boolean hasNext() throws VariableException {
        if (scanEnabled) {
            if (getMascot().getManager() == null) {
                return super.hasNext();
            }

            return super.hasNext() && (turning || target != null && target.get() != null && target.get().getAffordances().contains(getAffordance()));
        } else {
            final int targetX = getTargetX();
            final int targetY = getTargetY();

            // Have we reached the target coordinates?
            boolean hasReachedTarget = targetX != Integer.MIN_VALUE && getMascot().getAnchor().x == targetX ||
                    targetY != Integer.MIN_VALUE && getMascot().getAnchor().y == targetY;

            return super.hasNext() && (!hasReachedTarget || turning);
        }
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        super.tick();

        if (scanEnabled) {
            // cannot broadcast while scanning for an affordance
            getMascot().getAffordances().clear();
        }

        if (getBorder() != null && !getBorder().isOn(getMascot().getAnchor())) {
            // The mascot is off the wall
            log.log(Level.INFO, "Lost Ground ({0},{1})", new Object[]{getMascot(), this});
            throw new LostGroundException();
        }

        int targetX = scanEnabled ? target.get().getAnchor().x : getTargetX();
        int targetY = scanEnabled ? target.get().getAnchor().y : getTargetY();

        if (scanEnabled) {
            putVariable(getSchema().getString("TargetX"), targetX);
            putVariable(getSchema().getString("TargetY"), targetY);
        }

        if (getMascot().getAnchor().x != targetX) {
            // Activate turning animation if we change directions
            turning = hasTurningAnimation() && (turning || getMascot().getAnchor().x < targetX != getMascot().isLookRight());
            getMascot().setLookRight(getMascot().getAnchor().x < targetX);
        }
        boolean down = getMascot().getAnchor().y < targetY;

        // Check whether turning animation has finished
        if (turning && getTime() >= getAnimation().getDuration()) {
            turning = false;
        }

        // Animate
        getAnimation().next(getMascot(), getTime());

        if (targetX != DEFAULT_TARGETX || scanEnabled) {
            // If we went past the target, set ourselves to the target position
            if (getMascot().isLookRight() && getMascot().getAnchor().x >= targetX ||
                    !getMascot().isLookRight() && getMascot().getAnchor().x <= targetX) {
                getMascot().setAnchor(new Point(targetX, getMascot().getAnchor().y));
            }
        }
        if (targetY != DEFAULT_TARGETY || scanEnabled) {
            // If we went past the target, set ourselves to the target position
            if (down && getMascot().getAnchor().y >= targetY ||
                    !down && getMascot().getAnchor().y <= targetY) {
                getMascot().setAnchor(new Point(getMascot().getAnchor().x, targetY));
            }
        }

        if (breedEnabled && delegate.isIntervalFrame() && !turning && delegate.isEnabled()) {
            // Multiply
            delegate.breed();
        }

        if (!turning && getMascot().getAnchor().x == targetX && getMascot().getAnchor().y == targetY) {
            boolean setFirstBehavior = false;
            try {
                getMascot().setBehavior(Main.getInstance().getConfiguration(getMascot().getImageSet()).buildBehavior(getBehavior(), getMascot()));
                setFirstBehavior = true;
                Mascot targetMascot = target.get();
                if (targetMascot != null) {
                    targetMascot.setBehavior(Main.getInstance().getConfiguration(targetMascot.getImageSet()).buildBehavior(getTargetBehavior(), targetMascot));
                    if (getTargetLook() && targetMascot.isLookRight() == getMascot().isLookRight()) {
                        targetMascot.setLookRight(!getMascot().isLookRight());
                    }
                }
            } catch (final BehaviorInstantiationException | CantBeAliveException e) {
                log.log(Level.SEVERE, "Failed to set behavior to \"" + (setFirstBehavior ? getTargetBehavior() : getBehavior()) + "\" for mascot \"" + (setFirstBehavior ? target.get() : getMascot()) + "\"", e);
                Main.showError(Main.getInstance().getLanguageBundle().getString("FailedSetBehaviourErrorMessage"), e);
            }
        }
    }

    @Override
    protected Animation getAnimation() throws VariableException {
        // had to expose both animations and variables for this
        // is there a better way?
        List<Animation> animations = getAnimations();
        for (Animation animation : animations) {
            if (animation.isEffective(getVariables()) &&
                    turning == animation.isTurn()) {
                return animation;
            }
        }

        return null;
    }

    protected boolean hasTurningAnimation() {
        if (hasTurning == null) {
            hasTurning = false;
            List<Animation> animations = getAnimations();
            if (animations.stream().anyMatch(Animation::isTurn)) {
                hasTurning = true;
            }
        }
        return hasTurning;
    }

    protected boolean isTurning() {
        return turning;
    }

    private String getCharacteristics() throws VariableException {
        return eval(getSchema().getString(PARAMETER_CHARACTERISTICS), String.class, DEFAULT_CHARACTERISTICS);
    }

    private String getBehavior() throws VariableException {
        return eval(getSchema().getString(PARAMETER_BEHAVIOUR), String.class, DEFAULT_BEHAVIOUR);
    }

    private String getTargetBehavior() throws VariableException {
        return eval(getSchema().getString(PARAMETER_TARGETBEHAVIOUR), String.class, DEFAULT_TARGETBEHAVIOUR);
    }

    private boolean getTargetLook() throws VariableException {
        return eval(getSchema().getString(PARAMETER_TARGETLOOK), Boolean.class, DEFAULT_TARGETLOOK);
    }

    private int getTargetX() throws VariableException {
        return eval(getSchema().getString(PARAMETER_TARGETX), Number.class, DEFAULT_TARGETX).intValue();
    }

    private int getTargetY() throws VariableException {
        return eval(getSchema().getString(PARAMETER_TARGETY), Number.class, DEFAULT_TARGETY).intValue();
    }
}
