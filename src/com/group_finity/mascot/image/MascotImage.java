package com.group_finity.mascot.image;

import com.group_finity.mascot.NativeFactory;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */

public class MascotImage {

    private final NativeImage image;

    private final Point center;

    private final Dimension size;

    public MascotImage(final NativeImage image, final Point center, final Dimension size) {
        this.image = image;
        this.center = center;
        this.size = size;
    }

    public MascotImage(final BufferedImage image, final Point center) {
        this(NativeFactory.getInstance().newNativeImage(image), center, new Dimension(image.getWidth(), image.getHeight()));
    }

    public NativeImage getImage() {
        return this.image;
    }

    public Point getCenter() {
        return this.center;
    }

    public Dimension getSize() {
        return this.size;
    }

}
