package com.group_finity.mascot.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * An object that contains image data as well as information for how to position the image relative to a mascot.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
public class MascotImage {

    /**
     * This image's raw image data.
     *
     * @see #getImage()
     */
    private final BufferedImage image;

    /**
     * The point on this image that aligns with the mascot's anchor.
     *
     * @see #getCenter()
     */
    private final Point center;

    /**
     * The size of this image.
     *
     * @see #getSize()
     */
    private final Dimension size;

    /**
     * Creates a new MascotImage.
     *
     * @param image this image's raw image data
     * @param center the point on this image that aligns with the mascot's anchor
     * @param size the size of this image
     */
    public MascotImage(final BufferedImage image, final Point center, final Dimension size) {
        this.image = image;
        this.center = center;
        this.size = size;
    }

    /**
     * Creates a new MascotImage. The size is defaulted to the dimensions of the raw image data.
     *
     * @param image this image's raw image data
     * @param center the point on this image that aligns with the mascot's anchor
     */
    public MascotImage(final BufferedImage image, final Point center) {
        this(image, center, new Dimension(image.getWidth(), image.getHeight()));
    }

    /**
     * Gets this image's raw image data.
     *
     * @return this image's raw image data
     */
    public BufferedImage getImage() {
        return image;
    }

    /**
     * Gets the point on this image that aligns with the mascot's anchor.
     *
     * @return the point on this image that aligns with the mascot's anchor
     */
    public Point getCenter() {
        return center;
    }

    /**
     * Gets the size of this image.
     *
     * @return the size of this image
     */
    public Dimension getSize() {
        return size;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(image);
        result = 31 * result + Objects.hashCode(center);
        result = 31 * result + Objects.hashCode(size);
        return result;
    }

    @Override
    public final boolean equals(Object obj) {
        if (!(obj instanceof MascotImage mi)) return false;

        return image.equals(mi.image) && center.equals(mi.center) && size.equals(mi.size);
    }

    @Override
    public String toString() {
        return "MascotImage[" +
                "image=" + image +
                ", center=" + center +
                ", size=" + size +
                ']';
    }
}
