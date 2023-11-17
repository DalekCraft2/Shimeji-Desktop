package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
public abstract class InstantAction extends ActionBase {

    private static final Logger log = Logger.getLogger(InstantAction.class.getName());

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
