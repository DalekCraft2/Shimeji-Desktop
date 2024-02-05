package com.group_finity.mascot.virtual;

import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;

import javax.swing.*;
import java.awt.*;

/**
 * Virtual desktop translucent panel.
 *
 * @author Kilkakon
 * @since 1.0.20
 */
class VirtualTranslucentPanel extends JPanel implements TranslucentWindow {
    /**
     * Image to display.
     */
    private VirtualNativeImage image;

    @Override
    public String toString() {
        return "VirtualTranslucentPanel[hashCode=" + hashCode() + ",bounds=" + getBounds() + "]";
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (image != null) {
            g.drawImage(image.getManagedImage(), 0, 0, null);
        }
    }

    @Override
    public Component asComponent() {
        return this;
    }

    @Override
    public void setAlwaysOnTop(boolean onTop) {
    }

    @Override
    public void setImage(NativeImage image) {
        this.image = (VirtualNativeImage) image;
    }

    @Override
    public void dispose() {
        Container parent = getParent();
        if (parent != null) {
            parent.remove(this);
            parent.repaint();
        }
    }

    @Override
    public void updateImage() {
        repaint();
    }

    @Override
    public boolean contains(int x, int y) {
        if (super.contains(x, y)) {
            try {
                return (image.getManagedImage().getRGB(x, y) & 0xff000000) >>> 24 > 0;
            } catch (RuntimeException ex) {
                return false;
            }
        }
        return false;
    }
}