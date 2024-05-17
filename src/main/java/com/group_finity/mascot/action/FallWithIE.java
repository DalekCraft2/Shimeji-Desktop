package com.group_finity.mascot.action;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.awt.*;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action of falling whilst holding a window.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
// TODO Modify this, WalkWithIE, and ThrowIE to use scaling so they line up the sprites with the windows' corner at any scale.
public class FallWithIE extends Fall {
    private static final Logger log = Logger.getLogger(FallWithIE.class.getName());

    public static final String PARAMETER_IEOFFSETX = "IeOffsetX";

    private static final int DEFAULT_IEOFFSETX = 0;

    public static final String PARAMETER_IEOFFSETY = "IeOffsetY";

    private static final int DEFAULT_IEOFFSETY = 0;

    public FallWithIE(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public boolean hasNext() throws VariableException {
        if (!Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Throwing", "true"))) {
            return false;
        }

        return super.hasNext();
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        final Area activeIE = getEnvironment().getActiveIE();
        if (!activeIE.isVisible()) {
            log.log(Level.INFO, "IE not visible ({0}, {1})", new Object[]{getMascot(), this});
            throw new LostGroundException();
        }

        // Can't use scaling here yet because it doesn't work for scales other than 1; the Shimejis will just fall off the window immediately.
        // final int offsetX = (int) Math.round(getIEOffsetX() * scaling);
        // final int offsetY = (int) Math.round(getIEOffsetY() * scaling);
        final int offsetX = getIEOffsetX();
        final int offsetY = getIEOffsetY();

        // Check whether the mascot has the window properly
        if (getMascot().isLookRight()) {
            if (getMascot().getAnchor().x - offsetX != activeIE.getLeft()
                    || getMascot().getAnchor().y + offsetY != activeIE.getBottom()) {
                log.log(Level.INFO, "Lost ground ({0}, {1})", new Object[]{getMascot(), this});
                throw new LostGroundException();
            }
        } else {
            if (getMascot().getAnchor().x + offsetX != activeIE.getRight()
                    || getMascot().getAnchor().y + offsetY != activeIE.getBottom()) {
                log.log(Level.INFO, "Lost ground ({0}, {1})", new Object[]{getMascot(), this});
                throw new LostGroundException();
            }
        }

        super.tick();

        // Move window
        if (activeIE.isVisible()) {
            if (getMascot().isLookRight()) {
                getEnvironment().moveActiveIE(new Point(getMascot().getAnchor().x - offsetX,
                        getMascot().getAnchor().y + offsetY - activeIE.getHeight()));
            } else {
                getEnvironment().moveActiveIE(new Point(getMascot().getAnchor().x + offsetX - activeIE.getWidth(),
                        getMascot().getAnchor().y + offsetY - activeIE.getHeight()));
            }
        }
    }

    private int getIEOffsetX() throws VariableException {
        return eval(getSchema().getString(PARAMETER_IEOFFSETX), Number.class, DEFAULT_IEOFFSETX).intValue();
    }

    private int getIEOffsetY() throws VariableException {
        return eval(getSchema().getString(PARAMETER_IEOFFSETY), Number.class, DEFAULT_IEOFFSETY).intValue();
    }
}
