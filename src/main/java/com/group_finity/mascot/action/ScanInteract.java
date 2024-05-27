package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

/**
 * @author Kilkakon
 * @since 1.0.21
 */
public class ScanInteract extends BorderedAction {
    private static final Logger log = Logger.getLogger(ScanInteract.class.getName());

    public static final String PARAMETER_BEHAVIOUR = "Behaviour";

    private static final String DEFAULT_BEHAVIOUR = "";

    public static final String PARAMETER_TARGETBEHAVIOUR = "TargetBehaviour";

    private static final String DEFAULT_TARGETBEHAVIOUR = "";

    public static final String PARAMETER_TARGETLOOK = "TargetLook";

    private static final boolean DEFAULT_TARGETLOOK = false;

    private WeakReference<Mascot> target;

    private boolean turning = false;

    private Boolean hasTurning = null;

    public ScanInteract(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        super.init(mascot);

        // cannot broadcast while scanning for an affordance
        getMascot().getAffordances().clear();

        putVariable("target", null);
    }

    @Override
    public boolean hasNext() throws VariableException {
        final boolean inTime = getTime() < getAnimation().getDuration();

        return super.hasNext() && (turning || inTime);
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        super.tick();

        // cannot broadcast while scanning for an affordance
        getMascot().getAffordances().clear();

        if (getBorder() != null && !getBorder().isOn(getMascot().getAnchor())) {
            log.log(Level.INFO, "Lost ground ({0}, {1})", new Object[]{getMascot(), this});
            throw new LostGroundException();
        }

        // refresh target
        if (getMascot().getManager() != null && (target == null || target.get() == null || !target.get().getAffordances().contains(getAffordance()))) {
            target = getMascot().getManager().getMascotWithAffordance(getAffordance());
        }
        putVariable("target", target != null ? target.get() : null);

        if (target != null && target.get() != null && target.get().getAffordances().contains(getAffordance())) {
            if (getMascot().getAnchor().x != target.get().getAnchor().x) {
                // Activate turning animation if we change directions
                turning = hasTurningAnimation() && (turning || getMascot().getAnchor().x < target.get().getAnchor().x != getMascot().isLookRight());
                getMascot().setLookRight(getMascot().getAnchor().x < target.get().getAnchor().x);
            }

            // Check whether turning animation has finished
            if (turning && getTime() >= getAnimation().getDuration()) {
                setTime(getTime() - getAnimation().getDuration());
                turning = false;
            }

            getAnimation().next(getMascot(), getTime());

            if (!turning && getTime() == getAnimation().getDuration() - 1 && !getBehavior().trim().isEmpty()) {
                boolean setFirstBehavior = false;
                try {
                    getMascot().setBehavior(Main.getInstance().getConfiguration(getMascot().getImageSet()).buildBehavior(getBehavior(), getMascot()));
                    setFirstBehavior = true;
                    Mascot targetMascot = target.get();
                    if (targetMascot != null) {
                        if (!getTargetBehavior().trim().isEmpty()) {
                            targetMascot.setBehavior(Main.getInstance().getConfiguration(targetMascot.getImageSet()).buildBehavior(getTargetBehavior(), targetMascot));
                        }
                        if (getTargetLook() && targetMascot.isLookRight() == getMascot().isLookRight()) {
                            targetMascot.setLookRight(!getMascot().isLookRight());
                        }
                    }
                } catch (final BehaviorInstantiationException | CantBeAliveException e) {
                    log.log(Level.SEVERE, "Failed to set behavior to \"" + (setFirstBehavior ? getTargetBehavior() : getBehavior()) + "\" for mascot \"" + (setFirstBehavior ? target.get() : getMascot()) + "\"", e);
                    Main.showError(Main.getInstance().getLanguageBundle().getString("FailedSetBehaviourErrorMessage") + "\n" + e.getMessage() + "\n" + Main.getInstance().getLanguageBundle().getString("SeeLogForDetails"));
                }
            }
        }
    }

    @Override
    protected Animation getAnimation() throws VariableException {
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
            if (IntStream.range(0, animations.size()).anyMatch(index -> animations.get(index).isTurn())) {
                hasTurning = true;
            }
        }
        return hasTurning;
    }

    protected boolean isTurning() {
        return turning;
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
}
