package com.group_finity.mascot.action;

import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.awt.*;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Action for offsetting.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class Offset extends InstantAction {

    private static final Logger log = Logger.getLogger(Offset.class.getName());

    public static final String PARAMETER_OFFSETX = "X";

    private static final int DEFAULT_OFFSETX = 0;

    public static final String PARAMETER_OFFSETY = "Y";

    private static final int DEFAULT_OFFSETY = 0;

    // private double scaling;

    public Offset(ResourceBundle schema, final VariableMap context) {
        super(schema, context);
    }

    /* @Override
    public void init(final Mascot mascot) throws VariableException {
        super.init(mascot);

        scaling = Double.parseDouble(Main.getInstance().getProperties().getProperty("Scaling", "1.0"));
    } */

    @Override
    protected void apply() throws VariableException {
        // Can't use scaling here because it makes the mascots unable to climb on the ceiling.
        // Should've thought twice before I decided to cast doubles to integers...
        getMascot().setAnchor(
                // new Point(getMascot().getAnchor().x + (int) Math.round(getOffsetX() * scaling), getMascot().getAnchor().y + (int) Math.round(getOffsetY() * scaling)));
                new Point(getMascot().getAnchor().x + getOffsetX(), getMascot().getAnchor().y + getOffsetY()));
    }

    private int getOffsetX() throws VariableException {
        return eval(getSchema().getString(PARAMETER_OFFSETX), Number.class, DEFAULT_OFFSETX).intValue();
    }

    private int getOffsetY() throws VariableException {
        return eval(getSchema().getString(PARAMETER_OFFSETY), Number.class, DEFAULT_OFFSETY).intValue();
    }
}
