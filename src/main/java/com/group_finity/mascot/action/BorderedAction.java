package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.environment.Border;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Base class for actions that move while attached to a frame.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public abstract class BorderedAction extends ActionBase {

    private static final Logger log = Logger.getLogger(BorderedAction.class.getName());

    public static final String PARAMETER_BORDERTYPE = "BorderType";

    private static final String DEFAULT_BORDERTYPE = null;

    public static final String BORDERTYPE_CEILING = "Ceiling";

    public static final String BORDERTYPE_WALL = "Wall";

    public static final String BORDERTYPE_FLOOR = "Floor";

    private Border border;

    public BorderedAction(ResourceBundle schema, final List<Animation> animations, final VariableMap context) {
        super(schema, animations, context);
    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        super.init(mascot);

        final String borderType = getBorderType();

        if (getSchema().getString(BORDERTYPE_CEILING).equals(borderType)) {
            setBorder(getEnvironment().getCeiling());
        } else if (getSchema().getString(BORDERTYPE_WALL).equals(borderType)) {
            setBorder(getEnvironment().getWall());
        } else if (getSchema().getString(BORDERTYPE_FLOOR).equals(borderType)) {
            setBorder(getEnvironment().getFloor());
        }
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        if (getBorder() != null) {
            // The frame may be moving
            getMascot().setAnchor(getBorder().move(getMascot().getAnchor()));
        }
    }

    private String getBorderType() throws VariableException {
        return eval(getSchema().getString(PARAMETER_BORDERTYPE), String.class, DEFAULT_BORDERTYPE);
    }

    private void setBorder(final Border border) {
        this.border = border;
    }

    protected Border getBorder() {
        return border;
    }

}
