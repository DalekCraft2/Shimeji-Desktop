package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.awt.*;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Action of throwing a window.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class ThrowIE extends Animate {
    private static final Logger log = Logger.getLogger(ThrowIE.class.getName());

    public static final String PARAMETER_INITIALVX = "InitialVX";

    private static final int DEFAULT_INITIALVX = 32;

    public static final String PARAMETER_INITIALVY = "InitialVY";

    private static final int DEFAULT_INITIALVY = -10;

    public static final String PARAMETER_GRAVITY = "Gravity";

    private static final double DEFAULT_GRAVITY = 0.5;

    private double scaling;

    private long activeWindowId = 0;

    public ThrowIE(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        super.init(mascot);

        scaling = Double.parseDouble(Main.getInstance().getProperties().getProperty("Scaling", "1.0"));
        activeWindowId = getEnvironment().getActiveWindowId();
    }

    @Override
    public boolean hasNext() throws VariableException {
        if (!Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Throwing", "true"))) {
            return false;
        }

        // Check whether the window being thrown has not changed;
        // this ensures that only one window gets moved off-screen instead of all of the on-screen windows.
        final boolean isSameWindow = activeWindowId == getEnvironment().getActiveWindowId();
        final boolean ieVisible = getEnvironment().getActiveIE().isVisible();

        return super.hasNext() && isSameWindow && ieVisible;
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        super.tick();

        final Area activeIE = getEnvironment().getActiveIE();

        if (activeIE.isVisible()) {
            if (getMascot().isLookRight()) {
                getEnvironment().moveActiveIE(new Point(activeIE.getLeft() + (int) Math.round(getInitialVx() * scaling),
                        activeIE.getTop() + (int) Math.round(getInitialVy() * scaling + getTime() * getGravity() * scaling)));
            } else {
                getEnvironment().moveActiveIE(new Point(activeIE.getLeft() - (int) Math.round(getInitialVx() * scaling),
                        activeIE.getTop() + (int) Math.round(getInitialVy() * scaling + getTime() * getGravity() * scaling)));
            }
        }
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
}
