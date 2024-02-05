package com.group_finity.mascot.virtual;

import com.group_finity.mascot.image.NativeImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;

/**
 * Virtual desktop native image.
 *
 * @author Kilkakon
 * @since 1.0.20
 */
class VirtualNativeImage implements NativeImage {
    /**
     * Java Image object.
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

    public Graphics getGraphics() {
        return managedImage.createGraphics();
    }

    public int getWidth() {
        return managedImage.getWidth();
    }

    public int getHeight() {
        return managedImage.getHeight();
    }

    public int getWidth(final ImageObserver observer) {
        return managedImage.getWidth(observer);
    }

    public int getHeight(final ImageObserver observer) {
        return managedImage.getHeight(observer);
    }

    public Object getProperty(final String name, final ImageObserver observer) {
        return managedImage.getProperty(name, observer);
    }

    public ImageProducer getSource() {
        return managedImage.getSource();
    }

    BufferedImage getManagedImage() {
        return managedImage;
    }

    /* public Icon getIcon() {
        return icon;
    } */
}
