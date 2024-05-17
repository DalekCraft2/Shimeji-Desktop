package com.group_finity.mascot.behavior;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.action.ActionBase;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.hotspot.Hotspot;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple {@link Behavior} implementation.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class UserBehavior implements Behavior {
    private static final Logger log = Logger.getLogger(UserBehavior.class.getName());

    public static final String BEHAVIOURNAME_FALL = "Fall";

    public static final String BEHAVIOURNAME_DRAGGED = "Dragged";

    public static final String BEHAVIOURNAME_THROWN = "Thrown";

    private enum HotspotState {
        INACTIVE,
        ACTIVE_NULL,
        ACTIVE
    }

    private final String name;

    private final Configuration configuration;

    private final Action action;

    private Mascot mascot;

    public UserBehavior(final String name, final Action action, final Configuration configuration) {
        this.name = name;
        this.configuration = configuration;
        this.action = action;
    }

    @Override
    public String toString() {
        return "Behavior(" + getName() + ")";
    }

    @Override
    public synchronized void init(final Mascot mascot) throws CantBeAliveException {
        setMascot(mascot);

        log.log(Level.INFO, "Initializing behavior \"{0}\" for mascot \"{1}\"", new Object[]{getName(), getMascot()});

        try {
            getAction().init(mascot);
            if (!getAction().hasNext()) {
                try {
                    mascot.setBehavior(getConfiguration().buildNextBehavior(getName(), mascot));
                } catch (final BehaviorInstantiationException e) {
                    throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getString("FailedInitialiseFollowingBehaviourErrorMessage"), e);
                }
            }
        } catch (final VariableException e) {
            throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getString("VariableEvaluationErrorMessage"), e);
        }
    }

    private Configuration getConfiguration() {
        return configuration;
    }

    private Action getAction() {
        return action;
    }

    private String getName() {
        return name;
    }

    /**
     * Called when a mouse button is pressed.
     * If the left button is pressed, start dragging.
     *
     * @throws CantBeAliveException
     */
    @Override
    public synchronized void mousePressed(final MouseEvent event) throws CantBeAliveException {
        if (SwingUtilities.isLeftMouseButton(event)) {
            boolean handled = false;

            // check for hotspots
            if (!mascot.getHotspots().isEmpty()) {
                for (final Hotspot hotspot : mascot.getHotspots()) {
                    if (hotspot.contains(mascot, event.getPoint()) &&
                            Main.getInstance().getConfiguration(mascot.getImageSet()).isBehaviorEnabled(hotspot.getBehaviour(), mascot)) {
                        // activate hotspot
                        handled = true;
                        try {
                            getMascot().setCursorPosition(event.getPoint());
                            if (hotspot.getBehaviour() != null) {
                                getMascot().setBehavior(configuration.buildBehavior(hotspot.getBehaviour(), mascot));
                            }
                        } catch (final BehaviorInstantiationException e) {
                            throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getString("FailedInitialiseFollowingBehaviourErrorMessage") + " " + hotspot.getBehaviour(), e);
                        }
                        break;
                    }
                }
            }

            // check if this action has dragging disabled
            if (!handled && action != null && action instanceof ActionBase) {
                try {
                    handled = !((ActionBase) action).isDraggable();
                } catch (VariableException ex) {
                    throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getString("FailedDragActionInitialiseErrorMessage"), ex);
                }
            }

            if (!handled) {
                // Begin dragging
                try {
                    getMascot().setBehavior(configuration.buildBehavior(configuration.getSchema().getString(BEHAVIOURNAME_DRAGGED)));
                } catch (final BehaviorInstantiationException e) {
                    throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getString("FailedDragActionInitialiseErrorMessage"), e);
                }
            }
        }
    }

    /**
     * Called when a mouse button is released.
     * If the left button is released, the dragging ends.
     *
     * @throws CantBeAliveException
     */
    @Override
    public synchronized void mouseReleased(final MouseEvent event) throws CantBeAliveException {
        if (SwingUtilities.isLeftMouseButton(event)) {
            if (getMascot().isHotspotClicked()) {
                getMascot().setCursorPosition(null);
            }

            // check if we are in the middle of a drag, otherwise we do nothing
            if (getMascot().isDragging()) {
                try {
                    // Stop dragging
                    getMascot().setDragging(false);
                    getMascot().setBehavior(configuration.buildBehavior(configuration.getSchema().getString(BEHAVIOURNAME_THROWN)));
                } catch (final BehaviorInstantiationException e) {
                    throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getString("FailedDropActionInitialiseErrorMessage"), e);
                }
            }
        }
    }

    @Override
    public synchronized void next() throws CantBeAliveException {
        try {
            if (getAction().hasNext()) {
                getAction().next();
            }

            HotspotState hotspotState = HotspotState.INACTIVE;
            if (getMascot().isHotspotClicked()) {
                // activate any hotspots that emerge while mouse is held down
                if (!mascot.getHotspots().isEmpty()) {
                    for (final Hotspot hotspot : mascot.getHotspots()) {
                        if (hotspot.contains(mascot, mascot.getCursorPosition())) {
                            // activate hotspot
                            hotspotState = HotspotState.ACTIVE_NULL;
                            try {
                                // no need to set cursor position, it's already set
                                if (hotspot.getBehaviour() != null) {
                                    hotspotState = HotspotState.ACTIVE;
                                    getMascot().setBehavior(configuration.buildBehavior(hotspot.getBehaviour(), mascot));
                                }
                            } catch (final BehaviorInstantiationException e) {
                                throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getString("FailedInitialiseFollowingBehaviourErrorMessage") + " " + hotspot.getBehaviour(), e);
                            }
                            break;
                        }
                    }
                }

                if (hotspotState == HotspotState.INACTIVE) {
                    getMascot().setCursorPosition(null);
                }
            }

            if (hotspotState != HotspotState.ACTIVE) {
                if (getAction().hasNext()) {
                    // If it goes off-screen
                    if (getMascot().getBounds().getX() + getMascot().getBounds().getWidth()
                            <= getEnvironment().getScreen().getLeft()
                            || getEnvironment().getScreen().getRight() <= getMascot().getBounds().getX()
                            || getEnvironment().getScreen().getBottom() <= getMascot().getBounds().getY()) {
                        log.log(Level.INFO, "Out of the screen bounds ({0}, {1})", new Object[]{getMascot(), this});

                        if (Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Multiscreen", "true"))) {
                            getMascot().setAnchor(new Point((int) (Math.random() * (getEnvironment().getScreen().getRight() - getEnvironment().getScreen().getLeft())) + getEnvironment().getScreen().getLeft(),
                                    getEnvironment().getScreen().getTop() - 256));
                        } else {
                            getMascot().setAnchor(new Point((int) (Math.random() * (getEnvironment().getWorkArea().getRight() - getEnvironment().getWorkArea().getLeft())) + getEnvironment().getWorkArea().getLeft(),
                                    getEnvironment().getWorkArea().getTop() - 256));
                        }

                        try {
                            getMascot().setBehavior(getConfiguration().buildBehavior(configuration.getSchema().getString(BEHAVIOURNAME_FALL)));
                        } catch (final BehaviorInstantiationException e) {
                            throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getString("FailedFallingActionInitialiseErrorMessage"), e);
                        }
                    }
                } else {
                    log.log(Level.INFO, "Completed behavior \"{0}\" for mascot \"{1}\"", new Object[]{getName(), getMascot()});

                    try {
                        getMascot().setBehavior(getConfiguration().buildNextBehavior(getName(), getMascot()));
                    } catch (final BehaviorInstantiationException e) {
                        throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getString("FailedInitialiseFollowingActionsErrorMessage"), e);
                    }
                }
            }
        } catch (final LostGroundException e) {
            log.log(Level.INFO, "Lost ground ({0}, {1})", new Object[]{getMascot(), this});

            try {
                getMascot().setCursorPosition(null);
                getMascot().setDragging(false);
                getMascot().setBehavior(configuration.buildBehavior(configuration.getSchema().getString(BEHAVIOURNAME_FALL)));
            } catch (final BehaviorInstantiationException ex) {
                throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getString("FailedFallingActionInitialiseErrorMessage"), ex);
            }
        } catch (final VariableException e) {
            throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getString("VariableEvaluationErrorMessage"), e);
        }
    }

    private void setMascot(final Mascot mascot) {
        this.mascot = mascot;
    }

    private Mascot getMascot() {
        return mascot;
    }

    protected MascotEnvironment getEnvironment() {
        return getMascot().getEnvironment();
    }
}
