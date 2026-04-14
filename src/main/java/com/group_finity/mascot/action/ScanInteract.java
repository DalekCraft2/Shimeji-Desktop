package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author Kilkakon
 * @since 1.0.21
 */
public class ScanInteract extends BorderedAction {
    private static final Logger log = LoggerFactory.getLogger(ScanInteract.class);

    public static final String PARAMETER_BEHAVIOUR = "Behaviour";

    private static final String DEFAULT_BEHAVIOUR = "";

    public static final String PARAMETER_TARGETBEHAVIOUR = "TargetBehaviour";

    private static final String DEFAULT_TARGETBEHAVIOUR = "";

    public static final String PARAMETER_TARGETLOOK = "TargetLook";

    private static final boolean DEFAULT_TARGETLOOK = false;

    private WeakReference<Mascot> target;

    private Boolean hasTurning = null;

    private boolean turning = false;

    public ScanInteract(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        super.init(mascot);

        // cannot broadcast while scanning for an affordance
        getMascot().getAffordances().clear();

        putVariable(getSchema().getString("TargetX"), null);
        putVariable(getSchema().getString("TargetY"), null);
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
            log.info("Lost ground ({}, {})", getMascot(), this);
            throw new LostGroundException();
        }

        // refresh target
        Mascot targetMascot = target == null ? null : target.get();
        if (getMascot().getManager() != null && (targetMascot == null || !targetMascot.getAffordances().contains(getAffordance()))) {
            target = getMascot().getManager().getMascotWithAffordance(getAffordance());
            targetMascot = target == null ? null : target.get();
        }
        putVariable(getSchema().getString("TargetX"), targetMascot != null ? targetMascot.getAnchor().x : null);
        putVariable(getSchema().getString("TargetY"), targetMascot != null ? targetMascot.getAnchor().y : null);

        if (targetMascot != null && targetMascot.getAffordances().contains(getAffordance())) {
            if (getMascot().getAnchor().x != targetMascot.getAnchor().x) {
                // Activate turning animation if we change directions
                turning = hasTurningAnimation() && (turning || getMascot().getAnchor().x < targetMascot.getAnchor().x != getMascot().isLookRight());
                getMascot().setLookRight(getMascot().getAnchor().x < targetMascot.getAnchor().x);
            }

            // Check whether turning animation has finished
            Animation animation = getAnimation();
            if (turning && getTime() >= animation.getDuration()) {
                setTime(getTime() - animation.getDuration());
                turning = false;
                animation = getAnimation();
            }

            animation.next(getMascot(), getTime());
            animation = getAnimation();

            if (!turning && (getTime() == animation.getDuration() - 1 || animation.getDuration() == 1) && !getBehavior().trim().isEmpty()) {
                boolean setFirstBehavior = false;
                try {
                    getMascot().setBehavior(Main.getInstance().getConfiguration(getMascot().getImageSet()).buildBehavior(getBehavior(), getMascot()));
                    setFirstBehavior = true;
                    if (!getTargetBehavior().trim().isEmpty()) {
                        targetMascot.setBehavior(Main.getInstance().getConfiguration(targetMascot.getImageSet()).buildBehavior(getTargetBehavior(), targetMascot));
                    }
                    if (isTargetLook() && targetMascot.isLookRight() == getMascot().isLookRight()) {
                        targetMascot.setLookRight(!getMascot().isLookRight());
                    }
                } catch (final BehaviorInstantiationException | CantBeAliveException e) {
                    String behaviorParam = setFirstBehavior ? getTargetBehavior() : getBehavior();
                    Mascot mascotParam = setFirstBehavior ? targetMascot : getMascot();
                    log.error("Failed to set behavior to \"{}\" for mascot \"{}\"", behaviorParam, mascotParam, e);
                    Main.showError(String.format(Main.getInstance().getLanguageBundle().getString("FailedSetBehaviourErrorMessage"), behaviorParam, mascotParam), e);
                }
            }
        }
    }

    @Override
    protected Animation getAnimation() throws VariableException {
        List<Animation> animations = getAnimations();
        for (Animation animation : animations) {
            if (turning == animation.isTurn() &&
                    animation.isEffective(getVariables())) {
                return animation;
            }
        }

        return null;
    }

    protected boolean hasTurningAnimation() {
        if (hasTurning == null) {
            hasTurning = getAnimations().stream().anyMatch(Animation::isTurn);
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

    private boolean isTargetLook() throws VariableException {
        return eval(getSchema().getString(PARAMETER_TARGETLOOK), Boolean.class, DEFAULT_TARGETLOOK);
    }
}
