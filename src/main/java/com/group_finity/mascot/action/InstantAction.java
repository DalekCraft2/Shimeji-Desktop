package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * A base class for actions that can be completed instantly by simply changing the state of the mascot.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public abstract class InstantAction extends ActionBase {

    private static final Logger log = LoggerFactory.getLogger(InstantAction.class);

    public InstantAction(ResourceBundle schema, final VariableMap context) {
        super(schema, new ArrayList<>(), context);

    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        super.init(mascot);

        if (super.hasNext()) {
            apply();
        }
    }

    protected abstract void apply() throws VariableException;

    @Override
    public final boolean hasNext() throws VariableException {
        super.hasNext();
        return false;
    }

    @Override
    protected final void tick() {
    }
}
