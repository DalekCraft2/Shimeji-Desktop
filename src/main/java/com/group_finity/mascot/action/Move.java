package com.group_finity.mascot.action;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.script.VariableException;
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

    private static final String PARAMETER_TARGETX = "TargetX";
    private static final int DEFAULT_TARGETX = Integer.MAX_VALUE;

    private static final String PARAMETER_TARGETY = "TargetY";
    private static final int DEFAULT_TARGETY = Integer.MAX_VALUE;

    private Boolean hasTurning = null;

    protected boolean turning = false;

    public Move(final String name, final ResourceBundle schema, final List<Animation> animations,
                final VariableMap context) {
        super(name, schema, animations, context);
    }

    @Override
    public boolean hasNext() throws VariableException {
        if (!super.hasNext()) {
            return false;
        }

        if (turning) {
            return true;
        }

        final int targetX = getTargetX();
        final int targetY = getTargetY();

        final Point anchor = getMascot().getAnchor();

        /* Return true if the mascot has not reached the position (TargetX, TargetY).
        If TargetX has its default value, we only check the mascot's y-coordinate,
        and if TargetY has its default value, we only check the mascot's x-coordinate.
        If both TargetX and TargetY have their default values, return false. */
        return targetX != DEFAULT_TARGETX && anchor.x != targetX ||
                targetY != DEFAULT_TARGETY && anchor.y != targetY;
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        super.tick();

        Point anchor = getMascot().getAnchor();

        if (getBorder() != null && !getBorder().isOn(anchor)) {
            throw new LostGroundException("Mascot is not on border");
        }

        int targetX = getTargetX();
        int targetY = getTargetY();

        boolean down = false;

        if (targetX != DEFAULT_TARGETX) {
            if (anchor.x != targetX) {
                // Activate turning animation if we change directions
                turning = hasTurningAnimation() && (turning || anchor.x < targetX != getMascot().isLookRight());
                getMascot().setLookRight(anchor.x < targetX);
            }
        }
        if (targetY != DEFAULT_TARGETY) {
            down = anchor.y < targetY;
        }

        // Check whether turning animation has finished
        Animation animation = getAnimation();
        if (turning && getTime() >= animation.getDuration()) {
            turning = false;
            animation = getAnimation();
        }

        // Animate
        animation.apply(getMascot(), getTime());

        if (targetX != DEFAULT_TARGETX) {
            // If we went past the target, set ourselves to the target position
            if (getMascot().isLookRight() && anchor.x >= targetX
                    || !getMascot().isLookRight() && anchor.x <= targetX) {
                anchor.x = targetX;
            }
        }
        if (targetY != DEFAULT_TARGETY) {
            // If we went past the target, set ourselves to the target position
            if (down && anchor.y >= targetY ||
                    !down && anchor.y <= targetY) {
                anchor.y = targetY;
            }
        }
    }

    @Override
    protected Animation getAnimation() throws VariableException {
        // had to expose both animations and variables for this
        // is there a better way?
        List<Animation> animations = getAnimations();
        if (!animations.isEmpty()) {
            getVariableLock().readLock().lock();
            try {
                for (Animation animation : animations) {
                    if (turning == animation.isTurn() &&
                            animation.isEffective(getVariables())) {
                        return animation;
                    }
                }
            } finally {
                getVariableLock().readLock().unlock();
            }
        }

        return null;
    }

    protected boolean hasTurningAnimation() {
        if (hasTurning == null) {
            hasTurning = !getAnimations().isEmpty() && getAnimations().stream().anyMatch(Animation::isTurn);
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
