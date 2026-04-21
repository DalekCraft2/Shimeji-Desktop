package com.group_finity.mascot.action;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Moving action.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Move extends BorderedAction {
    private static final Logger log = LoggerFactory.getLogger(Move.class);

    public static final String PARAMETER_TARGETX = "TargetX";

    private static final int DEFAULT_TARGETX = Integer.MAX_VALUE;

    public static final String PARAMETER_TARGETY = "TargetY";

    private static final int DEFAULT_TARGETY = Integer.MAX_VALUE;

    private Boolean hasTurning = null;

    protected boolean turning = false;

    public Move(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public boolean hasNext() throws VariableException {
        final int targetX = getTargetX();
        final int targetY = getTargetY();

        // Have we reached the target coordinates?
        boolean hasReachedTarget = targetX != Integer.MIN_VALUE && getMascot().getAnchor().x == targetX ||
                targetY != Integer.MIN_VALUE && getMascot().getAnchor().y == targetY;

        return super.hasNext() && (!hasReachedTarget || turning);
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        super.tick();

        if (getBorder() != null && !getBorder().isOn(getMascot().getAnchor())) {
            // The mascot is off the wall
            log.info("Lost ground ({}, {})", getMascot(), this);
            throw new LostGroundException();
        }

        int targetX = getTargetX();
        int targetY = getTargetY();

        boolean down = false;

        if (targetX != DEFAULT_TARGETX) {
            if (getMascot().getAnchor().x != targetX) {
                // Activate turning animation if we change directions
                turning = hasTurningAnimation() && (turning || getMascot().getAnchor().x < targetX != getMascot().isLookRight());
                getMascot().setLookRight(getMascot().getAnchor().x < targetX);
            }
        }
        if (targetY != DEFAULT_TARGETY) {
            down = getMascot().getAnchor().y < targetY;
        }

        // Check whether turning animation has finished
        Animation animation = getAnimation();
        if (turning && getTime() >= animation.getDuration()) {
            turning = false;
            animation = getAnimation();
        }

        // Animate
        animation.next(getMascot(), getTime());

        if (targetX != DEFAULT_TARGETX) {
            // If we went past the target, set ourselves to the target position
            if (getMascot().isLookRight() && getMascot().getAnchor().x >= targetX
                    || !getMascot().isLookRight() && getMascot().getAnchor().x <= targetX) {
                getMascot().getAnchor().x = targetX;
            }
        }
        if (targetY != DEFAULT_TARGETY) {
            // If we went past the target, set ourselves to the target position
            if (down && getMascot().getAnchor().y >= targetY ||
                    !down && getMascot().getAnchor().y <= targetY) {
                getMascot().getAnchor().y = targetY;
            }
        }
    }

    @Override
    protected Animation getAnimation() throws VariableException {
        // had to expose both animations and variables for this
        // is there a better way?
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

    private int getTargetX() throws VariableException {
        return eval(getSchema().getString(PARAMETER_TARGETX), Number.class, DEFAULT_TARGETX).intValue();
    }

    private int getTargetY() throws VariableException {
        return eval(getSchema().getString(PARAMETER_TARGETY), Number.class, DEFAULT_TARGETY).intValue();
    }
}
