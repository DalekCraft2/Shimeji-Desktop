package com.group_finity.mascot.image;

/**
 * A pair of left and right mascot images.
 * <p>
 * It would be convenient if the left and right mascot images could be managed simultaneously.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 * @param leftImage left-facing image
 * @param rightImage right-facing image
 */
public record ImagePair(MascotImage leftImage, MascotImage rightImage) {
    /**
     * Obtains an image facing the specified direction.
     *
     * @param lookRight whether to get the right-facing image
     * @return image facing the specified direction
     */
    public MascotImage getImage(final boolean lookRight) {
        return lookRight ? rightImage : leftImage;
    }
}
