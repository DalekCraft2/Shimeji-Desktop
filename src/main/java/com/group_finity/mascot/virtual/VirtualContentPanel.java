package com.group_finity.mascot.virtual;

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
    private String mode;

    private static final String CENTRE = "centre";
    private static final String FIT = "fit";
    private static final String STRETCH = "stretch";
    private static final String FILL = "fill";

    public VirtualContentPanel(Dimension preferredSize, Color background, final Image image, final String mode) {
        setLayout(null);
        setPreferredSize(preferredSize);
        setBackground(background);
        resizedImage = image;
        this.mode = mode;

        if (!(mode.equals(CENTRE) || mode.equals(FIT) || mode.equals(STRETCH) || mode.equals(FILL))) {
            this.mode = CENTRE;
        }

        addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (image != null) {
                    if (mode.equals(STRETCH)) {
                        resizedImage = image.getScaledInstance(getWidth(),
                                getHeight(),
                                Image.SCALE_SMOOTH);
                    } else if (!mode.equals(CENTRE)) {
                        double factor = mode.equals(FIT) ?
                                Math.min(getWidth() / (double) image.getWidth(null), getHeight() / (double) image.getHeight(null)) :
                                Math.max(getWidth() / (double) image.getWidth(null), getHeight() / (double) image.getHeight(null));
                        resizedImage = image.getScaledInstance((int) (factor * image.getWidth(null)),
                                (int) (factor * image.getHeight(null)),
                                Image.SCALE_SMOOTH);
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
            if (mode.equals(STRETCH)) {
                g.drawImage(resizedImage, 0, 0, null);
            } else if (mode.equals(CENTRE)) {
                g.drawImage(resizedImage,
                        resizedImage.getWidth(null) > getWidth() ? (resizedImage.getWidth(null) - getWidth()) / -2 : (getWidth() - resizedImage.getWidth(null)) / 2,
                        resizedImage.getHeight(null) > getHeight() ? (resizedImage.getHeight(null) - getHeight()) / -2 : (getHeight() - resizedImage.getHeight(null)) / 2,
                        null);
            } else {
                g.drawImage(resizedImage,
                        (getWidth() - resizedImage.getWidth(null)) / 2,
                        (getHeight() - resizedImage.getHeight(null)) / 2,
                        null);
            }
        }
    }
}

