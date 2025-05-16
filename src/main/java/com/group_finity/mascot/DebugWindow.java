package com.group_finity.mascot;

import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.display.model.MascotInfoModel;
import com.group_finity.mascot.display.model.MascotInfoType;
import com.group_finity.mascot.display.view.MascotInfoView;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.MascotEnvironment;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * @author Kilkakon
 * @author Valkryst
 */
public class DebugWindow extends JFrame {
    /** {@link MascotInfoView} to display a {@link Mascot}'s properties in. */
    private final MascotInfoView mascotInfoView = new MascotInfoModel().createView();

    /**
     * Constructs a new {@link DebugWindow}.
     *
     * @param shjimejiId ID of the Shimeji whose properties are to be displayed.
     */
    public DebugWindow(final int shjimejiId) {
        this.setTitle("Shimeji #" + shjimejiId + " Properties");

        this.setContentPane(mascotInfoView);
        this.setVisible(true);
        this.pack();
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    /**
     * Updates the displayed properties in the {@link #mascotInfoView}.
     *
     * @param behaviour See {@link Mascot#behavior}.
     * @param shimejiAnchor See {@link Mascot#anchor}.
     * @param environmentArea See {@link MascotEnvironment#getWorkArea()}.
     * @param windowTitle See {@link MascotEnvironment#getActiveIETitle()}.
     * @param windowArea See {@link MascotEnvironment#getActiveIE()}.
     */
    public void update(
        final Behavior behaviour,
        final Point shimejiAnchor,
        final Area environmentArea,
        final String windowTitle,
        final Area windowArea
    ) {
        Objects.requireNonNull(behaviour);
        Objects.requireNonNull(shimejiAnchor);
        Objects.requireNonNull(environmentArea);
        Objects.requireNonNull(windowTitle);
        Objects.requireNonNull(windowArea);

        mascotInfoView.updateProperty(MascotInfoType.BEHAVIOUR, this.humanizeBehaviourName(behaviour));

        mascotInfoView.updateProperty(MascotInfoType.ENVIRONMENT_X, environmentArea.getLeft());
        mascotInfoView.updateProperty(MascotInfoType.ENVIRONMENT_Y, environmentArea.getTop());
        mascotInfoView.updateProperty(MascotInfoType.ENVIRONMENT_HEIGHT, environmentArea.getHeight());
        mascotInfoView.updateProperty(MascotInfoType.ENVIRONMENT_WIDTH, environmentArea.getWidth());

        mascotInfoView.updateProperty(MascotInfoType.SHIMEJI_X, shimejiAnchor.x);
        mascotInfoView.updateProperty(MascotInfoType.SHIMEJI_Y, shimejiAnchor.y);

        mascotInfoView.updateProperty(MascotInfoType.WINDOW_TITLE, windowTitle);
        mascotInfoView.updateProperty(MascotInfoType.WINDOW_HEIGHT, windowArea.getHeight());
        mascotInfoView.updateProperty(MascotInfoType.WINDOW_WIDTH, windowArea.getWidth());
        mascotInfoView.updateProperty(MascotInfoType.WINDOW_X, windowArea.getLeft());
        mascotInfoView.updateProperty(MascotInfoType.WINDOW_Y, windowArea.getTop());
    }

    /**
     * Generates a human-readable name for the given {@link Behavior}.
     *
     * @param behavior {@link Behavior} to generate a name for.
     * @return Human-readable name for the given {@link Behavior}.
     */
    private String humanizeBehaviourName(final Behavior behavior) {
        return behavior.toString()
                       .replaceAll("^Behavior\\(|\\)$", "")
                       .replaceAll("([a-z])(IE)?([A-Z])", "$1 $2 $3")
                       .replaceAll("\\s+", " ")
                       .trim();
    }
}