package com.group_finity.mascot.image;

/**
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
public class ImagePair {

    private MascotImage leftImage;

    private MascotImage rightImage;

    public ImagePair(
            final MascotImage leftImage, final MascotImage rightImage) {
        this.leftImage = leftImage;
        this.rightImage = rightImage;
    }

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
