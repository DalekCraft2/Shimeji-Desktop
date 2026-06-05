package com.group_finity.mascot.behavior;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.action.ActionBase;
import com.group_finity.mascot.action.LostGroundException;
import com.group_finity.mascot.animation.Hotspot;
import com.group_finity.mascot.config.BehaviorInstantiationException;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.script.VariableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Simple {@link Behavior} implementation.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class UserBehavior implements Behavior {
    private static final Logger log = LoggerFactory.getLogger(UserBehavior.class);

    /**
     * Action that matches the "Gather Around Mouse!" context menu command
     */
    public static final String BEHAVIORNAME_CHASEMOUSE = "ChaseMouse";

    public static final String BEHAVIORNAME_FALL = "Fall";

    public static final String BEHAVIORNAME_DRAGGED = "Dragged";

    public static final String BEHAVIORNAME_THROWN = "Thrown";

    /**
     * Enumeration of the state a hotspot is in, in terms of whether it is being clicked
     * and whether it has a behavior set.
     */
    private enum HotspotState {
        /**
         * No hotspot is being clicked.
         */
        INACTIVE,
        /**
         * A hotspot is being clicked, but it has no behavior set.
         */
        ACTIVE_NULL,
        /**
         * A hotspot is being clicked, and it has a behavior set.
         */
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
        return "Behavior[name=" + name + "]";
    }

    @Override
    public synchronized void init(final Mascot mascot) throws BehaviorExecutionException {
        if (mascot == null) {
            return;
        }

        this.mascot = mascot;

        log.info("Initializing behavior \"{}\" for mascot \"{}\"", name, mascot);

        try {
            action.init(mascot);
            if (!action.hasNext()) {
                try {
                    mascot.setBehavior(configuration.buildNextBehavior(name, mascot));
                } catch (final BehaviorInstantiationException e) {
                    throw new BehaviorExecutionException(String.format(Main.getInstance().getLanguageBundle().getString("FailedInitialiseFollowingBehaviourErrorMessage"), e.getBehaviorName()), e);
                }
            }
        } catch (final VariableException e) {
            throw new BehaviorExecutionException(Main.getInstance().getLanguageBundle().getString("VariableEvaluationErrorMessage"), e);
        }
    }

    @Override
    public synchronized void next() throws BehaviorExecutionException {
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
                            // no need to set cursor position, it's already set
                            if (hotspot.getBehaviour() != null) {
                                hotspotState = HotspotState.ACTIVE;
                                try {
                                    mascot.setBehavior(configuration.buildBehavior(hotspot.getBehaviour(), mascot));
                                } catch (final BehaviorInstantiationException e) {
                                    throw new BehaviorExecutionException(String.format(Main.getInstance().getLanguageBundle().getString("FailedInitialiseFollowingBehaviourErrorMessage"), e.getBehaviorName()), e);
                                }
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
                    Rectangle mascotBounds = mascot.getBounds();
                    Area screen = getEnvironment().getScreen();
                    // If it goes off-screen
                    if (mascotBounds.getX() + mascotBounds.getWidth()
                            <= screen.getLeft()
                            || screen.getRight() <= mascotBounds.getX()
                            || screen.getBottom() <= mascotBounds.getY()) {
                        log.info("Out of the screen bounds ({}, {})", mascot, this);

                        Area area = Main.getInstance().getSettings().multiscreen
                                ? screen : getEnvironment().getWorkArea();
                        // Subtract 2 from the width and add 1 to the left border X value so the mascot doesn't start climbing the walls instead of falling
                        mascot.getAnchor().setLocation((int) (Math.random() * (area.getWidth() - 2)) + area.getLeft() + 1, area.getTop() - 256);

                        try {
                            mascot.setBehavior(configuration.buildBehavior(configuration.getSchema().getString(BEHAVIORNAME_FALL)));
                        } catch (final BehaviorInstantiationException e) {
                            throw new BehaviorExecutionException(Main.getInstance().getLanguageBundle().getString("FailedFallingActionInitialiseErrorMessage"), e);
                        }
                    }
                } else {
                    log.info("Completed behavior \"{}\" for mascot \"{}\"", name, mascot);

                    try {
                        mascot.setBehavior(configuration.buildNextBehavior(name, mascot));
                    } catch (final BehaviorInstantiationException e) {
                        throw new BehaviorExecutionException(String.format(Main.getInstance().getLanguageBundle().getString("FailedInitialiseFollowingBehaviourErrorMessage"), e.getBehaviorName()), e);
                    }
                }
            }
        } catch (final LostGroundException e) {
            if (e.getMessage() != null) {
                log.info("Lost ground ({}, {}): {}", mascot, this, e.getMessage());
            } else {
                log.info("Lost ground ({}, {})", mascot, this);
            }

            mascot.setCursorPosition(null);
            mascot.setDragging(false);
            try {
                mascot.setBehavior(configuration.buildBehavior(configuration.getSchema().getString(BEHAVIORNAME_FALL)));
            } catch (final BehaviorInstantiationException ex) {
                throw new BehaviorExecutionException(Main.getInstance().getLanguageBundle().getString("FailedFallingActionInitialiseErrorMessage"), ex);
            }
        } catch (final VariableException e) {
            throw new BehaviorExecutionException(Main.getInstance().getLanguageBundle().getString("VariableEvaluationErrorMessage"), e);
        }
    }

    /**
     * Called when a mouse button is pressed.
     * If the left button is pressed, start dragging.
     *
     * @throws BehaviorExecutionException if the next behavior fails to initialize
     * @see java.awt.event.MouseListener#mousePressed(MouseEvent)
     */
    @Override
    public synchronized void mousePressed(final MouseEvent event) throws BehaviorExecutionException {
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
                        mascot.setCursorPosition(event.getPoint());
                        if (hotspot.getBehaviour() != null) {
                            try {
                                mascot.setBehavior(configuration.buildBehavior(hotspot.getBehaviour(), mascot));
                            } catch (final BehaviorInstantiationException e) {
                                throw new BehaviorExecutionException(String.format(Main.getInstance().getLanguageBundle().getString("FailedInitialiseFollowingBehaviourErrorMessage"), hotspot.getBehaviour()), e);
                            }
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
                    throw new BehaviorExecutionException(Main.getInstance().getLanguageBundle().getString("FailedDragActionInitialiseErrorMessage"), ex);
                }
            }

            if (!handled) {
                // Begin dragging
                try {
                    mascot.setBehavior(configuration.buildBehavior(configuration.getSchema().getString(BEHAVIORNAME_DRAGGED)));
                } catch (final BehaviorInstantiationException e) {
                    throw new BehaviorExecutionException(Main.getInstance().getLanguageBundle().getString("FailedDragActionInitialiseErrorMessage"), e);
                }
            }
        }
    }

    /**
     * Called when a mouse button is released.
     * If the left button is released, the dragging ends.
     *
     * @throws BehaviorExecutionException if the next behavior fails to initialize
     * @see java.awt.event.MouseListener#mouseReleased(MouseEvent)
     */
    @Override
    public synchronized void mouseReleased(final MouseEvent event) throws BehaviorExecutionException {
        if (mascot == null) {
            return;
        }

        if (SwingUtilities.isLeftMouseButton(event)) {
            if (mascot.isHotspotClicked()) {
                mascot.setCursorPosition(null);
            }

            // check if we are in the middle of a drag, otherwise we do nothing
            if (mascot.isDragging()) {
                // Stop dragging
                mascot.setDragging(false);
                try {
                    mascot.setBehavior(configuration.buildBehavior(configuration.getSchema().getString(BEHAVIORNAME_THROWN)));
                } catch (final BehaviorInstantiationException e) {
                    throw new BehaviorExecutionException(Main.getInstance().getLanguageBundle().getString("FailedDropActionInitialiseErrorMessage"), e);
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    protected MascotEnvironment getEnvironment() {
        return mascot.getEnvironment();
    }
}
