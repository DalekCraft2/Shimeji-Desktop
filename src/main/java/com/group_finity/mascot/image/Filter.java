package com.group_finity.mascot.image;

/**
 * Enumeration of the type of filter to be used for upscaling and drawing images.
 *
 * @author Shimeji-ee Group
 */
public enum Filter {
    /**
     * The color sample of the nearest neighboring integer coordinate sample in the image is used.
     *
     * @see java.awt.RenderingHints#VALUE_INTERPOLATION_NEAREST_NEIGHBOR
     */
    NEAREST_NEIGHBOUR,
    /**
     * The color samples of 9 nearby integer coordinate samples in the image are interpolated using a cubic function in
     * both {@code X} and {@code Y} to produce a color sample.
     *
     * @see java.awt.RenderingHints#VALUE_INTERPOLATION_BICUBIC
     */
    BICUBIC,
    /**
     * The <a href="https://en.wikipedia.org/wiki/Hqx_(algorithm)">hqx</a> filter is used. This can only be used
     * when an image is being upscaled by a factor of 2, 3, or 4.
     */
    HQX
}
