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
import java.util.stream.IntStream;

/**
 * @author Kilkakon
 * @since 1.0.14
 */
public class ScanMove extends BorderedAction {
    private static final Logger log = Logger.getLogger(ScanMove.class.getName());

    public static final String PARAMETER_BEHAVIOUR = "Behaviour";

    private static final String DEFAULT_BEHAVIOUR = "";

    public static final String PARAMETER_TARGETBEHAVIOUR = "TargetBehaviour";

    private static final String DEFAULT_TARGETBEHAVIOUR = "";

    public static final String PARAMETER_TARGETLOOK = "TargetLook";

    private static final boolean DEFAULT_TARGETLOOK = false;

    private WeakReference<Mascot> target;

    private boolean turning = false;

    private Boolean hasTurning = null;

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
        putVariable("target", target != null ? target.get() : null);
    }

    @Override
    public boolean hasNext() throws VariableException {
        if (getMascot().getManager() == null) {
            return super.hasNext();
        }

        return super.hasNext() && (turning || target != null && target.get() != null && target.get().getAffordances().contains(getAffordance()));
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

        int targetX = target.get().getAnchor().x;
        int targetY = target.get().getAnchor().y;

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

        getAnimation().next(getMascot(), getTime());

        if (getMascot().isLookRight() && getMascot().getAnchor().x >= targetX ||
                !getMascot().isLookRight() && getMascot().getAnchor().x <= targetX) {
            getMascot().setAnchor(new Point(targetX, getMascot().getAnchor().y));
        }
        if (down && getMascot().getAnchor().y >= targetY ||
                !down && getMascot().getAnchor().y <= targetY) {
            getMascot().setAnchor(new Point(getMascot().getAnchor().x, targetY));
        }

        boolean noMoveX = getMascot().getAnchor().x == targetX;
        boolean noMoveY = getMascot().getAnchor().y == targetY;

        if (!turning && noMoveX && noMoveY) {
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
                Main.showError(Main.getInstance().getLanguageBundle().getString("FailedSetBehaviourErrorMessage") + "\n" + e.getMessage() + "\n" + Main.getInstance().getLanguageBundle().getString("SeeLogForDetails"));
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
