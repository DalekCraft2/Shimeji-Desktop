package com.group_finity.mascot.generic;

import com.group_finity.mascot.image.NativeImage;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;

/**
 * {@link GenericTranslucentWindow} a value that can be used with images.
 * <p>
 * {@link GenericTranslucentWindow} is available because only Windows bitmap
 * {@link BufferedImage} existing copy pixels from a Windows bitmap.
 * <p>
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
class GenericNativeImage implements NativeImage {

    /**
     * Java Image object.
     */
    private final BufferedImage managedImage;

    private final Icon icon;

    public GenericNativeImage(final BufferedImage image) {
        managedImage = image;
        icon = new ImageIcon(image);
    }

    public void flush() {
        getManagedImage().flush();
    }

    public Graphics getGraphics() {
        return getManagedImage().createGraphics();
    }

    public int getWidth() {
        return getManagedImage().getWidth();
    }

    public int getHeight() {
        return getManagedImage().getHeight();
    }

    public int getWidth(final ImageObserver observer) {
        return getManagedImage().getWidth(observer);
    }

    public int getHeight(final ImageObserver observer) {
        return getManagedImage().getHeight(observer);
    }

    public Object getProperty(final String name, final ImageObserver observer) {
        return getManagedImage().getProperty(name, observer);
    }

    public ImageProducer getSource() {
        return getManagedImage().getSource();
    }

    BufferedImage getManagedImage() {
        return managedImage;
    }

    public Icon getIcon() {
        return icon;
    }

}
