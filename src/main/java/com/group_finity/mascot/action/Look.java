package com.group_finity.mascot.action;

import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Looking action.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class Look extends InstantAction {
    private static final Logger log = Logger.getLogger(Look.class.getName());

    public static final String PARAMETER_LOOKRIGHT = "LookRight";

    public Look(ResourceBundle schema, final VariableMap context) {
        super(schema, context);
    }

    @Override
    protected void apply() throws VariableException {
        getMascot().setLookRight(isLookRight());
    }

    private Boolean isLookRight() throws VariableException {
        return eval(getSchema().getString(PARAMETER_LOOKRIGHT), Boolean.class, !getMascot().isLookRight());
    }
}
