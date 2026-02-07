package com.group_finity.mascot.behavior;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.action.ActionBase;
import com.group_finity.mascot.animation.Hotspot;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple {@link Behavior} implementation.
 *
 * @author Yuki Yamada
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

    private final Action action;

    private final Configuration configuration;

    private Mascot mascot;

    public UserBehavior(final String name, final Action action, final Configuration configuration) {
        this.name = name;
        this.action = action;
        this.configuration = configuration;
    }

    @Override
    public String toString() {
        return "Behavior(" + name + ")";
    }

    @Override
    public synchronized void init(final Mascot mascot) throws CantBeAliveException {
        if (mascot == null) {
            return;
        }

        this.mascot = mascot;

        log.log(Level.INFO, "Initializing behavior \"{0}\" for mascot \"{1}\"", new Object[]{name, mascot});

        try {
            action.init(mascot);
            if (!action.hasNext()) {
                try {
                    mascot.setBehavior(configuration.buildNextBehavior(name, mascot));
                } catch (final BehaviorInstantiationException e) {
                    throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getString("FailedInitialiseFollowingBehaviourErrorMessage"), e);
                }
            }
        } catch (final VariableException e) {
            throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getString("VariableEvaluationErrorMessage"), e);
        }
    }

    /**
     * Called when a mouse button is pressed.
     * If the left button is pressed, start dragging.
     *
     * @throws CantBeAliveException if the next behavior fails to initialize and the associated {@link Mascot} should be
     * disposed
     */
    @Override
    public synchronized void mousePressed(final MouseEvent event) throws CantBeAliveException {
        if (mascot == null) {
            return;
        }

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
                            mascot.setCursorPosition(event.getPoint());
                            if (hotspot.getBehaviour() != null) {
                                mascot.setBehavior(configuration.buildBehavior(hotspot.getBehaviour(), mascot));
                            }
                        } catch (final BehaviorInstantiationException e) {
                            throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getString("FailedInitialiseFollowingBehaviourErrorMessage"), e);
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
                    mascot.setBehavior(configuration.buildBehavior(configuration.getSchema().getString(BEHAVIOURNAME_DRAGGED)));
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
     * @throws CantBeAliveException if the next behavior fails to initialize and the associated {@link Mascot} should be
     * disposed
     */
    @Override
    public synchronized void mouseReleased(final MouseEvent event) throws CantBeAliveException {
        if (mascot == null) {
            return;
        }

        if (SwingUtilities.isLeftMouseButton(event)) {
            if (mascot.isHotspotClicked()) {
                mascot.setCursorPosition(null);
            }

            // check if we are in the middle of a drag, otherwise we do nothing
            if (mascot.isDragging()) {
                try {
                    // Stop dragging
                    mascot.setDragging(false);
                    mascot.setBehavior(configuration.buildBehavior(configuration.getSchema().getString(BEHAVIOURNAME_THROWN)));
                } catch (final BehaviorInstantiationException e) {
                    throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getString("FailedDropActionInitialiseErrorMessage"), e);
                }
            }
        }
    }

    @Override
    public synchronized void next() throws CantBeAliveException {
        if (mascot == null) {
            return;
        }

        try {
            if (action.hasNext()) {
                action.next();
            }

            HotspotState hotspotState = HotspotState.INACTIVE;
            if (mascot.isHotspotClicked()) {
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
                                    mascot.setBehavior(configuration.buildBehavior(hotspot.getBehaviour(), mascot));
                                }
                            } catch (final BehaviorInstantiationException e) {
                                throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getString("FailedInitialiseFollowingBehaviourErrorMessage"), e);
                            }
                            break;
                        }
                    }
                }

                if (hotspotState == HotspotState.INACTIVE) {
                    mascot.setCursorPosition(null);
                }
            }

            if (hotspotState != HotspotState.ACTIVE) {
                if (action.hasNext()) {
                    // If it goes off-screen
                    if (mascot.getBounds().getX() + mascot.getBounds().getWidth()
                            <= getEnvironment().getScreen().getLeft()
                            || getEnvironment().getScreen().getRight() <= mascot.getBounds().getX()
                            || getEnvironment().getScreen().getBottom() <= mascot.getBounds().getY()) {
                        log.log(Level.INFO, "Out of the screen bounds ({0}, {1})", new Object[]{mascot, this});

                        if (Boolean.parseBoolean(Main.getInstance().getProperties().getProperty("Multiscreen", "true"))) {
                            mascot.setAnchor(new Point((int) (Math.random() * (getEnvironment().getScreen().getRight() - getEnvironment().getScreen().getLeft())) + getEnvironment().getScreen().getLeft(),
                                    getEnvironment().getScreen().getTop() - 256));
                        } else {
                            mascot.setAnchor(new Point((int) (Math.random() * (getEnvironment().getWorkArea().getRight() - getEnvironment().getWorkArea().getLeft())) + getEnvironment().getWorkArea().getLeft(),
                                    getEnvironment().getWorkArea().getTop() - 256));
                        }

                        try {
                            mascot.setBehavior(configuration.buildBehavior(configuration.getSchema().getString(BEHAVIOURNAME_FALL)));
                        } catch (final BehaviorInstantiationException e) {
                            throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getString("FailedFallingActionInitialiseErrorMessage"), e);
                        }
                    }
                } else {
                    log.log(Level.INFO, "Completed behavior \"{0}\" for mascot \"{1}\"", new Object[]{name, mascot});

                    try {
                        mascot.setBehavior(configuration.buildNextBehavior(name, mascot));
                    } catch (final BehaviorInstantiationException e) {
                        throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getString("FailedInitialiseFollowingActionsErrorMessage"), e);
                    }
                }
            }
        } catch (final LostGroundException e) {
            log.log(Level.INFO, "Lost ground ({0}, {1})", new Object[]{mascot, this});

            try {
                mascot.setCursorPosition(null);
                mascot.setDragging(false);
                mascot.setBehavior(configuration.buildBehavior(configuration.getSchema().getString(BEHAVIOURNAME_FALL)));
            } catch (final BehaviorInstantiationException ex) {
                throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getString("FailedFallingActionInitialiseErrorMessage"), ex);
            }
        } catch (final VariableException e) {
            throw new CantBeAliveException(Main.getInstance().getLanguageBundle().getString("VariableEvaluationErrorMessage"), e);
        }
    }

    protected MascotEnvironment getEnvironment() {
        return mascot.getEnvironment();
    }
}
