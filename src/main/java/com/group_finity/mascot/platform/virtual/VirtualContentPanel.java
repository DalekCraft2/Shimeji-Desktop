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
     * The unscaled instance of this content pane's background image.
     * If {@code null}, no image will be displayed.
     *
     * @see #updateResizedImage()
     * @see #setBackgroundImage(Image)
     */
    private Image backgroundImage;

    /**
     * A copy of {@link #backgroundImage} that has been scaled according
     * to the {@linkplain #resizeMode resize mode} and dimensions of this content pane.
     *
     * @see #updateResizedImage()
     */
    private Image resizedImage;

    /**
     * The type of behavior to use when scaling this content pane's background image,
     * if {@link #backgroundImage} is not {@code null}.
     *
     * @see #updateResizedImage()
     * @see #setResizeMode(ResizeMode)
     */
    private ResizeMode resizeMode;

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
     * @param backgroundImage an optional image to display in the background of this content pane
     * @param resizeMode the type of behavior to use when scaling this content pane's background image,
     * if {@code backgroundImage} is not {@code null}
     */
    VirtualContentPanel(Dimension preferredSize, Color background, final Image backgroundImage, final ResizeMode resizeMode) {
        setLayout(null);
        setPreferredSize(preferredSize);
        setBackground(background);
        this.backgroundImage = backgroundImage;
        this.resizeMode = resizeMode;

        /* Add a listener to resize the image as soon as the window is made visible,
        because resizing it in the constructor without using a listener would
        have no effect due to getWidth() and getHeight() returning 0 at that point. */
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                updateResizedImage();
            }
        });

        // Add a listener to resize the background image whenever the content pane is resized
        addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateResizedImage();
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
     * Sets the image to display in the background of this content pane.
     *
     * @param backgroundImage the background image to display, or {@code null} to display no image
     */
    void setBackgroundImage(Image backgroundImage) {
        if (this.backgroundImage != null) {
            this.backgroundImage.flush();
        }
        if (resizedImage != null && resizedImage != this.backgroundImage) {
            resizedImage.flush();
            resizedImage = null;
        }
        this.backgroundImage = backgroundImage;
    }

    /**
     * Sets the type of behavior to use when scaling this content pane's background image,
     * if the background image is not {@code null}.
     *
     * @param resizeMode the scaling behavior to use
     */
    void setResizeMode(ResizeMode resizeMode) {
        this.resizeMode = resizeMode;
    }

    /**
     * Updates {@link #resizedImage} by setting it to a scaled instance of this content pane's current background image.
     * Scaling is done based on the {@linkplain #resizeMode resize mode} and dimensions of this content pane.
     */
    void updateResizedImage() {
        if (backgroundImage != null) {
            if (resizedImage != null && !resizedImage.equals(backgroundImage)) {
                resizedImage.flush();
            }

            switch (resizeMode) {
                case CENTRE -> resizedImage = backgroundImage;
                case FILL, FIT -> {
                    int panelWidth = getWidth();
                    int panelHeight = getHeight();
                    if (panelWidth == 0 || panelHeight == 0) {
                        // Can't create a scaled image instance with 0 width or height
                        break;
                    }

                    double widthRatio = panelWidth / (double) backgroundImage.getWidth(null);
                    double heightRatio = panelHeight / (double) backgroundImage.getHeight(null);
                    double factor = resizeMode == ResizeMode.FIT ?
                            Math.min(widthRatio, heightRatio) :
                            Math.max(widthRatio, heightRatio);

                    resizedImage = backgroundImage.getScaledInstance((int) (factor * backgroundImage.getWidth(null)),
                            (int) (factor * backgroundImage.getHeight(null)),
                            Image.SCALE_SMOOTH);
                }
                case STRETCH -> {
                    int panelWidth = getWidth();
                    int panelHeight = getHeight();
                    if (panelWidth == 0 || panelHeight == 0) {
                        // Can't create a scaled image instance with 0 width or height
                        break;
                    }

                    resizedImage = backgroundImage.getScaledInstance(panelWidth, panelHeight, Image.SCALE_SMOOTH);
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (resizedImage != null) {
            switch (resizeMode) {
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

