package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.awt.*;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Jumping action.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class Jump extends ActionBase {
    private static final Logger log = Logger.getLogger(Jump.class.getName());

    public static final String PARAMETER_TARGETX = "TargetX";

    private static final int DEFAULT_TARGETX = 0;

    public static final String PARAMETER_TARGETY = "TargetY";

    private static final int DEFAULT_TARGETY = 0;

    // A Pose attribute is already named "Velocity", so this is named "VelocityParam" instead
    public static final String PARAMETER_VELOCITY = "VelocityParam";

    private static final double DEFAULT_VELOCITY = 20.0;

    public static final String VARIABLE_VELOCITYX = "VelocityX";

    public static final String VARIABLE_VELOCITYY = "VelocityY";

    private double scaling;

    public Jump(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        super.init(mascot);

        scaling = Double.parseDouble(Main.getInstance().getProperties().getProperty("Scaling", "1.0"));
    }

    @Override
    public boolean hasNext() throws VariableException {
        final int targetX = getTargetX();
        final int targetY = getTargetY();

        final double distanceX = targetX - getMascot().getAnchor().x;
        final double distanceY = targetY - getMascot().getAnchor().y - Math.abs(distanceX) / 2;

        final double distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY);

        return super.hasNext() && distance != 0;
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        final int targetX = getTargetX();
        final int targetY = getTargetY();

        getMascot().setLookRight(getMascot().getAnchor().x < targetX);

        final double distanceX = targetX - getMascot().getAnchor().x;
        final double distanceY = targetY - getMascot().getAnchor().y - Math.abs(distanceX) / 2;

        final double distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY);

        final double velocity = getVelocity() * scaling;

        if (distance != 0) {
            final int velocityX = (int) Math.round(velocity * distanceX / distance);
            final int velocityY = (int) Math.round(velocity * distanceY / distance);

            putVariable(getSchema().getString(VARIABLE_VELOCITYX), velocity * distanceX / distance);
            putVariable(getSchema().getString(VARIABLE_VELOCITYY), velocity * distanceY / distance);

            getMascot().setAnchor(new Point(getMascot().getAnchor().x + velocityX,
                    getMascot().getAnchor().y + velocityY));
            getAnimation().next(getMascot(), getTime());
        }

        if (distance <= velocity) {
            getMascot().setAnchor(new Point(targetX, targetY));
        }
    }

    private double getVelocity() throws VariableException {
        return eval(getSchema().getString(PARAMETER_VELOCITY), Number.class, DEFAULT_VELOCITY).doubleValue();
    }

    private int getTargetX() throws VariableException {
        return eval(getSchema().getString(PARAMETER_TARGETX), Number.class, DEFAULT_TARGETX).intValue();
    }

    private int getTargetY() throws VariableException {
        return eval(getSchema().getString(PARAMETER_TARGETY), Number.class, DEFAULT_TARGETY).intValue();
    }
}
