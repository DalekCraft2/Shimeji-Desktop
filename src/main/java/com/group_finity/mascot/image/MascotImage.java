package com.group_finity.mascot.image;

import com.group_finity.mascot.NativeFactory;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
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
        return image;
    }

    public Point getCenter() {
        return center;
    }

    public Dimension getSize() {
        return size;
    }

}
