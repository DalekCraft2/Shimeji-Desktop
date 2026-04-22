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

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.ResourceBundle;

/**
 * @author Kilkakon
 * @since 1.0.14
 */
public class ScanMove extends BorderedAction {
    private static final Logger log = LoggerFactory.getLogger(ScanMove.class);

    private static final String PARAMETER_BEHAVIOR = "Behaviour";
    private static final String DEFAULT_BEHAVIOR = "";

    private static final String PARAMETER_TARGETBEHAVIOR = "TargetBehaviour";
    private static final String DEFAULT_TARGETBEHAVIOR = "";

    private static final String PARAMETER_TARGETLOOK = "TargetLook";
    private static final boolean DEFAULT_TARGETLOOK = false;

    private WeakReference<Mascot> target;

    private Boolean hasTurning = null;

    private boolean turning = false;

    public ScanMove(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        super.init(mascot);

        // cannot broadcast while scanning for an affordance
        getMascot().getAffordances().clear();

        if (getMascot().getManager() != null) {
            target = getMascot().getManager().getMascotWithAffordance(getAffordance());
        }
        Mascot targetMascot = target == null ? null : target.get();
        putVariable(getSchema().getString("TargetX"), targetMascot != null ? targetMascot.getAnchor().x : null);
        putVariable(getSchema().getString("TargetY"), targetMascot != null ? targetMascot.getAnchor().y : null);
    }

    @Override
    public boolean hasNext() throws VariableException {
        if (getMascot().getManager() == null) {
            return super.hasNext();
        }

        Mascot targetMascot = target == null ? null : target.get();
        return super.hasNext() && (turning || targetMascot != null && targetMascot.getAffordances().contains(getAffordance()));
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

        Mascot targetMascot = target == null ? null : target.get();
        int targetX = targetMascot.getAnchor().x;
        int targetY = targetMascot.getAnchor().y;

        putVariable(getSchema().getString("TargetX"), targetX);
        putVariable(getSchema().getString("TargetY"), targetY);

        if (getMascot().getAnchor().x != targetX) {
            // Activate turning animation if we change directions
            turning = hasTurningAnimation() && (turning || getMascot().getAnchor().x < targetX != getMascot().isLookRight());
            getMascot().setLookRight(getMascot().getAnchor().x < targetX);
        }
        boolean down = getMascot().getAnchor().y < targetY;

        // Check whether turning animation has finished
        Animation animation = getAnimation();
        if (turning && getTime() >= animation.getDuration()) {
            turning = false;
            animation = getAnimation();
        }

        animation.next(getMascot(), getTime());

        if (getMascot().isLookRight() && getMascot().getAnchor().x >= targetX ||
                !getMascot().isLookRight() && getMascot().getAnchor().x <= targetX) {
            getMascot().getAnchor().x = targetX;
        }
        if (down && getMascot().getAnchor().y >= targetY ||
                !down && getMascot().getAnchor().y <= targetY) {
            getMascot().getAnchor().y = targetY;
        }

        boolean noMoveX = getMascot().getAnchor().x == targetX;
        boolean noMoveY = getMascot().getAnchor().y == targetY;

        if (!turning && noMoveX && noMoveY) {
            boolean setFirstBehavior = false;
            try {
                getMascot().setBehavior(Main.getInstance().getConfiguration(getMascot().getImageSet()).buildBehavior(getBehavior(), getMascot()));
                setFirstBehavior = true;
                targetMascot.setBehavior(Main.getInstance().getConfiguration(targetMascot.getImageSet()).buildBehavior(getTargetBehavior(), targetMascot));
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
        return eval(getSchema().getString(PARAMETER_BEHAVIOR), String.class, DEFAULT_BEHAVIOR);
    }

    private String getTargetBehavior() throws VariableException {
        return eval(getSchema().getString(PARAMETER_TARGETBEHAVIOR), String.class, DEFAULT_TARGETBEHAVIOR);
    }

    private boolean isTargetLook() throws VariableException {
        return eval(getSchema().getString(PARAMETER_TARGETLOOK), Boolean.class, DEFAULT_TARGETLOOK);
    }
}
