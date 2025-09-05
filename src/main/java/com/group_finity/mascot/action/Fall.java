package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.awt.*;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Falling action.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Fall extends ActionBase {

    private static final Logger log = Logger.getLogger(Fall.class.getName());

    public static final String PARAMETER_INITIALVX = "InitialVX";

    private static final int DEFAULT_INITIALVX = 0;

    public static final String PARAMETER_INITIALVY = "InitialVY";

    private static final int DEFAULT_INITIALVY = 0;

    public static final String PARAMETER_RESISTANCEX = "ResistanceX";

    private static final double DEFAULT_RESISTANCEX = 0.05;

    public static final String PARAMETER_RESISTANCEY = "ResistanceY";

    private static final double DEFAULT_RESISTANCEY = 0.1;

    public static final String PARAMETER_GRAVITY = "Gravity";

    private static final double DEFAULT_GRAVITY = 2;

    public static final String VARIABLE_VELOCITYX = "VelocityX";

    public static final String VARIABLE_VELOCITYY = "VelocityY";

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

        scaling = Double.parseDouble(Main.getInstance().getProperties().getProperty("Scaling", "1.0"));

        // TODO Deal with the below issue.
        // Shimejis start falling much more quickly than they should when released by the cursor, due to the scaling being used here.
        // However, other things besides being thrown use the Fall action, so I am kind of in a dilemma here.
        // This is a hotfix which does not use scaling if the initial velocities equal the dX or dY of the cursor.
        /* if (getInitialVx() == getEnvironment().getCursor().getDx()) {
            setVelocityX(getInitialVx());
        } else {
            setVelocityX(getInitialVx() * scaling);
        }
        if (getInitialVy() == getEnvironment().getCursor().getDy()) {
            setVelocityY(getInitialVy());
        } else {
            setVelocityY(getInitialVy() * scaling);
        } */

        velocityX = getInitialVx() * scaling;
        velocityY = getInitialVy() * scaling;
    }

    @Override
    public boolean hasNext() throws VariableException {
        Point pos = getMascot().getAnchor();
        // Check that the velocity is at least 0 so that, if a mascot starts an action with negative initial Y velocity,
        // the action is not cancelled immediately.
        boolean onBorder = getEnvironment().getFloor().isOn(pos) && velocityY >= 0 || getEnvironment().getWall().isOn(pos);
        return super.hasNext() && !onBorder;
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        final Mascot mascot = getMascot();
        if (velocityX != 0) {
            mascot.setLookRight(velocityX > 0);
        }

        velocityX = (velocityX / scaling - velocityX * getResistanceX() / scaling) * scaling;
        velocityY = (velocityY / scaling - velocityY * getResistanceY() / scaling + getGravity()) * scaling;

        putVariable(getSchema().getString(VARIABLE_VELOCITYX), velocityX);
        putVariable(getSchema().getString(VARIABLE_VELOCITYY), velocityY);

        modX += velocityX % 1;
        modY += velocityY % 1;

        // Movement amount
        final int dx = (int) Math.round(velocityX + modX);
        final int dy = (int) Math.round(velocityY + modY);

        modX %= 1;
        modY %= 1;

        int dev = Math.max(1, Math.max(Math.abs(dx), Math.abs(dy)));

        final Point anchor = mascot.getAnchor();
        final MascotEnvironment environment = getEnvironment();

        OUTER:
        for (int i = 0; i <= dev; i++) {
            int x = anchor.x + dx * i / dev;
            int y = anchor.y + dy * i / dev;

            // Move mascot
            mascot.setAnchor(new Point(x, y));
            if (dy > 0) {
                // HACK: Windows may be moved, so check them often.
                for (int j = -80; j <= 0; j++) {
                    mascot.setAnchor(new Point(x, y + j));
                    if (environment.getFloor(true).isOn(mascot.getAnchor())) {
                        break OUTER;
                    }
                }
            }
            if (environment.getWall(true).isOn(mascot.getAnchor())) {
                break;
            }
        }

        // Animate
        getAnimation().next(mascot, getTime());
    }

    private int getInitialVx() throws VariableException {
        return eval(getSchema().getString(PARAMETER_INITIALVX), Number.class, DEFAULT_INITIALVX).intValue();
    }

    private int getInitialVy() throws VariableException {
        return eval(getSchema().getString(PARAMETER_INITIALVY), Number.class, DEFAULT_INITIALVY).intValue();
    }

    private double getGravity() throws VariableException {
        return eval(getSchema().getString(PARAMETER_GRAVITY), Number.class, DEFAULT_GRAVITY).doubleValue();
    }

    private double getResistanceX() throws VariableException {
        return eval(getSchema().getString(PARAMETER_RESISTANCEX), Number.class, DEFAULT_RESISTANCEX).doubleValue();
    }

    private double getResistanceY() throws VariableException {
        return eval(getSchema().getString(PARAMETER_RESISTANCEY), Number.class, DEFAULT_RESISTANCEY).doubleValue();
    }
}
