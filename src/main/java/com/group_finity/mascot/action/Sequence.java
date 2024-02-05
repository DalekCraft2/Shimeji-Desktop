package com.group_finity.mascot.action;

import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * An action that combines multiple actions into one.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class Sequence extends ComplexAction {

    private static final Logger log = Logger.getLogger(Sequence.class.getName());

    public static final String PARAMETER_LOOP = "Loop";

    private static final boolean DEFAULT_LOOP = false;

    public Sequence(ResourceBundle schema, final VariableMap context, final Action... actions) {
        super(schema, context, actions);
    }

    @Override
    public boolean hasNext() throws VariableException {

        seek();

        return super.hasNext();
    }

    @Override
    protected void setCurrentAction(final int currentAction) throws VariableException {
        super.setCurrentAction(isLoop() ? currentAction % getActions().length : currentAction);
    }

    private Boolean isLoop() throws VariableException {
        return eval(getSchema().getString(PARAMETER_LOOP), Boolean.class, DEFAULT_LOOP);
    }
}
