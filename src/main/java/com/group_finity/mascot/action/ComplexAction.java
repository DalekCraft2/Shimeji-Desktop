package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * A base class for actions that combine multiple actions into one in series.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public abstract class ComplexAction extends ActionBase {
    private static final Logger log = LoggerFactory.getLogger(ComplexAction.class);

    private final Action[] actions;

    private int currentAction;

    public ComplexAction(ResourceBundle schema, final VariableMap context, final Action... actions) {
        super(schema, new ArrayList<>(), context);
        if (actions.length == 0) {
            throw new IllegalArgumentException("actions.length==0");
        }

        this.actions = actions;
    }

    @Override
    public void init(final Mascot mascot) throws VariableException {
        super.init(mascot);

        if (super.hasNext()) {
            setCurrentAction(0);
            seek();
        }
    }

    protected void seek() throws VariableException {
        if (super.hasNext()) {
            while (getCurrentAction() < getActions().length) {
                if (getAction().hasNext()) {
                    break;
                }
                setCurrentAction(getCurrentAction() + 1);
            }
        }
    }

    @Override
    public boolean hasNext() throws VariableException {
        final boolean inRange = getCurrentAction() < getActions().length;

        return super.hasNext() && inRange && getAction().hasNext();
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        if (getAction().hasNext()) {
            getAction().next();
        }
    }

    protected Action[] getActions() {
        return actions;
    }

    protected Action getAction() {
        return getActions()[getCurrentAction()];
    }

    protected int getCurrentAction() {
        return currentAction;
    }

    protected void setCurrentAction(final int currentAction) throws VariableException {
        this.currentAction = currentAction;
        if (super.hasNext()) {
            if (getCurrentAction() < getActions().length) {
                getAction().init(getMascot());
            }
        }
    }

    @Override
    public boolean isDraggable() throws VariableException {
        boolean draggable = true;
        if (currentAction < actions.length && actions[currentAction] != null && actions[currentAction] instanceof ActionBase) {
            return ((ActionBase) actions[currentAction]).isDraggable();
        }
        return draggable;
    }
}
