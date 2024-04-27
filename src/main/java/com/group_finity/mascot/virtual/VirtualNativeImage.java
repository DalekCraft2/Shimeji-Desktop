package com.group_finity.mascot.virtual;

import com.group_finity.mascot.image.NativeImage;

import java.awt.*;
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

    // private final Icon icon;

    public VirtualNativeImage(final BufferedImage image) {
        managedImage = image;
        // icon = new ImageIcon(image);
    }

    public void flush() {
        managedImage.flush();
    }

    @Override
    public Graphics2D createGraphics() {
        return managedImage.createGraphics();
    }

    @Override
    public int getWidth() {
        return managedImage.getWidth();
    }

    @Override
    public int getHeight() {
        return managedImage.getHeight();
    }

    @Override
    public Object getProperty(final String name) {
        return managedImage.getProperty(name);
    }

    BufferedImage getManagedImage() {
        return managedImage;
    }

    /* public Icon getIcon() {
        return icon;
    } */
}
