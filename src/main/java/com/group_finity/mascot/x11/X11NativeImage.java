/*
 * Created by asdfman
 * https://github.com/asdfman/linux-shimeji
 */
package com.group_finity.mascot.x11;

import com.group_finity.mascot.image.NativeImage;

import java.awt.image.BufferedImage;

/**
 * An image with alpha value that can be used for {@link X11TranslucentWindow}.
 *
 * @author asdfman
 */
class X11NativeImage implements NativeImage {

    /**
     * Java image object.
     */
    private final BufferedImage managedImage;

    public X11NativeImage(final BufferedImage image) {
        managedImage = image;
    }

    BufferedImage getManagedImage() {
        return managedImage;
    }
}
