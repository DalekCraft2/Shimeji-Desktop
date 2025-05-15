package com.group_finity.mascot.image;

import com.group_finity.mascot.Main;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Loads image pairs.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 * @author Valkryst
 */
public class ImagePairLoader {
    /**
     * Loads an image pair.
     *
     * @param name file name of left-facing image to load
     * @param rightName file name of right-facing image to load
     * @param center image center coordinate
     * @param scaling the scale factor of the image
     * @param imageScaler {@link ImageScaler} to use when rescaling the image.
     */
    public static void load(
        final String name,
        final String rightName,
        final Point center,
        final double scaling,
        final ImageScaler imageScaler,
        final double opacity
    ) throws IOException {
        String key = name + (rightName == null ? "" : rightName);
        if (ImagePairs.contains(key)) {
            return;
        }

        final BufferedImage leftImage = imageScaler.scale(premultiply(ImageIO.read(Main.IMAGE_DIRECTORY.resolve(name).toFile()), opacity), scaling);
        final BufferedImage rightImage;
        if (rightName == null) {
            rightImage = flip(leftImage);
        } else {
            rightImage = imageScaler.scale(premultiply(ImageIO.read(Main.IMAGE_DIRECTORY.resolve(rightName).toFile()), opacity), scaling);
        }

        ImagePair ip = new ImagePair(new MascotImage(leftImage, new Point((int) Math.round(center.x * scaling), (int) Math.round(center.y * scaling))),
                new MascotImage(rightImage, new Point(rightImage.getWidth() - (int) Math.round(center.x * scaling), (int) Math.round(center.y * scaling))));
        ImagePairs.load(key, ip);
    }

    /**
     * Flips the image horizontally.
     *
     * @param src the image to flip horizontally
     * @return horizontally flipped image
     */
    private static BufferedImage flip(final BufferedImage src) {
        final BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(),
                src.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB : src.getType());

        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                copy.setRGB(copy.getWidth() - x - 1, y, src.getRGB(x, y));
            }
        }
        return copy;
    }

    private static BufferedImage premultiply(final BufferedImage source, final double opacity) {
        final BufferedImage returnImage = new BufferedImage(source.getWidth(), source.getHeight(),
                source.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB_PRE : source.getType());
        Color colour;
        float[] components;

        for (int y = 0; y < returnImage.getHeight(); y++) {
            for (int x = 0; x < returnImage.getWidth(); x++) {
                colour = new Color(source.getRGB(x, y), true);
                components = colour.getComponents(null);
                components[3] *= (float) opacity;
                components[0] = components[3] * components[0];
                components[1] = components[3] * components[1];
                components[2] = components[3] * components[2];
                colour = new Color(components[0], components[1], components[2], components[3]);
                returnImage.setRGB(x, y, colour.getRGB());
            }
        }

        return returnImage;
    }
}
