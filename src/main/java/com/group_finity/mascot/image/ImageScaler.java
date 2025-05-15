package com.group_finity.mascot.image;

import hqx.Hqx_2x;
import hqx.Hqx_3x;
import hqx.Hqx_4x;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Facilitates scaling of {@link BufferedImage} objects using various algorithms.
 *
 * @author Valkryst
 */
public enum ImageScaler {
    NEAREST_NEIGHBOUR,
    BILINEAR,
    BICUBIC,
    HQX;

    /**
     * Scales a {@link BufferedImage} using this image scaling algorithm.
     *
     * @param source {@link BufferedImage} to scale.
     * @param scaleFactor Amount to scale the image by.
     * @return The rescaled {@link BufferedImage}.
     */
    public BufferedImage scale(final BufferedImage source, final double scaleFactor) {
        // If we're not scaling the image, just return a deep copy of it.
        // todo Determine if the rest of the codebase needs a deep copy, or if we can reuse the original.
        if (scaleFactor == 1.0) {
            final BufferedImage destination = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
            destination.copyData(source.getRaster());
            return destination;
        }

        // The HQX algorithm can only be used for upscaling.
        if (this == HQX && scaleFactor > 1.0) {
            return this.scaleHQX(source, scaleFactor);
        }

        final int scaledWidth = (int) Math.round(source.getWidth() * scaleFactor);
        final int scaledHeight = (int) Math.round(source.getHeight() * scaleFactor);

        final BufferedImage destination = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB_PRE);
        final Graphics2D graphics2D = destination.createGraphics();
        this.setRenderingHints(graphics2D);

        switch (this) {
            case NEAREST_NEIGHBOUR: {
                graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                break;
            }
            case BILINEAR: {
                graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                break;
            }
            case BICUBIC: {
                graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                break;
            }
            default: {
                throw new UnsupportedOperationException("No implementation has been provided for the " + this + " filter.");
            }
        }

        graphics2D.drawImage(source, 0, 0, scaledWidth, scaledHeight, null);
        graphics2D.dispose();

        return destination;
    }

    /**
     * Scales a {@link BufferedImage} using the HQX algorithm.
     *
     * @param source {@link BufferedImage} to scale.
     * @param scaleFactor Amount to scale the image by.
     * @return The rescaled {@link BufferedImage}.
     */
    private BufferedImage scaleHQX(final BufferedImage source, double scaleFactor) {
        int scaledWidth = source.getWidth();
        int scaledHeight = source.getHeight();
        final BufferedImage destination;

        int[] buffer;
        int[] rbgValues = source.getRGB(0, 0, scaledWidth, scaledHeight, null, 0, scaledWidth);

        if (scaleFactor == 4 || scaleFactor == 8) {
            scaledWidth *= 4;
            scaledHeight *= 4;
            buffer = new int[scaledWidth * scaledHeight];
            Hqx_4x.hq4x_32_rb(rbgValues, buffer, scaledWidth / 4, scaledHeight / 4);
            rbgValues = buffer;
            scaleFactor = scaleFactor > 4 ? 2 : 1;
        } else if (scaleFactor == 3 || scaleFactor == 6) {
            scaledWidth *= 3;
            scaledHeight *= 3;
            buffer = new int[scaledWidth * scaledHeight];
            Hqx_3x.hq3x_32_rb(rbgValues, buffer, scaledWidth / 3, scaledHeight / 3);
            rbgValues = buffer;
            scaleFactor = scaleFactor > 4 ? 2 : 1;
        } else if (scaleFactor == 2) {
            scaledWidth *= 2;
            scaledHeight *= 2;
            buffer = new int[scaledWidth * scaledHeight];
            Hqx_2x.hq2x_32_rb(rbgValues, buffer, scaledWidth / 2, scaledHeight / 2);
            rbgValues = buffer;
            scaleFactor = 1;
        } else {
            throw new UnsupportedOperationException("The HQX algorithm cannot be used with a scaleFactor of " + scaleFactor + ".");
        }

        destination = new BufferedImage((int) Math.round(scaledWidth * scaleFactor), (int) Math.round(scaledHeight * scaleFactor), BufferedImage.TYPE_INT_ARGB_PRE);
        int srcColIndex = 0;
        int srcRowIndex = 0;

        for (int y = 0; y < destination.getHeight(); y++) {
            for (int x = 0; x < destination.getWidth(); x++) {
                destination.setRGB(x, y, rbgValues[srcColIndex / (int) scaleFactor]);
                srcColIndex++;
            }

            // resets the srcColIndex to re-use the same indexes and stretch horizontally
            srcRowIndex++;
            if (srcRowIndex == scaleFactor) {
                srcRowIndex = 0;
            } else {
                srcColIndex -= destination.getWidth();
            }
        }

        return destination;
    }

    /**
     * Applies a set of {@link RenderingHints}, which have been deemed to be of high quality, to a {@link Graphics}
     * context.
     *
     * @param graphics2D {@link Graphics2D} context to apply the {@link RenderingHints} to.
     */
    private void setRenderingHints(final Graphics2D graphics2D) {
        // Automatically detect the best text rendering settings and apply them.
        final var desktopHints = (Map<?, ?>) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        if (desktopHints != null) {
            graphics2D.setRenderingHints(desktopHints);
        }

        graphics2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }
}
