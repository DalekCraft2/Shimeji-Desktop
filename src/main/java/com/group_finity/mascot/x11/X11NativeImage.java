/*
 * Created by asdfman
 * https://github.com/asdfman/linux-shimeji
 */
package com.group_finity.mascot.x11;

import com.group_finity.mascot.image.NativeImage;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * An image with alpha value that can be used for {@link X11TranslucentWindow}.
 * <p>
 * Only Windows bitmaps can be used for {@link X11TranslucentWindow}, so
 * copy pixels from an existing {@link BufferedImage} to a Windows bitmap.
 *
 * @author asdfman
 */
class X11NativeImage implements NativeImage {

    /**
     * Java image object.
     */
    private final BufferedImage managedImage;

    // private final Icon icon;

    public X11NativeImage(final BufferedImage image) {
        managedImage = image;
        // icon = new ImageIcon(image);
    }

    public int getWidth() {
        return getManagedImage().getWidth();
    }

    public int getHeight() {
        return getManagedImage().getHeight();
    }

    BufferedImage getManagedImage() {
        return managedImage;
    }

    /* public Icon getIcon() {
        return icon;
    } */
}
