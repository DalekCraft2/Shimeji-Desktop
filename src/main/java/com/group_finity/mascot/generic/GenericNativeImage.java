package com.group_finity.mascot.generic;

import com.group_finity.mascot.image.NativeImage;

import java.awt.image.BufferedImage;

/**
 * An image with alpha value that can be used for {@link GenericTranslucentWindow}.
 *
 * @author Yuki Yamada
 * @author Shimeji-ee Group
 */
class GenericNativeImage implements NativeImage {

    /**
     * Java image object.
     */
    private final BufferedImage managedImage;

    public GenericNativeImage(final BufferedImage image) {
        managedImage = image;
    }

    BufferedImage getManagedImage() {
        return managedImage;
    }
}
