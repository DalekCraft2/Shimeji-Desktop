package com.group_finity.mascot.platform.virtual;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyEvent;

/**
 * The content pane for the virtual environment window, which supports displaying a background image
 * that can be automatically rescaled when the size of the window is changed.
 *
 * @author Kilkakon
 * @see VirtualEnvironment
 * @since 1.0.21
 */
public class VirtualContentPanel extends JPanel {
    /**
     * An optional image to display in the background of this content pane.
     *
     * @see #resizeImage(Image)
     */
    private Image resizedImage;

    /**
     * The type of behavior to use when scaling this content pane's background image,
     * if {@link #resizedImage} is not {@code null}.
     *
     * @see #resizeImage(Image)
     */
    private final ResizeMode mode;

    /**
     * Enumeration of the type of behavior to use when scaling the content panel's background image.
     */
    public enum ResizeMode {
        /**
         * Positions the background image in the center of the content pane.
         * Does not scale the image.
         */
        CENTRE,
        /**
         * Scales the background image to fill the content pane whilst also maintaining the image's aspect ratio.
         * Scales the image past the bounds of the content pane if necessary.
         */
        FILL,
        /**
         * Scales the background image to fit the content pane whilst also maintaining the image's aspect ratio.
         * Keeps the image within the bounds of the content pane.
         */
        FIT,
        /**
         * Stretches the background image to the size of the panel. Does not maintain the image's aspect ratio.
         */
        STRETCH
    }

    /**
     * Initializes a new {@code VirtualContentPanel}.
     *
     * @param preferredSize the initial dimensions of this content pane
     * @param background the background color of this content pane
     * @param image an optional image to display in the background of this content pane
     * @param mode the type of behavior to use when scaling this content pane's background image,
     * if {@code image} is not {@code null}
     */
    VirtualContentPanel(Dimension preferredSize, Color background, final Image image, final ResizeMode mode) {
        setLayout(null);
        setPreferredSize(preferredSize);
        setBackground(background);
        resizedImage = image;
        this.mode = mode;

        /* Add a listener to resize the image as soon as the window is made visible,
        because resizing it in the constructor without using a listener would
        have no effect due to getWidth() and getHeight() returning 0 at that point. */
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                resizeImage(image);
            }
        });

        // Add a listener to resize the background image whenever the content pane is resized
        addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeImage(image);
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentShown(ComponentEvent e) {
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });
    }

    /**
     * Updates {@link #resizedImage} by setting it to a scaled instance of the specified image.
     * Scaling is done based on the {@linkplain #mode resize mode} and dimensions of this content pane.
     *
     * @param image the image from which to create a scaled instance
     */
    private void resizeImage(Image image) {
        if (image != null) {
            switch (mode) {
                case CENTRE -> {
                }
                case FILL, FIT -> {
                    double widthRatio = getWidth() / (double) image.getWidth(null);
                    double heightRatio = getHeight() / (double) image.getHeight(null);
                    double factor = mode == ResizeMode.FIT ?
                            Math.min(widthRatio, heightRatio) :
                            Math.max(widthRatio, heightRatio);

                    resizedImage = image.getScaledInstance((int) (factor * image.getWidth(null)),
                            (int) (factor * image.getHeight(null)),
                            Image.SCALE_SMOOTH);
                }
                case STRETCH -> resizedImage = image.getScaledInstance(getWidth(), getHeight(), Image.SCALE_SMOOTH);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (resizedImage != null) {
            switch (mode) {
                case CENTRE -> g.drawImage(resizedImage,
                        resizedImage.getWidth(null) > getWidth() ?
                                (resizedImage.getWidth(null) - getWidth()) / -2 :
                                (getWidth() - resizedImage.getWidth(null)) / 2,
                        resizedImage.getHeight(null) > getHeight() ?
                                (resizedImage.getHeight(null) - getHeight()) / -2 :
                                (getHeight() - resizedImage.getHeight(null)) / 2,
                        null);
                case FILL, FIT -> g.drawImage(resizedImage,
                        (getWidth() - resizedImage.getWidth(null)) / 2,
                        (getHeight() - resizedImage.getHeight(null)) / 2,
                        null);
                case STRETCH -> g.drawImage(resizedImage, 0, 0, null);
            }
        }
    }
}

