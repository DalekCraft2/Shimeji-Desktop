package com.group_finity.mascot.action;

import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

/**
 * Action for offsetting.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Offset extends InstantAction {
    private static final Logger log = LoggerFactory.getLogger(Offset.class);

    private static final String PARAMETER_OFFSETX = "X";
    private static final int DEFAULT_OFFSETX = 0;

    private static final String PARAMETER_OFFSETY = "Y";
    private static final int DEFAULT_OFFSETY = 0;

    // private double scaling;

    public Offset(ResourceBundle schema, final VariableMap context) {
        super(schema, context);
    }

    /* @Override
    public void init(final Mascot mascot) throws VariableException {
        // This must be set before super.init() is called because super.init() calls apply().
        scaling = Main.getInstance().getSettings().scaling;

        super.init(mascot);
    } */

    @Override
    protected void apply() throws VariableException {
        /*
        Can't use scaling here reliably yet. It works for x1 scale, but other scales mess things up when a mascot's
        script assumes the Offset's X or Y values will be the same as another value they have in an adjacent script.
        For instance, in the default actions.xml's "ClimbAlongWall" action sequence, the "ClimbWall" action is used
        with a TargetY of "mascot.environment.workArea.top+64", and then the "Offset" action is used with a Y of -64.
        If the former value is not scaled by the program, but the latter is, then the mascot will not be where it is
        intended to be by the time both actions have completed. In this example, the "ClimbCeiling" action is used after
        both actions, and the mascot immediately starts falling if the scale is not 1 because it is technically not on
        the ceiling.

        This is the same reason why this implementation of scaling does not work for the FallWithIE, WalkWithIE, and
        ThrowIE actions. A potential solution could be to scale the environment information instead of the action
        parameters.
         */
        getMascot().getAnchor().translate(
                // (int) Math.round(getOffsetX() * scaling), (int) Math.round(getOffsetY() * scaling));
                getOffsetX(), getOffsetY());
    }

    private int getOffsetX() throws VariableException {
        return eval(getSchema().getString(PARAMETER_OFFSETX), Number.class, DEFAULT_OFFSETX).intValue();
    }

    private int getOffsetY() throws VariableException {
        return eval(getSchema().getString(PARAMETER_OFFSETY), Number.class, DEFAULT_OFFSETY).intValue();
    }
}
