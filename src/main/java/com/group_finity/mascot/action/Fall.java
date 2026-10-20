package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.script.VariableException;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Falling action.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Fall extends ActionBase {
    private static final Logger log = LoggerFactory.getLogger(Fall.class);

    private static final String PARAMETER_INITIALVX = "InitialVX";
    private static final int DEFAULT_INITIALVX = 0;

    private static final String PARAMETER_INITIALVY = "InitialVY";
    private static final int DEFAULT_INITIALVY = 0;

    private static final String PARAMETER_RESISTANCEX = "ResistanceX";
    private static final double DEFAULT_RESISTANCEX = 0.05;

    private static final String PARAMETER_RESISTANCEY = "ResistanceY";
    private static final double DEFAULT_RESISTANCEY = 0.1;

    private static final String PARAMETER_GRAVITY = "Gravity";
    private static final double DEFAULT_GRAVITY = 2;

    private static final String VARIABLE_VELOCITYX = "VelocityX";

    private static final String VARIABLE_VELOCITYY = "VelocityY";

    private double velocityX;

    private double velocityY;

    private double modX;

    private double modY;

    protected double scaling;

    public Fall(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        super.init(mascot);

        scaling = Main.getInstance().getSettings().scaling;

        /*
         * FIXME: Mascots start falling much more quickly (if scaled up) or slowly (if scaled down) than they should
         *  when released by the cursor, due to the scaling being used here. However, other things besides being thrown
         *  use the Fall action, so I am kind of in a dilemma here.
         *
         * This is a hotfix that does not use scaling if the initial velocities equal the dX or dY of the cursor.
         */
        /* if (getInitialVx() == getEnvironment().getCursor().getDx()) {
            velocityX = getInitialVx();
        } else {
            velocityX = getInitialVx() * scaling;
        }
        if (getInitialVy() == getEnvironment().getCursor().getDy()) {
            velocityY = getInitialVy();
        } else {
            velocityY = getInitialVy() * scaling;
        } */

        velocityX = getInitialVx() * scaling;
        velocityY = getInitialVy() * scaling;
    }

    @Override
    public boolean hasNext() throws VariableException {
        if (!super.hasNext()) {
            return false;
        }

        Point anchor = getMascot().getAnchor();
        /* Return true if the mascot is not on a floor or wall.
        However, ignore whether the mascot is on a floor if they have upward velocity; this allows mascots to start
        the Fall action on the ground with an upward initial velocity and not have the action be canceled immediately. */
        return (velocityY < 0 || !getEnvironment().getFloor().isOn(anchor)) && !getEnvironment().getWall().isOn(anchor);
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        final Mascot mascot = getMascot();
        if (velocityX != 0) {
            mascot.setLookRight(velocityX > 0);
        }

        // velocityX and velocityY were already scaled in init(), so only scale gravity here
        velocityX = velocityX - velocityX * getResistanceX();
        velocityY = velocityY - velocityY * getResistanceY() + getGravity() * scaling;

        putVariable(getSchema().getString(VARIABLE_VELOCITYX), velocityX);
        putVariable(getSchema().getString(VARIABLE_VELOCITYY), velocityY);

        modX += velocityX % 1;
        modY += velocityY % 1;

        // Movement amount
        final int dx = (int) Math.round(velocityX + modX);
        final int dy = (int) Math.round(velocityY + modY);

        modX %= 1;
        modY %= 1;

        // This represents the maximum distance covered on either axis. It is used to iterate over every intermediate
        // position between the mascot's current position and their new position, to check for floor/wall collisions.
        // It is always at least 1 to prevent division by 0.
        // Note that this does not use the distance formula; this is the greater value of dX and dY.
        int dev = Math.max(1, Math.max(Math.abs(dx), Math.abs(dy)));

        final Point anchor = mascot.getAnchor();
        final int initialX = anchor.x;
        final int initialY = anchor.y;
        final MascotEnvironment environment = getEnvironment();

        // Check collisions with floors/walls at every intermediate position between the current position (X, Y) and
        // the potential new position (X + dX, Y + dY)
        OUTER:
        for (int i = 0; i <= dev; i++) {
            int x = initialX + dx * i / dev;
            int y = initialY + dy * i / dev;

            // Move mascot
            if (dy > 0) {
                // HACK: The active window may have been moved since the last tick, in which case its floor may have
                // quickly moved upward past a mascot that is falling and would have otherwise landed on that floor.
                // To account for this, if the mascot is moving downward, check all positions from their current
                // Y-coordinate to 80 units above it to see whether any position is on a floor.
                for (int j = -80; j <= 0; j++) {
                    // We have to set the anchor instead of passing a temporary Point variable to isOn() because
                    // getFloor() - and other MascotEnvironment methods that return borders - use the mascot's anchor
                    // to determine which border to return.
                    anchor.setLocation(x, y + j);
                    // If the new X- and Y-coordinates put the anchor on a floor,
                    // then break out of the loop and use those coordinates as the new anchor coordinates
                    if (environment.getFloor(true).isOn(anchor)) {
                        break OUTER;
                    }
                }
            } else {
                // If the mascot is moving upward or has reached the peak of their trajectory,
                // simply set the anchor to the new X- and Y-coordinates without any further checks
                anchor.setLocation(x, y);
            }
            // If the anchor's new position - as set above - puts it on a wall,
            // then break out of the loop and use that as the mascot's new anchor
            if (environment.getWall(true).isOn(anchor)) {
                break;
            }

            // If no collisions are detected, then the mascot's anchor will be set to the aforementioned potential new
            // position (X + dX, Y + dY) by the time the loop ends.
        }

        // Animate
        getAnimation().apply(mascot, getTime());
    }

    private int getInitialVx() throws VariableException {
        return eval(getSchema().getString(PARAMETER_INITIALVX), Number.class, DEFAULT_INITIALVX).intValue();
    }

    private int getInitialVy() throws VariableException {
        return eval(getSchema().getString(PARAMETER_INITIALVY), Number.class, DEFAULT_INITIALVY).intValue();
    }

    private double getResistanceX() throws VariableException {
        return eval(getSchema().getString(PARAMETER_RESISTANCEX), Number.class, DEFAULT_RESISTANCEX).doubleValue();
    }

    private double getResistanceY() throws VariableException {
        return eval(getSchema().getString(PARAMETER_RESISTANCEY), Number.class, DEFAULT_RESISTANCEY).doubleValue();
    }

    private double getGravity() throws VariableException {
        return eval(getSchema().getString(PARAMETER_GRAVITY), Number.class, DEFAULT_GRAVITY).doubleValue();
    }
}
