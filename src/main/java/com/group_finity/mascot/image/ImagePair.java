package com.group_finity.mascot.image;

/**
 * A pair of left and right mascot images.
 * <p>
 * It would be convenient if the left and right mascot images could be managed simultaneously.
 *
 * @author Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * @author Shimeji-ee Group
 */
public class ImagePair {

    /**
     * Image facing left.
     */
    private MascotImage leftImage;

    /**
     * Image facing right.
     */
    private MascotImage rightImage;

    /**
     * Creates an image pair from two existing images.
     *
     * @param leftImage image facing left
     * @param rightImage image facing right
     */
    public ImagePair(
            final MascotImage leftImage, final MascotImage rightImage) {
        this.leftImage = leftImage;
        this.rightImage = rightImage;
    }

    /**
     * Obtains an image facing the specified direction.
     *
     * @param lookRight whether to get images facing right
     * @return image facing the specified direction
     */
    public MascotImage getImage(final boolean lookRight) {
        return lookRight ? getRightImage() : getLeftImage();
    }

    private MascotImage getLeftImage() {
        return leftImage;
    }

    private MascotImage getRightImage() {
        return rightImage;
    }
}
