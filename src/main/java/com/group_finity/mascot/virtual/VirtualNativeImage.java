package com.group_finity.mascot.virtual;

import com.group_finity.mascot.image.NativeImage;

import java.awt.image.BufferedImage;

/**
 * Virtual desktop native image.
 *
 * @author Kilkakon
 * @since 1.0.20
 */
class VirtualNativeImage implements NativeImage {
    /**
     * Java image object.
     */
    private final BufferedImage managedImage;

    public VirtualNativeImage(final BufferedImage image) {
        managedImage = image;
    }

    BufferedImage getManagedImage() {
        return managedImage;
    }
}
