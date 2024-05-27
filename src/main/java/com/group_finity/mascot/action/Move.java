package com.group_finity.mascot.action;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.awt.*;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

/**
 * Moving action.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class Move extends BorderedAction {
    private static final Logger log = Logger.getLogger(Move.class.getName());

    public static final String PARAMETER_TARGETX = "TargetX";

    private static final int DEFAULT_TARGETX = Integer.MAX_VALUE;

    public static final String PARAMETER_TARGETY = "TargetY";

    private static final int DEFAULT_TARGETY = Integer.MAX_VALUE;

    private boolean turning = false;

    private Boolean hasTurning = null;

    public Move(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public boolean hasNext() throws VariableException {
        final int targetX = getTargetX();
        final int targetY = getTargetY();

        boolean noMoveX = false;
        boolean noMoveY = false;

        if (targetX != Integer.MIN_VALUE) {
            // Do we need to move in the X direction?
            if (getMascot().getAnchor().x == targetX) {
                noMoveX = true;
            }
        }

        if (targetY != Integer.MIN_VALUE) {
            // Do we need to move in the Y direction?
            if (getMascot().getAnchor().y == targetY) {
                noMoveY = true;
            }
        }

        return super.hasNext() && (turning || !noMoveX && !noMoveY);
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        super.tick();

        if (getBorder() != null && !getBorder().isOn(getMascot().getAnchor())) {
            // The mascot is off the wall
            log.log(Level.INFO, "Lost ground ({0}, {1})", new Object[]{getMascot(), this});
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
        if (turning && getTime() >= getAnimation().getDuration()) {
            turning = false;
        }

        // Animate
        getAnimation().next(getMascot(), getTime());

        if (targetX != DEFAULT_TARGETX) {
            // If we went past the target, set ourselves to the target position
            if (getMascot().isLookRight() && getMascot().getAnchor().x >= targetX
                    || !getMascot().isLookRight() && getMascot().getAnchor().x <= targetX) {
                getMascot().setAnchor(new Point(targetX, getMascot().getAnchor().y));
            }
        }
        if (targetY != DEFAULT_TARGETY) {
            // If we went past the target, set ourselves to the target position
            if (down && getMascot().getAnchor().y >= targetY ||
                    !down && getMascot().getAnchor().y <= targetY) {
                getMascot().setAnchor(new Point(getMascot().getAnchor().x, targetY));
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
            if (IntStream.range(0, animations.size()).anyMatch(index -> animations.get(index).isTurn())) {
                hasTurning = true;
            }
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
