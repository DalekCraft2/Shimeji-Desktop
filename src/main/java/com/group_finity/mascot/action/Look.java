package com.group_finity.mascot.action;

import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

/**
 * Looking action.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Look extends InstantAction {
    private static final Logger log = LoggerFactory.getLogger(Look.class);

    public static final String PARAMETER_LOOKRIGHT = "LookRight";

    public Look(ResourceBundle schema, final VariableMap context) {
        super(schema, context);
    }

    @Override
    protected void apply() throws VariableException {
        getMascot().setLookRight(isLookRight());
    }

    private boolean isLookRight() throws VariableException {
        return eval(getSchema().getString(PARAMETER_LOOKRIGHT), Boolean.class, !getMascot().isLookRight());
    }
}
