package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.exception.LostGroundException;
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
public abstract class ComplexAction extends ActionBase {

    private static final Logger log = Logger.getLogger(ComplexAction.class.getName());

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
        final boolean inrange = getCurrentAction() < getActions().length;

        return super.hasNext() && inrange && getAction().hasNext();
    }

    @Override
    protected void tick() throws LostGroundException, VariableException {
        if (getAction().hasNext()) {
            getAction().next();
        }
    }

    @Override
    public Boolean isDraggable() throws VariableException {
        boolean draggable = true;
        if (currentAction < actions.length && actions[currentAction] != null && actions[currentAction] instanceof ActionBase) {
            return ((ActionBase) actions[currentAction]).isDraggable();
        }
        return draggable;
    }

    protected void setCurrentAction(final int currentAction) throws VariableException {
        this.currentAction = currentAction;
        if (super.hasNext()) {
            if (getCurrentAction() < getActions().length) {
                getAction().init(getMascot());
            }
        }
    }

    protected int getCurrentAction() {
        return currentAction;
    }

    protected Action[] getActions() {
        return actions;
    }

    protected Action getAction() {
        return getActions()[getCurrentAction()];
    }

}
