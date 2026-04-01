package com.group_finity.mascot.action;

import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

/**
 * An action that combines multiple actions into one.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class Sequence extends ComplexAction {
    private static final Logger log = LoggerFactory.getLogger(Sequence.class);

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

    private boolean isLoop() throws VariableException {
        return eval(getSchema().getString(PARAMETER_LOOP), Boolean.class, DEFAULT_LOOP);
    }
}
