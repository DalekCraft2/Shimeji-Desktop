package com.group_finity.mascot.display.model;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.SettingsWindow;
import com.group_finity.mascot.display.view.MascotInfoView;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.Environment;

import javax.swing.*;
import java.util.Objects;

/** Represents the different types of information that can be obtained and displayed on a {@link MascotInfoView}. */
public enum MascotInfoType {
    /** A description of what the {@link Mascot} is doing. */
    BEHAVIOUR("Behaviour"),

    /** X-axis coordinate of the {@link Area} of the {@link Environment} that the {@link Mascot} is displayed within. */
    ENVIRONMENT_X("EnvironmentX"),

    /** Y-axis coordinate of the {@link Area} of the {@link Environment} that the {@link Mascot} is displayed within. */
    ENVIRONMENT_Y("EnvironmentY"),

    /** Height of the {@link Area} of the {@link Environment} that the {@link Mascot} is displayed within. */
    ENVIRONMENT_HEIGHT("EnvironmentHeight"),

    /** Width of the {@link Area} of the {@link Environment} that the {@link Mascot} is displayed within. */
    ENVIRONMENT_WIDTH("EnvironmentWidth"),

    /** X-axis {@link Mascot#anchor} coordinate of a {@link Mascot} within its {@link Environment}. */
    SHIMEJI_X("ShimejiX"),

    /** Y-axis {@link Mascot#anchor} coordinate of a {@link Mascot} within its {@link Environment}. */
    SHIMEJI_Y("ShimejiY"),

    /**
     * <p>
     * Height of the window (e.g. {@link JFrame}) that the {@link Mascot} will interacting with. At the time of writing,
     * this is <em>always</em> the most-recently focused window, which has a title that contains one of the values from
     * {@link SettingsWindow#lstInteractiveWindows}.
     * </p>
     *
     * <p>This value is never updated when {@link SettingsWindow#windowedMode} is {@code true}.</p>
     */
    WINDOW_HEIGHT("WindowHeight"),

    /**
     * <p>Title of the window (e.g. {@link JFrame}) that the {@link Mascot} is interacting with.</p>
     *
     * <p>
     *     For example, if <em>Notepad</em> is added as an <em>Interactive Window</em> and the {@link Mascot} is
     *     sitting on top of the <em>Notepad</em> window, this value will be the full title of the <em>Notepad</em>
     *     window.
     * </p>
     *
     * <p>This value is never updated when {@link SettingsWindow#windowedMode} is {@code true}.</p>
     **/
    WINDOW_TITLE("ActiveIE"),

    /**
     * <p>
     * Width of the window (e.g. {@link JFrame}) that the {@link Mascot} will interacting with. At the time of writing,
     * this is <em>always</em> the most-recently focused window, which has a title that contains one of the values from
     * {@link SettingsWindow#lstInteractiveWindows}.
     * </p>
     *
     * <p>This value is never updated when {@link SettingsWindow#windowedMode} is {@code true}.</p>
     */
    WINDOW_WIDTH("WindowWidth"),

    /**
     * <p>
     * X-axis coordinate of the window (e.g. {@link JFrame}) that the {@link Mascot} will interacting with. At the time
     * of writing, this is <em>always</em> the most-recently focused window, which has a title that contains one of the
     * values from {@link SettingsWindow#lstInteractiveWindows}.
     * </p>
     *
     * <p>This value is never updated when {@link SettingsWindow#windowedMode} is {@code true}.</p>
     */
    WINDOW_X("WindowX"),

    /**
     * <p>
     * Y-axis coordinate of the window (e.g. {@link JFrame}) that the {@link Mascot} will interacting with. At the time
     * of writing, this is <em>always</em> the most-recently focused window, which has a title that contains one of the
     * values from {@link SettingsWindow#lstInteractiveWindows}.
     * </p>
     *
     * <p>This value is never updated when {@link SettingsWindow#windowedMode} is {@code true}.</p>
     */
    WINDOW_Y("WindowY"),;

    /** The key to use when retrieving the translated enum value from the resource bundle. */
    private final String resourceBundleKey;

    /**
     * Constructs a new {@link MascotInfoType}.
     *
     * @param resourceBundleKey {@see #resourceBundleKey}.
     */
    MascotInfoType(final String resourceBundleKey) {
        Objects.requireNonNull(resourceBundleKey);

        if (resourceBundleKey.isBlank()) {
            throw new IllegalArgumentException("You must specify a non-blank language bundle key.");
        }

        this.resourceBundleKey = resourceBundleKey;
    }

    /**
     * Retrieves the localized name of this enum value.
     *
     * @return Localized name of this enum value.
     */
    public String localizedName() {
        return Main.getInstance().getLanguageBundle().getString(resourceBundleKey);
    }
}
