package com.group_finity.mascot.image;

import hqx.Hqx_2x;
import hqx.Hqx_3x;
import hqx.Hqx_4x;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * A collection of methods for transforming images.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 * @author DalekCraft
 */
public class ImageUtils {
    private ImageUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a copy of the given image that is optimized for the system's default graphics configuration.
     *
     * @param src the image to convert
     * @return an image compatible with the default graphics configuration
     */
    public static BufferedImage toCompatibleImage(BufferedImage src) {
        GraphicsConfiguration graphicsConfig = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

        if (src.getColorModel().equals(graphicsConfig.getColorModel())) {
            return src;
        }

        BufferedImage compatible = graphicsConfig.createCompatibleImage(src.getWidth(), src.getHeight(), src.getTransparency());
        Graphics2D g2d = compatible.createGraphics();
        g2d.drawImage(src, 0, 0, null);
        g2d.dispose();

        return compatible;
    }

    /**
     * Creates an image with the specified dimensions.
     * The image will be optimized for the system's default graphics configuration.
     *
     * @param width the width of the returned image
     * @param height the height of the returned image
     * @return an image compatible with the default graphics configuration
     */
    public static BufferedImage createCompatibleImage(int width, int height) {
        GraphicsConfiguration graphicsConfig = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        return graphicsConfig.createCompatibleImage(width, height);
    }

    /**
     * Creates an image with the specified dimensions and transparency mode.
     * The image will be optimized for the system's default graphics configuration.
     *
     * @param width the width of the returned image
     * @param height the height of the returned image
     * @param transparency the specified transparency mode
     * @return an image compatible with the default graphics configuration
     * @see Transparency#OPAQUE
     * @see Transparency#BITMASK
     * @see Transparency#TRANSLUCENT
     */
    public static BufferedImage createCompatibleImage(int width, int height, int transparency) {
        GraphicsConfiguration graphicsConfig = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        return graphicsConfig.createCompatibleImage(width, height, transparency);
    }

    /**
     * Flips the image horizontally.
     *
     * @param src the image to flip horizontally
     * @return horizontally flipped image
     */
    public static BufferedImage flip(final BufferedImage src) {
        final BufferedImage copy = createCompatibleImage(src.getWidth(), src.getHeight(), src.getTransparency());

        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                copy.setRGB(copy.getWidth() - x - 1, y, src.getRGB(x, y));
            }
        }
        return copy;
    }

    public static BufferedImage premultiply(final BufferedImage source, final double opacity) {
        final BufferedImage returnImage = createCompatibleImage(source.getWidth(), source.getHeight(), opacity == 1 ? source.getTransparency() : Transparency.TRANSLUCENT);
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

    public static BufferedImage scale(final BufferedImage source, final double scaling, Filter filter) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage workingImage = null;

        // apply hqx if applicable
        double effectiveScaling = scaling;
        if (filter == Filter.HQX && scaling > 1) {
            int[] buffer;
            int[] rbgValues = source.getRGB(0, 0, width, height, null, 0, width);

            if (scaling == 4 || scaling == 8) {
                width *= 4;
                height *= 4;
                buffer = new int[width * height];
                Hqx_4x.hq4x_32_rb(rbgValues, buffer, width / 4, height / 4);
                rbgValues = buffer;
                effectiveScaling = scaling > 4 ? 2 : 1;
            } else if (scaling == 3 || scaling == 6) {
                width *= 3;
                height *= 3;
                buffer = new int[width * height];
                Hqx_3x.hq3x_32_rb(rbgValues, buffer, width / 3, height / 3);
                rbgValues = buffer;
                effectiveScaling = scaling > 4 ? 2 : 1;
            } else if (scaling == 2) {
                width *= 2;
                height *= 2;
                buffer = new int[width * height];
                Hqx_2x.hq2x_32_rb(rbgValues, buffer, width / 2, height / 2);
                rbgValues = buffer;
                effectiveScaling = 1;
            } else {
                filter = Filter.NEAREST_NEIGHBOUR;
            }

            // if hqx is still on then apply the changes
            if (filter == Filter.HQX) {
                workingImage = createCompatibleImage((int) Math.round(width * effectiveScaling), (int) Math.round(height * effectiveScaling), Transparency.TRANSLUCENT);
                int srcColIndex = 0;
                int srcRowIndex = 0;

                for (int y = 0; y < workingImage.getHeight(); y++) {
                    for (int x = 0; x < workingImage.getWidth(); x++) {
                        workingImage.setRGB(x, y, rbgValues[srcColIndex / (int) effectiveScaling]);
                        srcColIndex++;
                    }

                    // resets the srcColIndex to re-use the same indexes and stretch horizontally
                    srcRowIndex++;
                    if (srcRowIndex == effectiveScaling) {
                        srcRowIndex = 0;
                    } else {
                        srcColIndex -= workingImage.getWidth();
                    }
                }
            }
        }

        width = (int) Math.round(width * effectiveScaling);
        height = (int) Math.round(height * effectiveScaling);

        final BufferedImage copy = createCompatibleImage(width, height, workingImage != null ? Transparency.TRANSLUCENT : source.getTransparency());

        Graphics2D g2d = copy.createGraphics();
        Object renderHint = filter == Filter.BICUBIC
                ? RenderingHints.VALUE_INTERPOLATION_BICUBIC
                : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, renderHint);
        g2d.drawImage(workingImage != null ? workingImage : source, 0, 0, width, height, null);

        g2d.dispose();

        return copy;
    }
}
