package com.group_finity.mascot.image;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class MascotImage {

    private final BufferedImage image;

    private final Point center;

    private final Dimension size;

    public MascotImage(final BufferedImage image, final Point center, final Dimension size) {
        this.image = image;
        this.center = center;
        this.size = size;
    }

    public MascotImage(final BufferedImage image, final Point center) {
        this(image, center, new Dimension(image.getWidth(), image.getHeight()));
    }

    public BufferedImage getImage() {
        return image;
    }

    public Point getCenter() {
        return center;
    }

    public Dimension getSize() {
        return size;
    }
}
