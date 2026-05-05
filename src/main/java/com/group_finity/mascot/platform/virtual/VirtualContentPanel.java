package com.group_finity.mascot.platform.virtual;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

/**
 * Virtual desktop content pane.
 *
 * @author Kilkakon
 * @since 1.0.21
 */
public class VirtualContentPanel extends JPanel {
    private Image resizedImage;
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

    VirtualContentPanel(Dimension preferredSize, Color background, final Image image, final ResizeMode mode) {
        setLayout(null);
        setPreferredSize(preferredSize);
        setBackground(background);
        resizedImage = image;
        this.mode = mode;

        addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (image != null) {
                    switch (mode) {
                        case CENTRE:
                            break;
                        case FILL:
                        case FIT:
                            double widthRatio = getWidth() / (double) image.getWidth(null);
                            double heightRatio = getHeight() / (double) image.getHeight(null);
                            double factor = mode == VirtualContentPanel.ResizeMode.FIT ?
                                    Math.min(widthRatio, heightRatio) :
                                    Math.max(widthRatio, heightRatio);

                            resizedImage = image.getScaledInstance((int) (factor * image.getWidth(null)),
                                    (int) (factor * image.getHeight(null)),
                                    Image.SCALE_SMOOTH);
                            break;
                        case STRETCH:
                            resizedImage = image.getScaledInstance(getWidth(), getHeight(), Image.SCALE_SMOOTH);
                    }
                }
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (resizedImage != null) {
            switch (mode) {
                case CENTRE:
                    g.drawImage(resizedImage,
                            resizedImage.getWidth(null) > getWidth() ?
                                    (resizedImage.getWidth(null) - getWidth()) / -2 :
                                    (getWidth() - resizedImage.getWidth(null)) / 2,
                            resizedImage.getHeight(null) > getHeight() ?
                                    (resizedImage.getHeight(null) - getHeight()) / -2 :
                                    (getHeight() - resizedImage.getHeight(null)) / 2,
                            null);
                    break;
                case FILL:
                case FIT:
                    g.drawImage(resizedImage,
                            (getWidth() - resizedImage.getWidth(null)) / 2,
                            (getHeight() - resizedImage.getHeight(null)) / 2,
                            null);
                    break;
                case STRETCH:
                    g.drawImage(resizedImage, 0, 0, null);
                    break;
            }
        }
    }
}

